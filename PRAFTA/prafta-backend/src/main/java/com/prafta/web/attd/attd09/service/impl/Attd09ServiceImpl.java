package com.prafta.web.attd.attd09.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;

import com.prafta.common.cmm.leave.service.LeaveDashboardService;
import com.prafta.common.cmm.leave.service.LeaveGrantEngineService;
import com.prafta.common.cmm.leave.service.LeavePolicyService;
import com.prafta.common.cmm.leave.service.LeaveRemnantCoverService;
import com.prafta.common.cmm.leave.vo.CoverGrantResultVO;
import com.prafta.common.cmm.leave.vo.HireDateGrantResultVO;
import com.prafta.common.cmm.leave.vo.LeaveDashboardResultVO;
import com.prafta.common.cmm.leave.vo.LeaveDetailResultVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;
import com.prafta.common.cmm.leave.vo.LeaveRecallResultVO;
import com.prafta.common.cmm.leave.vo.ManualGrantResultVO;
import com.prafta.common.cmm.leave.vo.PolicyGrantPreviewRowVO;
import com.prafta.common.cmm.leave.vo.PolicyGrantPreviewVO;
import com.prafta.common.cmm.leave.vo.RemnantCoverSummaryVO;
import com.prafta.common.cmm.leave.vo.RemnantReportVO;
import com.prafta.common.cmm.leave.vo.ShortfallListResultVO;
import com.prafta.common.cmm.leave.vo.ShortfallRowVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.web.attd.attd09.application.param.CoverGrantParam;
import com.prafta.web.attd.attd09.application.param.HireDateGrantParam;
import com.prafta.web.attd.attd09.application.param.LeaveDashboardListParam;
import com.prafta.web.attd.attd09.application.param.LeaveDetailParam;
import com.prafta.web.attd.attd09.application.param.LeaveRecallParam;
import com.prafta.web.attd.attd09.application.param.ManualGrantParam;
import com.prafta.web.attd.attd09.application.param.ManualTypesParam;
import com.prafta.web.attd.attd09.application.param.PolicyGrantParam;
import com.prafta.web.attd.attd09.application.param.PolicyInfoParam;
import com.prafta.web.attd.attd09.application.param.RemnantReportParam;
import com.prafta.web.attd.attd09.application.param.RemnantSummaryParam;
import com.prafta.web.attd.attd09.application.param.ShortfallListParam;
import com.prafta.web.attd.attd09.application.param.UsageHistoryParam;
import com.prafta.web.attd.attd09.dto.response.CoverGrantResponse;
import com.prafta.web.attd.attd09.dto.response.HireDateGrantResponse;
import com.prafta.web.attd.attd09.dto.response.LeaveDashboardResponse;
import com.prafta.web.attd.attd09.dto.response.LeaveDetailResponse;
import com.prafta.web.attd.attd09.dto.response.LeaveRecallResponse;
import com.prafta.web.attd.attd09.dto.response.ManualGrantResponse;
import com.prafta.web.attd.attd09.dto.response.ManualTypesResponse;
import com.prafta.web.attd.attd09.dto.response.PolicyGrantPolicyInfoResponse;
import com.prafta.web.attd.attd09.dto.response.PolicyGrantPreviewResponse;
import com.prafta.web.attd.attd09.dto.response.PolicyGrantResponse;
import com.prafta.web.attd.attd09.dto.response.RemnantCoverSummaryResponse;
import com.prafta.web.attd.attd09.dto.response.RemnantReportResponse;
import com.prafta.web.attd.attd09.dto.response.ShortfallListResponse;
import com.prafta.web.attd.attd09.dto.response.UsageHistoryResponse;
import com.prafta.web.attd.attd09.service.Attd09Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link Attd09Service} 구현체.
 *
 * <p>attd09 모듈은 연차 현황 화면(Attd_09.vue + 모달 2종)의 백엔드 어댑터다.
 * 비즈니스 로직은 모두 {@link LeaveDashboardService}로 위임한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Attd09ServiceImpl implements Attd09Service {

    /** AXIS3=PRORATE 첫해 방식 식별자(SYS037) — 안내 노출 판정용. */
    private static final String AXIS3_PRORATE = "PRORATE";

    /** PRORATE 폴백 안내 문구(화면 캡션/배너 공용). prafta-022 보완. */
    private static final String PRORATE_FALLBACK_NOTICE =
            "현재 연차정책의 첫해 방식이 비례부여(PRORATE)입니다. "
            + "정밀 비례부여는 추후 적용 예정으로, 현재는 차년도 일괄(NEXT_YEAR_BULK) 기준으로 부여됩니다.";

    private final LeaveDashboardService leaveDashboardService;
    private final LeaveGrantEngineService leaveGrantEngineService;
    private final LeavePolicyService leavePolicyService;
    /** PC-07(D9-②③): 짜투리 회사 부담 집계/소멸 임박 리포트 — 비즈니스 로직 위임(어댑터 관례). */
    private final LeaveRemnantCoverService leaveRemnantCoverService;

    @Override
    public LeaveDashboardResponse getDashboard(LeaveDashboardListParam param) {
        LeaveDashboardResultVO result = leaveDashboardService.getDashboard(
                param.gvCmpnyCd(),
                param.gvAuthCd(),
                param.siteCd(),
                param.nodeCd(),
                param.incSubNodeYn(),
                param.userNm(),
                param.page(),
                param.size());
        return LeaveDashboardResponse.builder()
                .metrics(result.getMetrics())
                .list(result.getList())
                .paging(result.getPaging())
                .convMinutes(result.getConvMinutes()) // LC-07(표기): "N일 H시간 M분" 조립 분모
                .build();
    }

    @Override
    public LeaveDetailResponse getDetail(LeaveDetailParam param) {
        LeaveDetailResultVO result = leaveDashboardService.getDetail(
                param.gvCmpnyCd(), param.gvAuthCd(), param.userCd());
        return LeaveDetailResponse.builder()
                .user(result.getUser())
                .legalSummary(result.getLegalSummary())
                .nonLegalSummary(result.getNonLegalSummary())
                .appliedLeaveTypes(result.getAppliedLeaveTypes())
                .grantHistory(result.getGrantHistory())
                .convMinutes(result.getConvMinutes())           // LC-07(표기)
                .hourlyUsedMinutes(result.getHourlyUsedMinutes()) // LC-07(표기)
                .build();
    }

    @Override
    public UsageHistoryResponse getUsageHistory(UsageHistoryParam param) {
        return UsageHistoryResponse.builder()
                .year(param.year())
                .usageHistory(leaveDashboardService.getUsageHistory(
                        param.gvCmpnyCd(), param.gvAuthCd(), param.userCd(), param.year()))
                .build();
    }

    @Override
    public com.prafta.common.cmm.file.application.model.FileBytesResult getUsageEvidenceFile(
            LeaveDetailParam param, String fileMgmtCd) {
        return leaveDashboardService.getUsageEvidenceFile(
                param.gvCmpnyCd(), param.gvAuthCd(), param.userCd(), fileMgmtCd);
    }

    @Override
    public ManualTypesResponse getManualTypes(ManualTypesParam param) {
        return ManualTypesResponse.builder()
                .types(leaveDashboardService.getManualGrantTypes(param.gvCmpnyCd(), param.gvAuthCd()))
                .build();
    }

    @Override
    public ManualGrantResponse manualGrant(ManualGrantParam param) {
        ManualGrantResultVO result = leaveDashboardService.manualGrant(
                param.gvCmpnyCd(),
                param.toCommand(),
                param.gvAuthCd(),
                param.gvUserCd());
        return ManualGrantResponse.builder()
                .grantedCount(result.getGrantedCount())
                .grantedUserCds(result.getGrantedUserCds())
                .build();
    }

    @Override
    public LeaveRecallResponse recallGrant(LeaveRecallParam param) {
        LeaveRecallResultVO result = leaveDashboardService.recallGrant(
                param.gvCmpnyCd(),
                param.grantId(),
                param.reason(),
                param.gvAuthCd(),
                param.gvUserCd());
        return LeaveRecallResponse.builder()
                .grantId(result.getGrantId())
                .status(result.getStatus())
                .build();
    }

    @Override
    public HireDateGrantResponse hireDateGrant(HireDateGrantParam param) {
        HireDateGrantResultVO result = leaveDashboardService.hireDateGrant(
                param.gvCmpnyCd(),
                param.userCds(),
                param.gvAuthCd(),
                param.gvUserCd());
        return HireDateGrantResponse.builder()
                .grantedCount(result.getGrantedCount())
                .grantedUserCds(result.getGrantedUserCds())
                .skippedCount(result.getSkippedCount())
                .skippedUserCds(result.getSkippedUserCds())
                .grantedDays(result.getGrantedDays())
                .build();
    }

    @Override
    public PolicyGrantPreviewResponse previewPolicyGrant(PolicyGrantParam param) {
        // 프리뷰는 read-only dry-run → 부여 엔진을 직접 호출(대시보드 서비스 미경유, 쓰기 없음).
        PolicyGrantPreviewVO result = leaveGrantEngineService.previewPolicyGrant(
                param.gvCmpnyCd(),
                param.userCds(),
                param.gvAuthCd());

        List<PolicyGrantPreviewResponse.Row> rows = result.getRows().stream()
                .map(Attd09ServiceImpl::toPreviewRow)
                .toList();

        return PolicyGrantPreviewResponse.builder()
                .selectedCount(result.getSelectedCount())
                .newGrantCount(result.getNewGrantCount())
                .noChangeCount(result.getNoChangeCount())
                .rows(rows)
                .build();
    }

    @Override
    public PolicyGrantResponse policyGrant(PolicyGrantParam param) {
        // 적용은 기존 배선(대시보드 서비스 → 엔진 위임)을 그대로 따른다. hire-date-grant와 동일 엔진 메서드.
        HireDateGrantResultVO result = leaveDashboardService.hireDateGrant(
                param.gvCmpnyCd(),
                param.userCds(),
                param.gvAuthCd(),
                param.gvUserCd());
        return PolicyGrantResponse.builder()
                .grantedCount(result.getGrantedCount())
                .grantedUserCds(result.getGrantedUserCds())
                .skippedCount(result.getSkippedCount())
                .skippedUserCds(result.getSkippedUserCds())
                .grantedDays(result.getGrantedDays())
                .build();
    }

    @Override
    public PolicyGrantPolicyInfoResponse getPolicyInfo(PolicyInfoParam param) {
        // 관리자(MASTER/HR) 권한 가드 — 정책 기준 부여 화면의 보조 조회이므로 부여와 동일 기준 적용(정책서 §8.5.7).
        ensureManager(param.gvCmpnyCd(), param.gvAuthCd(), "정책 기준 부여 안내 정보 조회");

        // 활성 정책 조회(읽기 전용, 락 없음). 없으면 null.
        LeavePolicyVO policy = leavePolicyService.findActivePolicy(param.gvCmpnyCd());

        String grantBase = (policy == null) ? null : policy.getAxis1GrantBase();
        String firstYearMethod = (policy == null) ? null : policy.getAxis3FirstYearMethod();
        // prafta-023 D: PRORATE 비례부여가 구현되어 더 이상 차년도 일괄 폴백이 아니다 → 안내 비표시.
        // (AXIS4=HALF_DAY 0.5일 절사만 후속 — 임시 CEIL 적용, 상세는 가이드 문서 참조)
        boolean prorateFallback = false;
        String noticeText = "";

        log.info("정책 기준 부여 안내 정보 조회. cmpnyCd={}, grantBase={}, firstYearMethod={}, prorateFallback={}",
                param.gvCmpnyCd(), grantBase, firstYearMethod, prorateFallback);

        return PolicyGrantPolicyInfoResponse.builder()
                .grantBase(grantBase)
                .firstYearMethod(firstYearMethod)
                .prorateFallback(prorateFallback)
                .noticeText(noticeText)
                .build();
    }

    @Override
    public RemnantCoverSummaryResponse getRemnantCoverSummary(RemnantSummaryParam param) {
        // 관리자 게이트는 서비스 진입부(LeaveRemnantCoverService)에서 강제(정책서 §8.5.7).
        RemnantCoverSummaryVO result = leaveRemnantCoverService.getCoverSummary(
                param.gvCmpnyCd(), param.gvAuthCd(), param.year());
        return RemnantCoverSummaryResponse.builder()
                .remnantPolicyOn(result.remnantPolicyOn())
                .year(result.year())
                .totalCoverDays(result.totalCoverDays())
                .coverCount(result.coverCount())
                .items(result.items())
                .build();
    }

    @Override
    public RemnantReportResponse getRemnantReport(RemnantReportParam param) {
        // 관리자 게이트는 서비스 진입부(LeaveRemnantCoverService)에서 강제(정책서 §8.5.7).
        RemnantReportVO result = leaveRemnantCoverService.getRemnantReport(
                param.gvCmpnyCd(), param.gvAuthCd());
        return RemnantReportResponse.builder()
                .remnantPolicyOn(result.remnantPolicyOn())
                .rows(result.rows())
                .build();
    }

    /**
     * 관리자(MASTER/HR) 권한 가드 (정책서 §8.5.7). 위반 시 {@link AttdErrorCode#ATTD_403_020}.
     * LeaveDashboardServiceImpl.ensureManager와 동일 기준(AuthRoleUtils.isManager).
     */
    private void ensureManager(String cmpnyCd, String authCd, String action) {
        if (!AuthRoleUtils.isManager(authCd)) {
            log.warn("{} 권한 없음. cmpnyCd={}, authCd={}", action, cmpnyCd, authCd);
            throw new ApiException(AttdErrorCode.ATTD_403_020);
        }
    }

    /** 엔진 프리뷰 행 VO → attd09 응답 행 DTO 변환. */
    private static PolicyGrantPreviewResponse.Row toPreviewRow(PolicyGrantPreviewRowVO vo) {
        return PolicyGrantPreviewResponse.Row.builder()
                .userCd(vo.getUserCd())
                .addDays(vo.getAddDays())
                .note(vo.getNote())
                .build();
    }

    @Override
    public ShortfallListResponse getShortfallList(ShortfallListParam param) {
        ShortfallListResultVO result = leaveDashboardService.getShortfallList(
                param.gvCmpnyCd(),
                param.gvAuthCd(),
                param.gvUserCd(),
                param.gvSiteCd(),
                param.siteCd(),
                param.nodeCd(),
                param.incSubNodeYn(),
                param.userNm(),
                param.baseYmd(),
                param.page(),
                param.size());

        List<ShortfallListResponse.Row> rows = result.getRows().stream()
                .map(Attd09ServiceImpl::toShortfallRow)
                .toList();

        return ShortfallListResponse.builder()
                .fiscalYearYn(result.getFiscalYearYn())
                .baseYmd(result.getBaseYmd())
                .rows(rows)
                .totalCount(result.getTotalCount())
                .build();
    }

    private static ShortfallListResponse.Row toShortfallRow(ShortfallRowVO vo) {
        return ShortfallListResponse.Row.builder()
                .userCd(vo.getUserCd())
                .userNm(vo.getUserNm())
                .hireDate(vo.getHireDate())
                .hireBasisAccrual(vo.getHireBasisAccrual())
                .actualAccrual(vo.getActualAccrual())
                .diff(vo.getDiff())
                .coveredTotal(vo.getCoveredTotal())
                .remainingShortfall(vo.getRemainingShortfall())
                .build();
    }

    @Override
    public CoverGrantResponse coverGrant(CoverGrantParam param) {
        CoverGrantResultVO result = leaveDashboardService.coverGrant(
                param.gvCmpnyCd(),
                param.command(),
                param.gvAuthCd(),
                param.gvUserCd());
        return CoverGrantResponse.builder()
                .grantId(result.getGrantId())
                .grantedDays(result.getGrantedDays())
                .remainingShortfallAfter(result.getRemainingShortfallAfter())
                .build();
    }
}
