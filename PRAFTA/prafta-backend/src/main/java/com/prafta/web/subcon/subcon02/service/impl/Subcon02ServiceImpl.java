package com.prafta.web.subcon.subcon02.service.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.error.subcon.SubconErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.subcon.subcon02.application.command.MirrorSiteInsertCommand;
import com.prafta.web.subcon.subcon02.application.command.SiteLinkInsertCommand;
import com.prafta.web.subcon.subcon02.application.command.SiteLinkProcessCommand;
import com.prafta.web.subcon.subcon02.application.param.ChkptLinkParam;
import com.prafta.web.subcon.subcon02.application.param.SiteLinkListParam;
import com.prafta.web.subcon.subcon02.application.param.SiteLinkProcessParam;
import com.prafta.web.subcon.subcon02.application.param.SiteLinkProposeParam;
import com.prafta.web.subcon.subcon02.dto.response.LinkProposeCandidatesResponse;
import com.prafta.web.subcon.subcon02.dto.response.SiteLinkListResponse;
import com.prafta.web.subcon.subcon02.dto.response.SiteLinkProposeResponse;
import com.prafta.web.subcon.subcon02.mapper.ChkptLinkMapper;
import com.prafta.web.subcon.subcon02.mapper.Subcon02Mapper;
import com.prafta.web.subcon.subcon02.result.MySiteResult;
import com.prafta.web.subcon.subcon02.result.RelationCmpnyResult;
import com.prafta.web.subcon.subcon02.result.SiteLinkRaw;
import com.prafta.web.subcon.subcon02.result.SiteLinkResult;
import com.prafta.web.subcon.subcon02.result.SiteLinkSrcRaw;
import com.prafta.web.subcon.subcon02.result.SiteSrcRaw;
import com.prafta.web.subcon.subcon02.service.ChkptLinkMirrorService;
import com.prafta.web.subcon.subcon02.service.SiteLinkTerminationListener;
import com.prafta.web.subcon.subcon02.service.Subcon02Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사업장 연동(Subcon_02) 서비스 — PRAFTA-SUBCON-T2.
 *
 * <p>보안 원칙(T1 승계 + T2 요청서 §6):
 * 회사 스코프는 JWT 클레임(gvCmpnyCd)만 신뢰, 상태 전이는 조건부 UPDATE(0행=404 존재 비노출),
 * 미러 생성의 INSERT 대상 CMPNY_CD 는 gvCmpnyCd 강제(타 테넌트 쓰기 봉인),
 * SRC 데이터 read 는 링크 당사자 검증(수락 선점) 후에만 수행,
 * 루프 가드는 서버 데이터(tb_site.LINK_SRC)만으로 순회(클라 입력 불신).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Subcon02ServiceImpl implements Subcon02Service {

    private final Subcon02Mapper subcon02Mapper;

    /** PRAFTA-SUBCON-T6: 점검 연동 전용 매퍼/미러 서비스(T2 매퍼·SQL 무수정 — 별도 매퍼로 분리). */
    private final ChkptLinkMapper chkptLinkMapper;
    private final ChkptLinkMirrorService chkptLinkMirrorService;

    /**
     * PRAFTA-SUBCON-T6-08: 사업장 링크 해지 시 산하 연동 자동 독립화 리스너(T6 구현체 = 점검 미러).
     * List 직접 주입은 후보 빈 0개일 때 기동 실패하므로 ObjectProvider 로 빈 스트림을 허용한다(T1 훅 패턴 동형).
     */
    private final ObjectProvider<SiteLinkTerminationListener> siteLinkTerminationListeners;

    /** 본 화면 메뉴 식별자(서버측 역할 게이트 기준). */
    private static final String MENU_D_ID = "Subcon_02";
    /** 메뉴 버튼 권한 종류(고정 상수 — 동적 컬럼 주입 금지). §4 매핑: 조회=SRCH, 제안=NEW, 수락/거부/취소=SAVE, 해지=DELT. */
    private static final String BTN_SRCH = "SRCH";
    private static final String BTN_NEW = "NEW";
    private static final String BTN_SAVE = "SAVE";
    private static final String BTN_DELT = "DELT";

    /** 목록 조회 상한(전수조회 방지 — Subcon01 LIST_LIMIT 준용). */
    private static final int LIST_LIMIT = 500;

    /** 처리 코멘트(거부 사유 등) 최대 길이(DDL varchar(500) 정합). */
    private static final int COMMENT_MAX_LEN = 500;

    /** 루프 가드 조상 순회 깊이 안전핀(§5-3 #4 — 초과 시 데이터 오염 감지). */
    private static final int MAX_CHAIN_DEPTH = 20;

    /** 로그 위조 방지용 외부 입력 정제 — 개행 제거 + 50자 상한(T1 SEC-ADV-1 승계). */
    private String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim().replaceAll("[\\r\\n]", "");
        return cleaned.length() > 50 ? cleaned.substring(0, 50) + "..." : cleaned;
    }

    @Override
    public SiteLinkListResponse selectSiteLinkList(SiteLinkListParam param) {
        log.info("사업장 연동 목록 조회 진입 - gvCmpnyCd={}", param.gvCmpnyCd());

        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_SRCH);

        List<SiteLinkResult> links = subcon02Mapper.selectSiteLinkList(param.gvCmpnyCd(), LIST_LIMIT);

        log.info("사업장 연동 목록 조회 종료 - gvCmpnyCd={}, rows={}", param.gvCmpnyCd(), links.size());

        return SiteLinkListResponse.builder()
                .links(links)
                .build();
    }

    @Override
    public LinkProposeCandidatesResponse selectProposeCandidates(SiteLinkListParam param) {
        log.info("연동 제안 후보 조회 진입 - gvCmpnyCd={}", param.gvCmpnyCd());

        // 제안 플로우 전용 조회 → BTN_NEW 게이트(§4 버튼-액션 매핑).
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_NEW);

        List<RelationCmpnyResult> cmpnyList = subcon02Mapper.selectActiveRelationCmpnyList(param.gvCmpnyCd());
        List<MySiteResult> siteList = subcon02Mapper.selectMyActiveSiteList(param.gvCmpnyCd());

        log.info("연동 제안 후보 조회 종료 - gvCmpnyCd={}, cmpny={}건, site={}건",
                param.gvCmpnyCd(), cmpnyList.size(), siteList.size());

        return LinkProposeCandidatesResponse.builder()
                .cmpnyList(cmpnyList)
                .siteList(siteList)
                .build();
    }

    @Override
    @Transactional
    public SiteLinkProposeResponse proposeSiteLink(SiteLinkProposeParam param) {
        // 0) 서버측 메뉴 권한 게이트(제안 생성 — BTN_NEW). 진입 로그는 게이트 후 + 정제값.
        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_NEW);
        log.info("사업장 연동 제안 진입 - gvCmpnyCd={}, tgtCmpnyCd={}, siteCd={}",
                param.gvCmpnyCd(), sanitizeForLog(param.tgtCmpnyCd()), sanitizeForLog(param.siteCd()));

        // 1) 필수값 + 자기 회사 대상 금지(§5-3 #3).
        if (param.tgtCmpnyCd() == null || param.tgtCmpnyCd().isBlank()
                || param.siteCd() == null || param.siteCd().isBlank()) {
            throw new ApiException(SubconErrorCode.SUBCON_400_001);
        }
        String tgtCmpnyCd = param.tgtCmpnyCd().trim();
        String siteCd = param.siteCd().trim();
        if (tgtCmpnyCd.equals(param.gvCmpnyCd())) {
            throw new ApiException(SubconErrorCode.SUBCON_400_002);
        }

        // 2) 사업장 소유 검증(§5-3 #1) — 제안측 소유 활성 사업장만(타사 사업장 제안 봉인). 미존재 404.
        if (subcon02Mapper.selectMySiteActiveCnt(param.gvCmpnyCd(), siteCd) <= 0) {
            throw new ApiException(SubconErrorCode.SUBCON_404_004);
        }

        // 3) 관계 ACCEPTED 검증(§5-3 #2) — 방향 불문. RELATION_ID 를 링크에 기록.
        Long relationId = subcon02Mapper.selectActiveRelationId(param.gvCmpnyCd(), tgtCmpnyCd);
        if (relationId == null) {
            throw new ApiException(SubconErrorCode.SUBCON_409_004);
        }

        // 4) 루프 가드(§5-3 #4) — 제안 사업장의 LINK_SRC 체인을 원본까지 순회해
        //    조상 회사 집합(자기 포함)에 대상 회사가 있으면 차단. 서버 데이터만 사용.
        Set<String> ancestorCmpnySet = collectAncestorCmpnySet(param.gvCmpnyCd(), siteCd);
        if (ancestorCmpnySet.contains(tgtCmpnyCd)) {
            throw new ApiException(SubconErrorCode.SUBCON_409_003);
        }

        // 5) 활성 중복 가드(§5-3 #5) — FOR UPDATE 직렬화 + UX_SITE_LINK_ACTIVE DB 백스톱 이중.
        if (subcon02Mapper.selectActiveLinkCntForUpdate(param.gvCmpnyCd(), siteCd, tgtCmpnyCd) > 0) {
            throw new ApiException(SubconErrorCode.SUBCON_409_002);
        }

        // 6) INSERT — 동시 제안 레이스는 UNIQUE 백스톱이 DuplicateKeyException 으로 수렴.
        SiteLinkInsertCommand command = new SiteLinkInsertCommand(
                relationId
                , param.gvCmpnyCd()
                , siteCd
                , tgtCmpnyCd
                , param.gvUserCd()
                , param.gvUserCd());
        try {
            subcon02Mapper.insertSiteLink(command);
        } catch (DuplicateKeyException e) {
            log.info("사업장 연동 제안 - 활성 중복(UNIQUE 백스톱) gvCmpnyCd={}, siteCd={}, tgtCmpnyCd={}",
                    param.gvCmpnyCd(), siteCd, tgtCmpnyCd);
            throw new ApiException(SubconErrorCode.SUBCON_409_002);
        }

        log.info("사업장 연동 제안 종료 - gvCmpnyCd={}, siteCd={}, tgtCmpnyCd={}, linkId={}",
                param.gvCmpnyCd(), siteCd, tgtCmpnyCd, command.getLinkId());

        return SiteLinkProposeResponse.builder()
                .linkId(command.getLinkId())
                .build();
    }

    @Override
    @Transactional
    public void acceptSiteLink(SiteLinkProcessParam param) {
        log.info("사업장 연동 수락 진입 - gvCmpnyCd={}, linkId={}", param.gvCmpnyCd(), param.linkId());

        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_SAVE);
        validateLinkId(param.linkId());
        validateCommentLength(param.comment());

        // 1) 조건부 UPDATE 선점(PROPOSED→ACTIVE + DST 소속 + 관계 ACCEPTED 존속) — 동시 수락 레이스 차단.
        int updated = subcon02Mapper.acceptSiteLinkPreempt(toProcessCommand(param));
        if (updated <= 0) {
            throw new ApiException(SubconErrorCode.SUBCON_404_003);
        }

        // 선점 성공으로 당사자성 기증명 — 링크 원시행 회수(SRC 좌표).
        SiteLinkRaw link = subcon02Mapper.selectSiteLinkById(param.linkId());

        // 2) SRC 사업장 원시행 read(cross-tenant read — 당사자 검증 후) — 미존재 시 전체 롤백.
        SiteSrcRaw srcSite = subcon02Mapper.selectSiteSrcRaw(link.srcCmpnyCd(), link.srcSiteCd());
        if (srcSite == null) {
            log.warn("사업장 연동 수락 실패 - 원본 사업장 미존재 linkId={}, src={}:{}",
                    link.linkId(), link.srcCmpnyCd(), link.srcSiteCd());
            throw new ApiException(SubconErrorCode.SUBCON_404_004);
        }

        // 3) DST_SITE_CD 채번(baim01 시퀀스 재사용 — DST=gv 회사 기준, 기존 사업장과 충돌 없음).
        String dstSiteCd = subcon02Mapper.selectNewMirrorSiteCd(param.gvCmpnyCd());

        // 4) SITE_NO 보정(D4) — DST 회사 내 중복이면 "SITE_NO-{LINK_ID}" 접미(수락 실패 대신 결정적 보정).
        String mirrorSiteNo = srcSite.siteNo();
        if (subcon02Mapper.selectSiteNoDupCnt(param.gvCmpnyCd(), mirrorSiteNo) > 0) {
            mirrorSiteNo = mirrorSiteNo + "-" + link.linkId();
        }

        // 5) TB_SITE 복제(INSERT...SELECT 원본행 — SITE_ADMIN_CD=NULL, LINK_SRC_* 세팅).
        //    INSERT 대상 CMPNY_CD 는 gvCmpnyCd 강제(타 테넌트 쓰기 봉인).
        int inserted = subcon02Mapper.insertMirrorSite(new MirrorSiteInsertCommand(
                link.srcCmpnyCd()
                , link.srcSiteCd()
                , param.gvCmpnyCd()
                , dstSiteCd
                , mirrorSiteNo
                , param.gvUserCd()));
        if (inserted <= 0) {
            // 선점과 read 사이 원본 소실 등 — 전체 롤백(링크 PROPOSED 복원).
            throw new ApiException(SubconErrorCode.SUBCON_404_004);
        }

        // 6) 기본 부서노드 1개(루트 n1 — 소속이동 목적지 확보. 이후 트리는 수신사 자율).
        subcon02Mapper.insertMirrorRootNode(param.gvCmpnyCd(), dstSiteCd, srcSite.siteNm(), param.gvUserCd());

        // 7) 전사역할 사업장권한 자동부여(DST 회사 기준 — 수신사 관리자 화면 노출 확보).
        subcon02Mapper.mergeMasterSiteAuthSet(param.gvCmpnyCd(), dstSiteCd, param.gvUserCd());

        // 8) 활성 근무타입 전량 복제(SCH_CD=원본 그대로 — D3. 과거 HIST 미복제 — 현재본만).
        int schCnt = subcon02Mapper.insertMirrorSchAll(
                link.srcCmpnyCd(), link.srcSiteCd(), param.gvCmpnyCd(), dstSiteCd, param.gvUserCd());

        // 8-1) SHIFT-LINK-T2: 교대 정의 4테이블 복제(TEAM_MGMT/TEAM_USER 실인원 계열 제외 — 지시서 §2).
        //      패턴/배정표의 SCH_CD 참조 정합을 위해 근무타입 복제(8번) 직후에만 호출한다(지시서 §4-1).
        //      INSERT 대상 CMPNY_CD 는 gvCmpnyCd 강제(타 테넌트 쓰기 봉인 — 5번 단계 원칙 동일).
        int shiftCnt = subcon02Mapper.insertMirrorShiftAll(
                link.srcCmpnyCd(), link.srcSiteCd(), param.gvCmpnyCd(), dstSiteCd, param.gvUserCd());
        subcon02Mapper.insertMirrorShiftPtrnAll(
                link.srcCmpnyCd(), link.srcSiteCd(), param.gvCmpnyCd(), dstSiteCd, param.gvUserCd());
        subcon02Mapper.insertMirrorShiftTeamMetaAll(
                link.srcCmpnyCd(), link.srcSiteCd(), param.gvCmpnyCd(), dstSiteCd, param.gvUserCd());
        subcon02Mapper.insertMirrorShiftAssignAll(
                link.srcCmpnyCd(), link.srcSiteCd(), param.gvCmpnyCd(), dstSiteCd, param.gvUserCd());

        // 9) 링크 행에 DST_SITE_CD 기록.
        subcon02Mapper.updateSiteLinkDstSite(link.linkId(), dstSiteCd, param.gvUserCd());

        log.info("미러 생성 — link={}, 사업장 {}:{} -> {}:{}, 근무타입 {}건, 교대타입 {}건",
                link.linkId(), link.srcCmpnyCd(), link.srcSiteCd(), param.gvCmpnyCd(), dstSiteCd, schCnt, shiftCnt);
    }

    @Override
    @Transactional
    public void rejectSiteLink(SiteLinkProcessParam param) {
        log.info("사업장 연동 거부 진입 - gvCmpnyCd={}, linkId={}", param.gvCmpnyCd(), param.linkId());

        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_SAVE);
        validateLinkId(param.linkId());

        // 거부 사유 필수 + 길이 제한(§4 #3).
        if (param.comment() == null || param.comment().isBlank()) {
            throw new ApiException(SubconErrorCode.SUBCON_400_003);
        }
        validateCommentLength(param.comment());

        int updated = subcon02Mapper.rejectSiteLink(toProcessCommand(param));
        if (updated <= 0) {
            throw new ApiException(SubconErrorCode.SUBCON_404_003);
        }

        log.info("사업장 연동 거부 종료 - gvCmpnyCd={}, linkId={}", param.gvCmpnyCd(), param.linkId());
    }

    @Override
    @Transactional
    public void cancelSiteLink(SiteLinkProcessParam param) {
        log.info("사업장 연동 취소 진입 - gvCmpnyCd={}, linkId={}", param.gvCmpnyCd(), param.linkId());

        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_SAVE);
        validateLinkId(param.linkId());
        validateCommentLength(param.comment());

        int updated = subcon02Mapper.cancelSiteLink(toProcessCommand(param));
        if (updated <= 0) {
            throw new ApiException(SubconErrorCode.SUBCON_404_003);
        }

        log.info("사업장 연동 취소 종료 - gvCmpnyCd={}, linkId={}", param.gvCmpnyCd(), param.linkId());
    }

    @Override
    @Transactional
    public void terminateSiteLink(SiteLinkProcessParam param) {
        log.info("사업장 연동 해지 진입 - gvCmpnyCd={}, linkId={}", param.gvCmpnyCd(), param.linkId());

        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_DELT);
        validateLinkId(param.linkId());
        validateCommentLength(param.comment());

        // 1) 조건부 UPDATE(ACTIVE + 양측 중 어느 쪽이든) — 0행이면 통합 404.
        int updated = subcon02Mapper.terminateSiteLink(toProcessCommand(param));
        if (updated <= 0) {
            throw new ApiException(SubconErrorCode.SUBCON_404_003);
        }

        // 2) 미러 독립화(§5-6) — 해당 링크의 수신 미러만 LINK_SRC NULL 화(일반 사업장 전환).
        //    하위 체인 링크·TB_CMPNY 는 절대 무접촉(정오표 1·2 — 수신사가 새 루트).
        SiteLinkRaw link = subcon02Mapper.selectSiteLinkById(param.linkId());
        independizeMirror(link, param.gvUserCd());

        // 3) PRAFTA-SUBCON-T6-08: 산하 연동(점검 구성) 자동 독립화 훅 — 동일 트랜잭션(예외 시 해지 전체 롤백).
        siteLinkTerminationListeners.orderedStream()
                .forEach(listener -> listener.onSiteLinkTerminated(link, param.gvUserCd()));

        log.info("사업장 연동 해지 종료 - gvCmpnyCd={}, linkId={}", param.gvCmpnyCd(), param.linkId());
    }

    // =========================== 순회점검 구성 연동(PRAFTA-SUBCON-T6-02) ===========================

    @Override
    @Transactional
    public void enableChkptLink(ChkptLinkParam param) {
        log.info("순회점검 구성 연동 실행 진입 - gvCmpnyCd={}, linkId={}", param.gvCmpnyCd(), param.linkId());

        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_SAVE);
        validateLinkId(param.linkId());

        // 1) 조건부 UPDATE 선점(사업장 링크 ACTIVE + 점검연동 NONE + 행위자 = SRC 소속) — 0행이면 통합 404.
        int updated = chkptLinkMapper.enableChkptLinkPreempt(
                param.linkId(), param.gvCmpnyCd(), param.gvUserCd());
        if (updated <= 0) {
            throw new ApiException(SubconErrorCode.SUBCON_404_006);
        }

        // 선점 성공으로 당사자성 기증명 — 링크 원시행 회수(SRC/DST 좌표).
        SiteLinkRaw link = subcon02Mapper.selectSiteLinkById(param.linkId());
        if (link == null || link.dstSiteCd() == null) {
            throw new ApiException(SubconErrorCode.SUBCON_404_006);
        }

        // 2) 점검 구성 미러 생성 + 점검연동 ACTIVE 하위 체인 재귀 확장(단일 트랜잭션 — 중간 실패 시 전체 롤백).
        chkptLinkMirrorService.mirrorChkptConfig(
                link.srcCmpnyCd(), link.srcSiteCd(), link.dstCmpnyCd(), link.dstSiteCd());

        log.info("순회점검 구성 연동 실행 종료 - link={}, {}:{} -> {}:{}",
                link.linkId(), link.srcCmpnyCd(), link.srcSiteCd(), link.dstCmpnyCd(), link.dstSiteCd());
    }

    @Override
    @Transactional
    public void disableChkptLink(ChkptLinkParam param) {
        log.info("순회점검 구성 연동 해제 진입 - gvCmpnyCd={}, linkId={}", param.gvCmpnyCd(), param.linkId());

        assertMenuButton(param.gvCmpnyCd(), param.gvAuthCd(), param.gvUserCd(), BTN_DELT);
        validateLinkId(param.linkId());

        // 1) 조건부 UPDATE 선점(점검연동 ACTIVE + 양측 중 어느 쪽이든) — 0행이면 통합 404.
        int updated = chkptLinkMapper.disableChkptLinkPreempt(
                param.linkId(), param.gvCmpnyCd(), param.gvUserCd());
        if (updated <= 0) {
            throw new ApiException(SubconErrorCode.SUBCON_404_006);
        }

        // 2) 점검 미러 독립화(LINK_SRC NULL 화) — 잠금 해제 + 결과 통합 중단. 기존 실적은 전량 보존.
        SiteLinkRaw link = subcon02Mapper.selectSiteLinkById(param.linkId());
        chkptLinkMirrorService.independizeChkptMirror(link, param.gvUserCd());

        log.info("순회점검 구성 연동 해제 종료 - gvCmpnyCd={}, linkId={}", param.gvCmpnyCd(), param.linkId());
    }

    // =========================== 독립화 공용(해지 + 관계 해지 훅) ===========================

    /**
     * 미러 독립화 — 수신 미러의 TB_SITE + 소속 TB_SCH_MGMT 전량 LINK_SRC_* NULL 화.
     * 데이터 이동·근로자 회수·회사 상태 변경 없음(불변식 — TB_CMPNY 무작용).
     * DST_SITE_CD 가 NULL(미수락 링크)이면 정리할 미러가 없어 no-op.
     */
    void independizeMirror(SiteLinkRaw link, String actionUserCd) {
        if (link == null || link.dstSiteCd() == null) {
            return;
        }
        int siteCleared = subcon02Mapper.clearSiteLinkSrc(
                link.dstCmpnyCd(), link.dstSiteCd(), link.srcCmpnyCd(), actionUserCd);
        int schCleared = subcon02Mapper.clearSchLinkSrc(
                link.dstCmpnyCd(), link.dstSiteCd(), link.srcCmpnyCd(), actionUserCd);
        // SHIFT-LINK-T5: 교대근무 타입도 독립화(NULL 화만 — 데이터 유지, 하위 3테이블 무접촉).
        int shiftCleared = subcon02Mapper.clearShiftLinkSrc(
                link.dstCmpnyCd(), link.dstSiteCd(), link.srcCmpnyCd(), actionUserCd);

        log.info("미러 독립화 — link={}, {}:{} 일반 전환(site={}행, sch={}행, shift={}행)",
                link.linkId(), link.dstCmpnyCd(), link.dstSiteCd(), siteCleared, schCleared, shiftCleared);
    }

    // =========================== private ===========================

    /**
     * 루프 가드 조상 순회(§5-3 #4) — tb_site.LINK_SRC 를 따라 원본까지 거슬러 조상 회사 집합(자기 포함)을
     * 수집한다. 깊이 안전핀 초과 시 데이터 오염 감지로 중단(SUBCON_500_001).
     */
    private Set<String> collectAncestorCmpnySet(String cmpnyCd, String siteCd) {
        Set<String> ancestors = new HashSet<>();
        ancestors.add(cmpnyCd);

        String curCmpnyCd = cmpnyCd;
        String curSiteCd = siteCd;
        int depth = 0;

        while (true) {
            SiteLinkSrcRaw src = subcon02Mapper.selectSiteLinkSrc(curCmpnyCd, curSiteCd);
            if (src == null || src.linkSrcCmpnyCd() == null) {
                break; // 원본(체인 루트) 도달 또는 행 미존재 — 순회 종료.
            }
            ancestors.add(src.linkSrcCmpnyCd());
            curCmpnyCd = src.linkSrcCmpnyCd();
            curSiteCd = src.linkSrcSiteCd();

            if (++depth > MAX_CHAIN_DEPTH) {
                log.error("루프 가드 조상 순회 깊이 초과(데이터 오염 의심) - 기점={}:{}", cmpnyCd, siteCd);
                throw new ApiException(SubconErrorCode.SUBCON_500_001);
            }
        }
        return ancestors;
    }

    /**
     * 서버측 메뉴 권한 게이트 — Subcon_02 메뉴의 지정 버튼권한(BTN_SRCH/NEW/SAVE/DELT)을 보유한 역할만 통과.
     * authCd 는 JWT 클레임 도출값만 신뢰한다(Subcon01 패턴 미러).
     */
    private void assertMenuButton(String cmpnyCd, String authCd, String userCd, String btnType) {
        if (authCd == null || authCd.isBlank()
                || subcon02Mapper.selectMenuButtonAuthCnt(cmpnyCd, authCd, MENU_D_ID, btnType) <= 0) {
            log.warn("사업장 연동 관리 권한 없음(역할 게이트 차단) - userCd={}, authCd={}, btnType={}", userCd, authCd, btnType);
            throw new ApiException(SubconErrorCode.SUBCON_403_001);
        }
    }

    private void validateLinkId(Long linkId) {
        if (linkId == null) {
            throw new ApiException(SubconErrorCode.SUBCON_400_001);
        }
    }

    /** 코멘트 길이 제한(DDL varchar(500) — truncation/500 방지). null/빈 값은 통과(거부만 별도 필수 검증). */
    private void validateCommentLength(String comment) {
        if (comment != null && comment.length() > COMMENT_MAX_LEN) {
            throw new ApiException(SubconErrorCode.SUBCON_400_004);
        }
    }

    private SiteLinkProcessCommand toProcessCommand(SiteLinkProcessParam param) {
        return new SiteLinkProcessCommand(
                param.linkId()
                , param.gvCmpnyCd()
                , param.gvUserCd()
                , param.comment());
    }
}
