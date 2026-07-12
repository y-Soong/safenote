package com.prafta.web.nearmiss.nearmiss01.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.nearmiss.NearMissErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.nearmiss.nearmiss01.application.command.ChangeStatusCommand;
import com.prafta.web.nearmiss.nearmiss01.application.command.SaveIncidentCommand;
import com.prafta.web.nearmiss.nearmiss01.application.param.ChangeStatusParam;
import com.prafta.web.nearmiss.nearmiss01.application.param.IncidentInfoParam;
import com.prafta.web.nearmiss.nearmiss01.application.param.IncidentListParam;
import com.prafta.web.nearmiss.nearmiss01.application.param.SaveIncidentParam;
import com.prafta.web.nearmiss.nearmiss01.application.query.IncidentInfoQuery;
import com.prafta.web.nearmiss.nearmiss01.application.query.IncidentListQuery;
import com.prafta.web.nearmiss.nearmiss01.dto.response.IncidentInfoResponse;
import com.prafta.web.nearmiss.nearmiss01.dto.response.IncidentListResponse;
import com.prafta.web.nearmiss.nearmiss01.dto.response.StatusCountResponse;
import com.prafta.web.nearmiss.nearmiss01.mapper.NearMiss01Mapper;
import com.prafta.web.nearmiss.nearmiss01.result.IncidentResult;
import com.prafta.web.nearmiss.nearmiss01.result.StatusCountResult;
import com.prafta.web.nearmiss.nearmiss01.service.NearMiss01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NearMiss01ServiceImpl implements NearMiss01Service {

    // SYS063 재번호(D4): 100 접수 / 200 조치중 / 300 완료 / 400 미처리대상.
    private static final String STATUS_RECEIVED    = "100"; // 접수
    private static final String STATUS_ACTING      = "200"; // 조치중
    private static final String STATUS_COMPLETED   = "300"; // 완료
    private static final String STATUS_UNADDRESSED = "400"; // 미처리대상(기존 반려 자리)

    // 처리상태 단계 순서(앞→뒤). 전이는 '전진 점프'를 허용한다.
    //   (정책 A) 활성단계(접수/조치중)에서 더 뒤 단계로 자유롭게 전진 가능(접수→완료 직접 등).
    //   뒤로 가기/같은 단계 전이는 불가, 종결 상태(완료/미처리대상)는 더 이상 전이 불가,
    //   미처리대상(400)은 활성단계 어디서든 가능(선형 밖 종결).
    private static final List<String> STAGE_ORDER = List.of(
        STATUS_RECEIVED, STATUS_ACTING, STATUS_COMPLETED
    );

    private final NearMiss01Mapper nearMiss01Mapper;

    @Override
    public IncidentListResponse selectIncidentList(IncidentListParam param) {
        log.info("아차사고 목록 조회 진입 - cmpnyCd={}, siteCd={}", param.gvCmpnyCd(), param.siteCd());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        List<IncidentResult> incidentResultList =
            nearMiss01Mapper.selectIncidentList(IncidentListQuery.from(param));

        return IncidentListResponse.builder()
            .incidentResultList(incidentResultList)
            .build();
    }

    @Override
    public IncidentInfoResponse selectIncidentInfo(IncidentInfoParam param) {
        log.info("아차사고 상세 조회 진입 - cmpnyCd={}, siteCd={}, nearMissId={}",
            param.gvCmpnyCd(), param.siteCd(), param.nearMissId());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        IncidentResult incidentInfo =
            nearMiss01Mapper.selectIncidentInfo(IncidentInfoQuery.from(param));

        if (incidentInfo == null) {
            throw new ApiException(NearMissErrorCode.NEARMISS_404_001);
        }

        return IncidentInfoResponse.builder()
            .incidentInfo(incidentInfo)
            .build();
    }

    @Override
    public StatusCountResponse selectStatusCounts(IncidentListParam param) {
        log.info("아차사고 상태카운트 조회 진입 - cmpnyCd={}, siteCd={}", param.gvCmpnyCd(), param.siteCd());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        StatusCountResult statusCount =
            nearMiss01Mapper.selectStatusCounts(IncidentListQuery.from(param));

        return StatusCountResponse.builder()
            .statusCount(statusCount)
            .build();
    }

    @Override
    @Transactional
    public void saveIncident(SaveIncidentParam param) {
        log.info("아차사고 정밀조사 저장 진입 - cmpnyCd={}, siteCd={}, nearMissId={}",
            param.gvCmpnyCd(), param.siteCd(), param.nearMissId());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        // 사업장 스코프 존재 확인(IDOR 차단)
        ensureExists(param.gvCmpnyCd(), param.siteCd(), param.nearMissId());

        int updated = nearMiss01Mapper.updateIncident(SaveIncidentCommand.from(param));
        if (updated == 0) {
            throw new ApiException(NearMissErrorCode.NEARMISS_404_001);
        }
        log.info("아차사고 정밀조사 저장 완료 - nearMissId={}", param.nearMissId());
    }

    @Override
    @Transactional
    public void changeStatus(ChangeStatusParam param) {
        log.info("아차사고 상태전환 진입 - cmpnyCd={}, siteCd={}, nearMissId={}, target={}",
            param.gvCmpnyCd(), param.siteCd(), param.nearMissId(), param.reportStatusCd());

        // 사업장 권한 검증 (cross-site IDOR 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        String target = param.reportStatusCd();
        if (!StringUtils.hasText(target)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // 현재 상태 조회(사업장 스코프). 없으면 404.
        String current = nearMiss01Mapper.selectReportStatus(
            new IncidentInfoQuery(param.siteCd(), param.nearMissId(), param.gvCmpnyCd()));
        if (current == null) {
            throw new ApiException(NearMissErrorCode.NEARMISS_404_001);
        }

        // 종결 상태(완료300/미처리대상400)는 더 이상 전이 불가.
        if (STATUS_COMPLETED.equals(current) || STATUS_UNADDRESSED.equals(current)) {
            throw new ApiException(NearMissErrorCode.NEARMISS_422_001);
        }

        if (STATUS_UNADDRESSED.equals(target)) {
            // 미처리대상(400): 활성 단계 어디서든 가능. 단 사유 필수(REJECT_REASON 컬럼=미처리 사유 재활용).
            if (!StringUtils.hasText(param.rejectReason())) {
                throw new ApiException(NearMissErrorCode.NEARMISS_400_001);
            }
        } else {
            // 전진 점프만 허용: target 이 current 보다 뒤 단계여야 한다(같거나 앞 단계면 거부).
            int curIdx = STAGE_ORDER.indexOf(current);
            int tgtIdx = STAGE_ORDER.indexOf(target);
            if (curIdx < 0 || tgtIdx < 0 || tgtIdx <= curIdx) {
                throw new ApiException(NearMissErrorCode.NEARMISS_422_001);
            }
        }

        int updated = nearMiss01Mapper.updateStatus(ChangeStatusCommand.from(param));
        if (updated == 0) {
            throw new ApiException(NearMissErrorCode.NEARMISS_404_001);
        }
        log.info("아차사고 상태전환 완료 - nearMissId={}, {} -> {}",
            param.nearMissId(), current, target);
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────

    /**
     * 사업장(siteCd) 접근 권한 검증 (cross-site IDOR 차단).
     *
     * <p>nearmiss 는 부서(node) 단위가 아니라 사업장 단위이므로 attd11 의 canManageNode 를
     * 그대로 재사용하지 않는다(canManageNode 는 비 master/hr 에 대해 nodeCd 필수 + 노드관리자만 통과 →
     * 사업장 단독 권한 보유자를 부당하게 차단). 대신 전사 권한(master/hr)은 예외로 전체 허용하고
     * (예외 판정 기준은 canManageNode 내부의 {@link AuthRoleUtils#isManager}와 동일하게 맞춤),
     * 그 외 사용자는 tb_user_site_auth(USE_YN='Y') 매핑 보유 여부로 사업장 접근을 판정한다.
     */
    private void assertSiteAccess(String authCd, String userCd, String cmpnyCd, String siteCd) {
        // 전사 권한(master/hr): 모든 사업장 접근 허용 (canManageNode 와 동일한 전사권한 예외 기준)
        if (AuthRoleUtils.isManager(authCd)) {
            return;
        }
        // 사업장 미지정이면 사업장 단위 검증 불가 → 차단
        if (!StringUtils.hasText(siteCd)) {
            log.warn("아차사고 사업장 권한 없음(siteCd 미지정) - userCd={}, authCd={}", userCd, authCd);
            throw new ApiException(NearMissErrorCode.NEARMISS_403_001);
        }
        // 그 외: tb_user_site_auth 매핑 보유 시에만 허용
        if (nearMiss01Mapper.countUserSiteAuth(cmpnyCd, userCd, siteCd) == 0) {
            log.warn("아차사고 사업장 권한 없음 - userCd={}, authCd={}, siteCd={}", userCd, authCd, siteCd);
            throw new ApiException(NearMissErrorCode.NEARMISS_403_001);
        }
    }

    private void ensureExists(String gvCmpnyCd, String siteCd, String nearMissId) {
        String status = nearMiss01Mapper.selectReportStatus(
            new IncidentInfoQuery(siteCd, nearMissId, gvCmpnyCd));
        if (status == null) {
            throw new ApiException(NearMissErrorCode.NEARMISS_404_001);
        }
    }
}
