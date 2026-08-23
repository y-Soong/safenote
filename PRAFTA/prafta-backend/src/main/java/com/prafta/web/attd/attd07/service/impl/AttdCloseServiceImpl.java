package com.prafta.web.attd.attd07.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.attd.attd07.application.command.AttdCloseCommand;
import com.prafta.web.attd.attd07.application.param.AttdCloseParam;
import com.prafta.web.attd.attd07.application.param.AttdCloseStatusParam;
import com.prafta.web.attd.attd07.dto.response.AttdCloseStatusResponse;
import com.prafta.web.attd.attd07.mapper.AttdCloseMapper;
import com.prafta.web.attd.attd07.result.AttdCloseHistResult;
import com.prafta.web.attd.attd07.result.AttdCloseRowResult;
import com.prafta.web.attd.attd07.service.AttdCloseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link AttdCloseService} 구현 (prafta-019-C / prafta-028 부서 단위 확장).
 *
 * <p>정책서: attd/§13(근태 마감 차단 조건), 재기획서 §3.3(자동/강제 마감 금지).
 *
 * <p>PRAFTA-028: 마감 단위에 부서(NODE_CD) 차원 추가.
 * <ul>
 *   <li>조회 스코프 기준 마감 — master/hr 이 부서 없이 조회하면 전체('*'), 부서 선택 시 그 부서(+하위포함).</li>
 *   <li>권한 — master/hr 은 전체/임의 부서. 그 외는 본인이 정/부 관리자인 부서만(상위 노드 포함). 전체('*') 불가.</li>
 *   <li>커버리지 — 하위부서까지 마감(INC_SUB_YN='Y') 시 하위 노드의 데이터도 마감으로 본다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttdCloseServiceImpl implements AttdCloseService {

    private static final String STATUS_CLOSED = "CLOSED";
    private static final String ACTION_CLOSE = "CLOSE";
    private static final String ACTION_UNCLOSE = "UNCLOSE";

    /** 전체 사업장 마감을 나타내는 NODE_CD 센티넬. */
    private static final String WHOLE_SITE = "*";

    private final AttdCloseMapper attdCloseMapper;
    /** 사업장 접근 인가(공용 cmm 빈) — 토큰 사업장 등식 대신 User_03 원장(TB_USER_SITE_AUTH) 기반 인가. */
    private final com.prafta.common.cmm.siteauth.service.SiteAccessService siteAccessService;

    @Override
    public AttdCloseStatusResponse getCloseStatus(AttdCloseStatusParam param) {
        // 사업장 접근 인가(구 토큰 사업장 등식 가드 대체).
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());
        String cmpnyCd = param.gvCmpnyCd();
        String siteCd = param.siteCd();
        String closeYm = param.closeYm();
        String nodeCd = normalizeNode(param.nodeCd());
        String incSubYn = normalizeIncSub(nodeCd, param.incSubNodeYn());

        // 조회도 권한 스코프를 강제: 비 master/hr 은 본인이 관리하는 부서만 (부서 미지정 불가)
        ensureCanManageScope(param.gvAuthCd(), param.gvUserCd(), cmpnyCd, siteCd, nodeCd);

        // 마감 여부는 커버리지(자기자신 / 상위(INC_SUB) / 전체) 기준
        boolean closed = attdCloseMapper.countCovering(cmpnyCd, siteCd, nodeCd, closeYm) > 0;
        // 표시용 정확 스코프 행 (상위/전체로 덮인 경우 null 일 수 있음)
        AttdCloseRowResult row = attdCloseMapper.selectCloseRow(cmpnyCd, siteCd, nodeCd, closeYm);

        int pendingReqCnt = attdCloseMapper.countPendingReq(cmpnyCd, siteCd, nodeCd, incSubYn, closeYm);
        int gpsUnconfirmedCnt = attdCloseMapper.countGpsUnconfirmed(cmpnyCd, siteCd, nodeCd, incSubYn, closeYm);
        int unapprovedOtCnt = attdCloseMapper.countUnapprovedOt(cmpnyCd, siteCd, nodeCd, incSubYn, closeYm);
        // 미결 연차 변경(이동/삭제) 요청도 차단 사유 — 마감 후엔 Attd13 마감 가드에 걸려 교착이 된다.
        int pendingLeaveChangeCnt = attdCloseMapper.countPendingLeaveChange(cmpnyCd, siteCd, nodeCd, incSubYn, closeYm);
        int blockTotal = pendingReqCnt + gpsUnconfirmedCnt + unapprovedOtCnt + pendingLeaveChangeCnt;

        boolean closable = !closed && blockTotal == 0;

        List<AttdCloseHistResult> histList = attdCloseMapper.selectCloseHist(cmpnyCd, siteCd, nodeCd, closeYm);

        return AttdCloseStatusResponse.builder()
                .closeYm(closeYm)
                .nodeCd(nodeCd)
                .incSubYn(incSubYn)
                .closeStatus(closed ? STATUS_CLOSED : "OPEN")
                .closed(closed)
                .closeDtime(row == null ? null : row.closeDtime())
                .closeUserCd(row == null ? null : row.closeUserCd())
                .uncloseDtime(row == null ? null : row.uncloseDtime())
                .uncloseUserCd(row == null ? null : row.uncloseUserCd())
                .closeDesc(row == null ? null : row.closeDesc())
                .pendingReqCnt(pendingReqCnt)
                .gpsUnconfirmedCnt(gpsUnconfirmedCnt)
                .unapprovedOtCnt(unapprovedOtCnt)
                .pendingLeaveChangeCnt(pendingLeaveChangeCnt)
                .blockTotalCnt(blockTotal)
                .closable(closable)
                .histList(histList)
                .build();
    }

    @Override
    @Transactional
    public void closeAttendance(AttdCloseParam param) {
        // 사업장 접근 인가(구 토큰 사업장 등식 가드 대체).
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());
        String cmpnyCd = param.gvCmpnyCd();
        String siteCd = param.siteCd();
        String closeYm = param.closeYm();
        String nodeCd = normalizeNode(param.nodeCd());
        String incSubYn = normalizeIncSub(nodeCd, param.incSubNodeYn());

        // 권한: master/hr 또는 해당(상위) 부서 관리자 (정책서 §12.2/§13.4 + PRAFTA-028)
        ensureCanManageScope(param.gvAuthCd(), param.gvUserCd(), cmpnyCd, siteCd, nodeCd);

        // 이미 마감된 (정확) 스코프 재마감 차단
        if (attdCloseMapper.countClosed(cmpnyCd, siteCd, nodeCd, closeYm) > 0) {
            throw new ApiException(AttdErrorCode.ATTD_409_020);
        }

        // 자동/강제 마감 금지(§3.3): 스코프 내 차단 사유가 1건이라도 있으면 마감 불가
        int blockTotal = attdCloseMapper.countPendingReq(cmpnyCd, siteCd, nodeCd, incSubYn, closeYm)
                + attdCloseMapper.countGpsUnconfirmed(cmpnyCd, siteCd, nodeCd, incSubYn, closeYm)
                + attdCloseMapper.countUnapprovedOt(cmpnyCd, siteCd, nodeCd, incSubYn, closeYm)
                + attdCloseMapper.countPendingLeaveChange(cmpnyCd, siteCd, nodeCd, incSubYn, closeYm);
        if (blockTotal > 0) {
            log.warn("근태 마감 차단 - 미결 항목 잔존. cmpnyCd={}, siteCd={}, nodeCd={}, incSub={}, closeYm={}, blockTotal={}",
                    cmpnyCd, siteCd, nodeCd, incSubYn, closeYm, blockTotal);
            throw new ApiException(AttdErrorCode.ATTD_400_040);
        }

        AttdCloseCommand cmd = new AttdCloseCommand(
                cmpnyCd, siteCd, nodeCd, incSubYn, closeYm, param.gvUserCd(), param.closeDesc(), ACTION_CLOSE);
        attdCloseMapper.upsertClose(cmd);
        writeHist(cmd);

        log.info("근태 마감 완료. cmpnyCd={}, siteCd={}, nodeCd={}, incSub={}, closeYm={}, userCd={}",
                cmpnyCd, siteCd, nodeCd, incSubYn, closeYm, param.gvUserCd());
    }

    @Override
    @Transactional
    public void uncloseAttendance(AttdCloseParam param) {
        // 사업장 접근 인가(구 토큰 사업장 등식 가드 대체).
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());
        String cmpnyCd = param.gvCmpnyCd();
        String siteCd = param.siteCd();
        String closeYm = param.closeYm();
        String nodeCd = normalizeNode(param.nodeCd());
        String incSubYn = normalizeIncSub(nodeCd, param.incSubNodeYn());

        ensureCanManageScope(param.gvAuthCd(), param.gvUserCd(), cmpnyCd, siteCd, nodeCd);

        AttdCloseCommand cmd = new AttdCloseCommand(
                cmpnyCd, siteCd, nodeCd, incSubYn, closeYm, param.gvUserCd(), param.closeDesc(), ACTION_UNCLOSE);

        // 해당 (정확) 스코프가 CLOSED 일 때만 갱신됨. 영향 행수 0이면 미마감 스코프 해제 시도.
        int updated = attdCloseMapper.updateUnclose(cmd);
        if (updated == 0) {
            throw new ApiException(AttdErrorCode.ATTD_400_041);
        }
        writeHist(cmd);

        log.info("근태 마감 해제 완료. cmpnyCd={}, siteCd={}, nodeCd={}, closeYm={}, userCd={}",
                cmpnyCd, siteCd, nodeCd, closeYm, param.gvUserCd());
    }

    @Override
    public boolean isClosedForNode(String cmpnyCd, String siteCd, String nodeCd, String closeYm) {
        if (cmpnyCd == null || siteCd == null || closeYm == null) {
            return false;
        }
        return attdCloseMapper.countCovering(cmpnyCd, siteCd, normalizeNode(nodeCd), closeYm) > 0;
    }

    @Override
    public boolean isClosedForUser(String cmpnyCd, String siteCd, String userCd, String closeYm) {
        if (cmpnyCd == null || siteCd == null || userCd == null || closeYm == null) {
            return false;
        }
        String nodeCd = attdCloseMapper.selectUserNodeCd(cmpnyCd, siteCd, userCd);
        return isClosedForNode(cmpnyCd, siteCd, nodeCd, closeYm);
    }

    @Override
    public boolean canManageNode(String authCd, String userCd, String cmpnyCd, String siteCd, String nodeCd) {
        // PRAFTA-042-6: 전사 노드 관리 역할(master/hr/safe)은 전체('*') 포함 즉시 통과.
        if (AuthRoleUtils.canManageAllNodes(authCd)) {
            return true;
        }
        // 그 외(노드 관리자 후보)는 전체('*')/부서 미지정 불가, 본인이 해당/상위 부서 관리자인 경우만 허용
        if (nodeCd == null || nodeCd.isBlank() || WHOLE_SITE.equals(nodeCd)) {
            return false;
        }
        return attdCloseMapper.countNodeAdmin(cmpnyCd, siteCd, nodeCd, userCd) > 0;
    }

    @Override
    public boolean canManageUser(String authCd, String requesterUserCd, String cmpnyCd, String siteCd, String targetUserCd) {
        // PRAFTA-042-6: 전사 노드 관리 역할(master/hr/safe)은 부서 조회 없이 즉시 통과
        //   (safe 도 타사용자 근태 관리 가능해야 함. 대량 셀에서 불필요한 DB 조회 방지)
        if (AuthRoleUtils.canManageAllNodes(authCd)) {
            return true;
        }
        // 그 외(노드 관리자 후보): 대상 사용자의 현재 소속 부서를 서버 조회(클라이언트 nodeCd 불신뢰)
        String targetNodeCd = attdCloseMapper.selectUserNodeCd(cmpnyCd, siteCd, targetUserCd);
        if (targetNodeCd == null || targetNodeCd.isBlank()) {
            return false;
        }
        return canManageNode(authCd, requesterUserCd, cmpnyCd, siteCd, targetNodeCd);
    }

    @Override
    public boolean canManageNodeExcludeSafe(String authCd, String userCd, String cmpnyCd, String siteCd, String nodeCd) {
        // PRAFTA-COM-008-C 작업1: 전사 통과는 master/hr 만(safe 제외). isManager = master/hr.
        if (AuthRoleUtils.isManager(authCd)) {
            return true;
        }
        // 그 외(노드 관리자 후보)는 전체('*')/부서 미지정 불가, 본인이 해당/상위 부서 관리자인 경우만 허용
        if (nodeCd == null || nodeCd.isBlank() || WHOLE_SITE.equals(nodeCd)) {
            return false;
        }
        return attdCloseMapper.countNodeAdmin(cmpnyCd, siteCd, nodeCd, userCd) > 0;
    }

    @Override
    public boolean canManageUserExcludeSafe(String authCd, String requesterUserCd, String cmpnyCd, String siteCd, String targetUserCd) {
        // PRAFTA-COM-008-C 작업1: 전사 통과는 master/hr 만(safe 제외)
        if (AuthRoleUtils.isManager(authCd)) {
            return true;
        }
        String targetNodeCd = attdCloseMapper.selectUserNodeCd(cmpnyCd, siteCd, targetUserCd);
        if (targetNodeCd == null || targetNodeCd.isBlank()) {
            return false;
        }
        return canManageNodeExcludeSafe(authCd, requesterUserCd, cmpnyCd, siteCd, targetNodeCd);
    }

    @Override
    public String resolveUserNodeCd(String cmpnyCd, String siteCd, String userCd) {
        // com-013-06-FU 보안 재작업: 대상 사용자 소속 부서를 서버 권위값으로 해석(클라 nodeCd 불신뢰).
        //   canManageUser 가 사용하는 것과 동일한 TB_USER 기반 출처를 재사용한다.
        if (cmpnyCd == null || siteCd == null || userCd == null) {
            return null;
        }
        return attdCloseMapper.selectUserNodeCd(cmpnyCd, siteCd, userCd);
    }

    // ===== 내부 헬퍼 =====

    /** 빈 부서 = 전체('*') 스코프. */
    private String normalizeNode(String rawNode) {
        return (rawNode == null || rawNode.isBlank()) ? WHOLE_SITE : rawNode;
    }

    /** 전체('*')는 항상 하위 포함. 그 외는 입력값(Y/N), 기본 N. */
    private String normalizeIncSub(String node, String rawIncSub) {
        if (WHOLE_SITE.equals(node)) {
            return "Y";
        }
        return "Y".equals(rawIncSub) ? "Y" : "N";
    }

    /**
     * 마감/해제/조회 스코프 관리 권한 검증 (PRAFTA-028).
     * <ul>
     *   <li>master/hr: 전체('*') 포함 임의 부서 허용.</li>
     *   <li>그 외: 전체('*') 불가(부서 필수) + 본인이 해당 부서 또는 상위 부서의 정/부 관리자여야 한다.</li>
     * </ul>
     */
    private void ensureCanManageScope(String authCd, String userCd, String cmpnyCd, String siteCd, String nodeCd) {
        if (!canManageNode(authCd, userCd, cmpnyCd, siteCd, nodeCd)) {
            log.warn("근태 마감 스코프 권한 없음 - userCd={}, authCd={}, nodeCd={}", userCd, authCd, nodeCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
    }

    private void writeHist(AttdCloseCommand cmd) {
        String histId = attdCloseMapper.selectNextCloseHistId(cmd.cmpnyCd());
        attdCloseMapper.insertCloseHist(
                histId, cmd.cmpnyCd(), cmd.siteCd(), cmd.nodeCd(), cmd.incSubNodeYn(),
                cmd.closeYm(), cmd.actionType(), cmd.userCd(), cmd.closeDesc());
    }
}
