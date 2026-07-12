package com.prafta.web.acct.acct01.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.common.error.acct.AcctErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.acct.acct01.application.command.AcctInsertCommand;
import com.prafta.web.acct.acct01.application.command.LinkQueryContext;
import com.prafta.web.acct.acct01.application.param.AcctCreateParam;
import com.prafta.web.acct.acct01.application.param.AcctDeleteParam;
import com.prafta.web.acct.acct01.application.param.AcctInfoParam;
import com.prafta.web.acct.acct01.application.param.AcctListParam;
import com.prafta.web.acct.acct01.application.param.AcctUpdateParam;
import com.prafta.web.acct.acct01.application.param.AttdTbmPrintParam;
import com.prafta.web.acct.acct01.application.param.ChkptOptionParam;
import com.prafta.web.acct.acct01.application.param.LegalStepListParam;
import com.prafta.web.acct.acct01.application.param.LegalStepSaveParam;
import com.prafta.web.acct.acct01.application.param.LinkConfirmParam;
import com.prafta.web.acct.acct01.application.param.LinkQueryParam;
import com.prafta.web.acct.acct01.application.param.LinkSnapshotParam;
import com.prafta.web.acct.acct01.application.param.RiskAssessmentPrintParam;
import com.prafta.web.acct.acct01.application.param.RiskCategoryOptionParam;
import com.prafta.web.acct.acct01.application.param.VictimSearchParam;
import com.prafta.web.acct.acct01.dto.request.LinkConfirmRequest;
import com.prafta.web.acct.acct01.dto.response.AcctCreateResponse;
import com.prafta.web.acct.acct01.dto.response.AcctInfoResponse;
import com.prafta.web.acct.acct01.dto.response.AcctListResponse;
import com.prafta.web.acct.acct01.dto.response.AttdTbmPrintResponse;
import com.prafta.web.acct.acct01.dto.response.AttendanceLinkResponse;
import com.prafta.web.acct.acct01.dto.response.ChkptOptionResponse;
import com.prafta.web.acct.acct01.dto.response.LegalStepHistoryResponse;
import com.prafta.web.acct.acct01.dto.response.LegalStepListResponse;
import com.prafta.web.acct.acct01.dto.response.LinkSnapshotResponse;
import com.prafta.web.acct.acct01.dto.response.PatrolLinkResponse;
import com.prafta.web.acct.acct01.dto.response.RiskCategoryOptionResponse;
import com.prafta.web.acct.acct01.dto.response.RiskLinkResponse;
import com.prafta.web.acct.acct01.dto.response.TbmLinkResponse;
import com.prafta.web.acct.acct01.dto.response.VictimSearchResponse;
import com.prafta.web.acct.acct01.mapper.Acct01Mapper;
import com.prafta.web.acct.acct01.result.AcctResult;
import com.prafta.web.acct.acct01.result.RiskAssessmentDetailResult;
import com.prafta.web.acct.acct01.result.ScheduleLinkResult;
import com.prafta.web.acct.acct01.service.Acct01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Acct01ServiceImpl implements Acct01Service {

    // 화면 안내문구(설계문서 §6 / 작업지시서 §5.2). FE 가 각 원 화면에 표시.
    private static final String NOTICE_PATROL =
        "사고일로부터 1주일 이내 점검 결과를 집계합니다. (양호/불량 기준)";
    private static final String NOTICE_RISK =
        "사고일로부터 최근 3개월 이내 유효 위험성평가입니다. (사고 날짜·시각 기준 조회)";
    private static final String NOTICE_TBM =
        "사고 발생 당일 진행된 TBM만 표시합니다. (당일 기준 고정)";
    private static final String NOTICE_ATTD =
        "모든 항목은 본 시스템 기록 기준이며, '기록 없음'은 행위 부재가 아니라 입력 부재일 수 있습니다.";
    private static final String NOTICE_LEGAL =
        "법정 기한·조문은 노무사 최종확인 대상이며, 본 화면은 실무 보조용입니다.";

    private static final String SCHEDULE_NOTE_DAILY = "일용직은 스케줄 없음";

    // 연계 도메인 구분(SYS067)
    private static final String DOMAIN_ATTD = "ATTD";
    private static final String DOMAIN_CHKPT = "CHKPT";
    private static final String DOMAIN_RISK = "RISK";
    private static final String DOMAIN_TBM = "TBM";

    private static final String USER_TYPE_DAILY = "DAILY";

    private final Acct01Mapper acct01Mapper;

    // ── 048-03 CRUD ──────────────────────────────────────────────

    @Override
    public AcctListResponse selectAcctList(AcctListParam param) {
        log.info("사고 목록 조회 진입 - cmpnyCd={}, siteCd={}", param.gvCmpnyCd(), param.siteCd());

        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        List<AcctResult> acctList = acct01Mapper.selectAcctList(param);

        return AcctListResponse.builder()
            .acctList(acctList)
            .build();
    }

    @Override
    public AcctInfoResponse selectAcctInfo(AcctInfoParam param) {
        log.info("사고 상세 조회 진입 - cmpnyCd={}, siteCd={}, acctId={}",
            param.gvCmpnyCd(), param.siteCd(), param.acctId());

        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        AcctResult info = acct01Mapper.selectAcctInfo(param);
        if (info == null) {
            throw new ApiException(AcctErrorCode.ACCT_404_001);
        }

        return AcctInfoResponse.builder()
            .acctInfo(info)
            .build();
    }

    @Override
    @Transactional
    public AcctCreateResponse createAcct(AcctCreateParam param) {
        log.info("사고 등록 진입 - cmpnyCd={}, siteCd={}, victimUserCd={}",
            param.gvCmpnyCd(), param.siteCd(), param.victimUserCd());

        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());
        assertCanWrite(param.gvAuthCd(), param.gvUserCd());

        // 필수값 검증
        if (!StringUtils.hasText(param.victimUserTypeCd())
            || !StringUtils.hasText(param.victimUserCd())
            || !StringUtils.hasText(param.occurYmd())
            || !StringUtils.hasText(param.occurTime())
            || !StringUtils.hasText(param.acctGradeCd())
            || !StringUtils.hasText(param.acctDesc())) {
            throw new ApiException(AcctErrorCode.ACCT_400_001);
        }

        // 재해자 실재/소속 검증: body 의 재해자가 권한검증을 통과한 회사+사업장에 실재하는지 확인.
        // siteCd 는 assertSiteAccess 로 검증된 값만 사용(body siteCd 단독 신뢰 금지 원칙 유지).
        // victimUserTypeCd 화이트리스트(REGULAR/DAILY) 밖이거나 매칭 0 건이면 차단(유령/타 사업장 IDOR).
        if (acct01Mapper.countVictim(
                param.gvCmpnyCd(), param.siteCd(), param.victimUserTypeCd(), param.victimUserCd()) == 0) {
            log.warn("재해자 실재/소속 검증 실패 - cmpnyCd={}, siteCd={}, victimUserTypeCd={}, victimUserCd={}",
                param.gvCmpnyCd(), param.siteCd(), param.victimUserTypeCd(), param.victimUserCd());
            throw new ApiException(AcctErrorCode.ACCT_400_002);
        }

        // 채번: ACC + YYYYMMDD(발생일) + 4자리 SEQ (사업장+발생일 기준)
        String acctId = acct01Mapper.selectNextAcctId(
            param.gvCmpnyCd(), param.siteCd(), param.occurYmd());

        acct01Mapper.insertAcct(AcctInsertCommand.from(param, acctId));

        log.info("사고 등록 완료 - acctId={}", acctId);

        return AcctCreateResponse.builder()
            .acctId(acctId)
            .build();
    }

    @Override
    @Transactional
    public void updateAcct(AcctUpdateParam param) {
        log.info("사고 수정 진입 - cmpnyCd={}, siteCd={}, acctId={}",
            param.gvCmpnyCd(), param.siteCd(), param.acctId());

        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());
        assertCanWrite(param.gvAuthCd(), param.gvUserCd());

        if (!StringUtils.hasText(param.acctId())) {
            throw new ApiException(AcctErrorCode.ACCT_400_001);
        }

        int updated = acct01Mapper.updateAcct(param);
        if (updated == 0) {
            throw new ApiException(AcctErrorCode.ACCT_404_001);
        }
        log.info("사고 수정 완료 - acctId={}", param.acctId());
    }

    @Override
    @Transactional
    public void deleteAcct(AcctDeleteParam param) {
        log.info("사고 삭제 진입 - cmpnyCd={}, siteCd={}, acctId={}",
            param.gvCmpnyCd(), param.siteCd(), param.acctId());

        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());
        // 삭제는 BTN_DELT 보유 역할(master/system)만 — prafta-048 시드 정의
        if (!AuthRoleUtils.isSystemOperator(param.gvAuthCd())) {
            log.warn("사고 삭제 역할 권한 없음 - userCd={}, authCd={}", param.gvUserCd(), param.gvAuthCd());
            throw new ApiException(AcctErrorCode.ACCT_403_002);
        }

        if (!StringUtils.hasText(param.acctId())) {
            throw new ApiException(AcctErrorCode.ACCT_400_001);
        }

        int deleted = acct01Mapper.deleteAcct(
            param.gvCmpnyCd(), param.siteCd(), param.acctId(), param.gvUserCd());
        if (deleted == 0) {
            throw new ApiException(AcctErrorCode.ACCT_404_001);
        }
        log.info("사고 삭제 완료 - acctId={}", param.acctId());
    }

    @Override
    public VictimSearchResponse searchVictim(VictimSearchParam param) {
        log.info("재해자 검색 진입 - cmpnyCd={}, siteCd={}", param.gvCmpnyCd(), param.siteCd());

        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        return VictimSearchResponse.builder()
            .victimList(acct01Mapper.selectVictimList(param))
            .build();
    }

    // ── 048-04 연계 조회 ─────────────────────────────────────────

    @Override
    public AttendanceLinkResponse selectLinkAttendance(LinkQueryParam param) {
        LinkQueryContext ctx = resolveLinkContext(param);

        boolean isDaily = USER_TYPE_DAILY.equals(ctx.victimUserTypeCd());
        // 일용직은 스케줄 없음(정책 04-user-tracks). 정규직만 스케줄 조회.
        ScheduleLinkResult schedule = isDaily ? null : acct01Mapper.selectAttendanceSchedule(ctx);

        return AttendanceLinkResponse.builder()
            .hasSchedule(schedule != null)
            .scheduleNote(isDaily ? SCHEDULE_NOTE_DAILY : null)
            .schedule(schedule)
            .records(acct01Mapper.selectAttendanceRecords(ctx))
            .occurTime(ctx.occurTime())
            .notice(NOTICE_ATTD)
            .build();
    }

    @Override
    public PatrolLinkResponse selectLinkPatrol(LinkQueryParam param) {
        LinkQueryContext ctx = resolveLinkContext(param);

        return PatrolLinkResponse.builder()
            .summaryList(acct01Mapper.selectPatrolSummary(ctx))
            .badItemList(acct01Mapper.selectPatrolBadItems(ctx))
            .notice(NOTICE_PATROL)
            .build();
    }

    @Override
    public RiskLinkResponse selectLinkRisk(LinkQueryParam param) {
        LinkQueryContext ctx = resolveLinkContext(param);

        return RiskLinkResponse.builder()
            .riskList(acct01Mapper.selectRiskList(ctx))
            .notice(NOTICE_RISK)
            .build();
    }

    @Override
    public TbmLinkResponse selectLinkTbm(LinkQueryParam param) {
        LinkQueryContext ctx = resolveLinkContext(param);

        return TbmLinkResponse.builder()
            .tbmList(acct01Mapper.selectTbmList(ctx))
            .notice(NOTICE_TBM)
            .build();
    }

    // ── T8 안전관리 현황 일괄 출력 ──────────────────────────────

    @Override
    public AttdTbmPrintResponse selectAttdTbmPrint(AttdTbmPrintParam param) {
        log.info("근태+TBM 합본 출력 조회 진입 - cmpnyCd={}, siteCd={}, acctId={}",
            param.gvCmpnyCd(), param.siteCd(), param.acctId());

        // 기존 IDOR 헬퍼 재사용: 사고 헤더(사업장 스코프)로 victim/occurYmd 서버 도출 + 권한 이중 검증.
        LinkQueryContext ctx = resolveLinkContext(toLinkQueryParam(
            param.siteCd(), param.acctId(), param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd()));

        // 헤더(표시용 마스킹 이름/등급명/사업장명)는 신뢰 원천(ctx) 스코프로 재조회.
        AcctResult header = acct01Mapper.selectAcctHeader(ctx.gvCmpnyCd(), ctx.siteCd(), param.acctId());
        if (header == null) {
            throw new ApiException(AcctErrorCode.ACCT_404_001);
        }

        // 일용직은 스케줄 없음(selectLinkAttendance 분기/상수 미러링). 실근태·TBM 은 DAILY 도 정상 조회.
        boolean isDaily = USER_TYPE_DAILY.equals(ctx.victimUserTypeCd());
        ScheduleLinkResult schedule = isDaily ? null : acct01Mapper.selectAttendanceSchedule(ctx);

        return AttdTbmPrintResponse.builder()
            .acctHeader(header)
            .hasSchedule(schedule != null)
            .scheduleNote(isDaily ? SCHEDULE_NOTE_DAILY : null)
            .schedule(schedule)
            .records(acct01Mapper.selectAttendanceRecords(ctx))
            .tbmList(acct01Mapper.selectTbmList(ctx))
            .build();
    }

    @Override
    public RiskAssessmentDetailResult selectRiskAssessmentForPrint(RiskAssessmentPrintParam param) {
        log.info("위험성평가 출력 보강 조회 진입 - cmpnyCd={}, siteCd={}, acctId={}, assessmentCd={}",
            param.gvCmpnyCd(), param.siteCd(), param.acctId(), param.assessmentCd());

        if (!StringUtils.hasText(param.assessmentCd())) {
            throw new ApiException(AcctErrorCode.ACCT_400_001);
        }

        // 1) 사고 헤더(사업장 스코프) 도출 + 권한 이중 검증(IDOR).
        LinkQueryContext ctx = resolveLinkContext(toLinkQueryParam(
            param.siteCd(), param.acctId(), param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd()));

        // 2) 요청 assessmentCd 가 해당 사고의 RISK 연계에 실제 등록된 값인지 정확 매칭 검증(부분일치 우회 차단).
        if (acct01Mapper.selectAcctLinkAssessmentCnt(
                ctx.gvCmpnyCd(), ctx.siteCd(), param.acctId(), param.assessmentCd()) == 0) {
            log.warn("위험성평가 연계 검증 실패 - acctId={}, assessmentCd={}", param.acctId(), param.assessmentCd());
            throw new ApiException(AcctErrorCode.ACCT_404_001);
        }

        // 3) 사고 헤더 사업장 스코프로 평가 상세 라이브 조회.
        RiskAssessmentDetailResult detail = acct01Mapper.selectRiskAssessmentDetailForPrint(
            ctx.gvCmpnyCd(), ctx.siteCd(), param.assessmentCd());
        if (detail == null) {
            throw new ApiException(AcctErrorCode.ACCT_404_001);
        }
        return detail;
    }

    @Override
    public ChkptOptionResponse selectChkptOptions(ChkptOptionParam param) {
        log.info("점검대상 옵션 조회 진입 - cmpnyCd={}, siteCd={}", param.gvCmpnyCd(), param.siteCd());

        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        return ChkptOptionResponse.builder()
            .chkptOptionList(acct01Mapper.selectChkptOptions(param))
            .build();
    }

    @Override
    public RiskCategoryOptionResponse selectRiskCategoryOptions(RiskCategoryOptionParam param) {
        log.info("위험성평가 카테고리 옵션 조회 진입 - cmpnyCd={}, siteCd={}",
            param.gvCmpnyCd(), param.siteCd());

        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        return RiskCategoryOptionResponse.builder()
            .categoryOptionList(acct01Mapper.selectRiskCategoryOptions(param))
            .build();
    }

    // ── 048-05 스냅샷/법정절차 ───────────────────────────────────

    @Override
    @Transactional
    public void confirmLink(LinkConfirmParam param) {
        log.info("연계 스냅샷 확정 진입 - cmpnyCd={}, siteCd={}, acctId={}, domain={}",
            param.gvCmpnyCd(), param.siteCd(), param.acctId(), param.linkDomainCd());

        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());
        assertCanWrite(param.gvAuthCd(), param.gvUserCd());

        if (!StringUtils.hasText(param.acctId()) || !StringUtils.hasText(param.linkDomainCd())) {
            throw new ApiException(AcctErrorCode.ACCT_400_001);
        }

        // 사고 헤더 존재(사업장 스코프) 확인 — 타 사업장 사고 ID 조작 차단
        AcctResult header = acct01Mapper.selectAcctHeader(
            param.gvCmpnyCd(), param.siteCd(), param.acctId());
        if (header == null) {
            throw new ApiException(AcctErrorCode.ACCT_404_001);
        }

        // REPLACE 전략: 해당 사고+도메인 스냅샷 전체 삭제 후 재INSERT
        acct01Mapper.deleteLinkByDomain(
            param.gvCmpnyCd(), param.siteCd(), param.acctId(), param.linkDomainCd());

        List<LinkConfirmRequest.LinkItem> items = param.items();
        if (items != null) {
            int seq = 1;
            for (LinkConfirmRequest.LinkItem item : items) {
                acct01Mapper.insertLink(
                    param.gvCmpnyCd()
                    , param.siteCd()
                    , param.acctId()
                    , param.linkDomainCd()
                    , seq++
                    , item.getLinkKeyJson()
                    , item.getSnapshotJson()
                    , param.gvUserCd());
            }
        }
        log.info("연계 스냅샷 확정 완료 - acctId={}, domain={}, 건수={}",
            param.acctId(), param.linkDomainCd(), items == null ? 0 : items.size());
    }

    @Override
    public LinkSnapshotResponse selectLinkSnapshot(LinkSnapshotParam param) {
        log.info("연계 스냅샷 조회 진입 - cmpnyCd={}, siteCd={}, acctId={}",
            param.gvCmpnyCd(), param.siteCd(), param.acctId());

        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        return LinkSnapshotResponse.builder()
            .snapshotList(acct01Mapper.selectLinkSnapshots(param))
            .build();
    }

    @Override
    public LegalStepListResponse selectLegalStepList(LegalStepListParam param) {
        log.info("법정절차 조회 진입 - cmpnyCd={}, siteCd={}, acctId={}",
            param.gvCmpnyCd(), param.siteCd(), param.acctId());

        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        // 사고 헤더에서 등급/발생일을 가져와 master 를 등급 기준으로 필터(IDOR 스코프 강제)
        AcctResult header = acct01Mapper.selectAcctHeader(
            param.gvCmpnyCd(), param.siteCd(), param.acctId());
        if (header == null) {
            throw new ApiException(AcctErrorCode.ACCT_404_001);
        }

        return LegalStepListResponse.builder()
            .acctGradeCd(header.acctGradeCd())
            .occurYmd(header.occurYmd())
            .legalStepList(acct01Mapper.selectLegalStepList(
                param.gvCmpnyCd(), param.siteCd(), param.acctId(), header.acctGradeCd()))
            .notice(NOTICE_LEGAL)
            .build();
    }

    @Override
    @Transactional
    public void saveLegalStep(LegalStepSaveParam param) {
        log.info("법정절차 저장 진입 - cmpnyCd={}, siteCd={}, acctId={}, stepCd={}",
            param.gvCmpnyCd(), param.siteCd(), param.acctId(), param.stepCd());

        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());
        assertCanWrite(param.gvAuthCd(), param.gvUserCd());

        if (!StringUtils.hasText(param.acctId()) || !StringUtils.hasText(param.stepCd())) {
            throw new ApiException(AcctErrorCode.ACCT_400_001);
        }

        // 사고 헤더 존재(사업장 스코프) 확인 — 타 사업장 사고 ID 조작 차단
        AcctResult header = acct01Mapper.selectAcctHeader(
            param.gvCmpnyCd(), param.siteCd(), param.acctId());
        if (header == null) {
            throw new ApiException(AcctErrorCode.ACCT_404_001);
        }

        acct01Mapper.upsertLegalStep(param);
        log.info("법정절차 저장 완료 - acctId={}, stepCd={}", param.acctId(), param.stepCd());
    }

    @Override
    public LegalStepHistoryResponse selectLegalStepHistory(LegalStepListParam param) {
        log.info("처리이력 조회 진입 - cmpnyCd={}, siteCd={}, acctId={}",
            param.gvCmpnyCd(), param.siteCd(), param.acctId());

        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        return LegalStepHistoryResponse.builder()
            .historyList(acct01Mapper.selectLegalStepHistory(param))
            .build();
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────

    /**
     * 연계 조회 컨텍스트 해석: 사고 헤더(사업장 스코프)를 조회해 매칭키를 도출한다.
     *
     * <p>siteCd 는 body 가 아니라 사고 헤더에서 가져온다. 권한 검증은 헤더의 siteCd 로 수행해
     * 사고 ID 조작으로 타 사업장 근태/순회/위험/TBM/아차 데이터를 읽지 못하게 한다(IDOR).
     */
    /**
     * 출력(T8) 전용 파라미터를 연계 조회 IDOR 헬퍼(resolveLinkContext) 입력으로 변환한다.
     * 선택 필터(chklstType/chkptCd/process/risk/hazard)는 출력 집계에선 불필요하므로 null.
     */
    private LinkQueryParam toLinkQueryParam(
            String siteCd, String acctId, String gvCmpnyCd, String gvUserCd, String gvAuthCd) {
        return new LinkQueryParam(
            siteCd, acctId, null, null, null, null, null, gvCmpnyCd, gvUserCd, gvAuthCd);
    }

    private LinkQueryContext resolveLinkContext(LinkQueryParam param) {
        if (!StringUtils.hasText(param.acctId())) {
            throw new ApiException(AcctErrorCode.ACCT_400_001);
        }

        // 1) 요청 siteCd 로 1차 접근 검증(없으면 차단), 2) 사고 헤더 siteCd 로 재검증.
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd());

        AcctResult header = acct01Mapper.selectAcctHeader(
            param.gvCmpnyCd(), param.siteCd(), param.acctId());
        if (header == null) {
            throw new ApiException(AcctErrorCode.ACCT_404_001);
        }

        // 헤더 siteCd 로 한 번 더 권한 검증(요청 siteCd 와 헤더 siteCd 가 어긋날 가능성 차단)
        assertSiteAccess(param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), header.siteCd());

        return LinkQueryContext.of(param, header);
    }

    /**
     * 쓰기(등록/수정/연계확정/법정절차저장) 역할 권한 검증.
     * BTN_NEW / BTN_SAVE 보유 역할(master/hr/safe/system)만 허용 — prafta-048 시드 정의.
     * 삭제(BTN_DELT=master/system)는 별도로 호출부에서 isSystemOperator 로 강제한다.
     */
    private void assertCanWrite(String authCd, String userCd) {
        if (!AuthRoleUtils.canWriteSafetyContent(authCd)) {
            log.warn("사고관리 쓰기 역할 권한 없음 - userCd={}, authCd={}", userCd, authCd);
            throw new ApiException(AcctErrorCode.ACCT_403_002);
        }
    }

    /**
     * 사업장(siteCd) 접근 권한 검증 (cross-site IDOR 차단). nearmiss/tbm 선례와 동일.
     * 전사 권한(master/hr/safe)은 전체 허용, 그 외는 tb_user_site_auth 매핑 보유 시 허용.
     */
    private void assertSiteAccess(String authCd, String userCd, String cmpnyCd, String siteCd) {
        // 전사 권한(master/hr/safe): 모든 사업장 접근 허용 (prafta-042 전사 스코프 정책)
        if (AuthRoleUtils.canManageAllNodes(authCd)) {
            return;
        }
        // 사업장 미지정이면 사업장 단위 검증 불가 → 차단
        if (!StringUtils.hasText(siteCd)) {
            log.warn("사고관리 사업장 권한 없음(siteCd 미지정) - userCd={}, authCd={}", userCd, authCd);
            throw new ApiException(AcctErrorCode.ACCT_403_001);
        }
        // 그 외: tb_user_site_auth 매핑 보유 시에만 허용
        if (acct01Mapper.countUserSiteAuth(cmpnyCd, userCd, siteCd) == 0) {
            log.warn("사고관리 사업장 권한 없음 - userCd={}, authCd={}, siteCd={}", userCd, authCd, siteCd);
            throw new ApiException(AcctErrorCode.ACCT_403_001);
        }
    }
}
