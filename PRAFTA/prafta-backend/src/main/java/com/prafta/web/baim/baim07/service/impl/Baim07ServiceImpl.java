package com.prafta.web.baim.baim07.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.service.LeavePolicyService;
import com.prafta.common.cmm.leave.vo.AnalyzeImpactVO;
import com.prafta.common.cmm.leave.vo.ImpactSummaryVO;
import com.prafta.common.cmm.leave.vo.LeaveConversionPolicyVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyHistoryVO;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;
import com.prafta.common.cmm.leave.vo.PagedResult;
import com.prafta.web.baim.baim07.application.param.ActivePolicyParam;
import com.prafta.web.baim.baim07.application.param.LeaveConversionParam;
import com.prafta.web.baim.baim07.application.param.LeaveConversionSaveParam;
import com.prafta.web.baim.baim07.application.param.LeavePolicyHistoryListParam;
import com.prafta.web.baim.baim07.application.param.LeavePolicySaveParam;
import com.prafta.web.baim.baim07.dto.response.AnalyzeImpactResponse;
import com.prafta.web.baim.baim07.dto.response.ImpactPreviewResponse;
import com.prafta.web.baim.baim07.dto.response.LeaveConversionResponse;
import com.prafta.web.baim.baim07.dto.response.LeaveConversionSaveResponse;
import com.prafta.web.baim.baim07.dto.response.LeavePolicyHistoryListResponse;
import com.prafta.web.baim.baim07.dto.response.LeavePolicyResponse;
import com.prafta.web.baim.baim07.dto.response.LeavePolicySaveResponse;
import com.prafta.web.baim.baim07.service.Baim07Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link Baim07Service} 구현체.
 *
 * <p>baim07 모듈은 정책 관리 화면(Baim_07.vue, 단계 3)의 백엔드 어댑터다.
 * 비즈니스 로직은 모두 {@link LeavePolicyService}로 위임하여, 채널(웹/배치/프로시저)에
 * 따라 코드가 분기되지 않도록 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Baim07ServiceImpl implements Baim07Service {

    /** LC-02: 저장 안내 문구(적용일 이후 신청분부터 반영 — F4 소급 재계산 없음). */
    private static final String CONVERSION_SAVE_MESSAGE =
            "적용일 이후 신청분부터 반영됩니다. 과거 신청분은 재계산되지 않습니다.";

    private final LeavePolicyService leavePolicyService;
    /** LC-02: 시간차 1일 환산시간(분모) 정책 — 조회/저장 위임. */
    private final LeaveConversionPolicyService leaveConversionPolicyService;

    @Override
    public LeavePolicyResponse getActivePolicy(ActivePolicyParam param) {
        LeavePolicyVO policy = leavePolicyService.findActivePolicy(param.gvCmpnyCd());
        return LeavePolicyResponse.builder()
                .policy(policy)
                .build();
    }

    @Override
    public LeavePolicySaveResponse createPolicy(LeavePolicySaveParam param) {
        Long newPolicySeq = leavePolicyService.createPolicy(
                param.gvCmpnyCd(), param.toCommand(), param.gvAuthCd(), param.gvUserCd());
        // 생성 직후 영향 분석을 화면 갱신용으로 함께 반환 (DB는 이미 HISTORY에 저장됨)
        ImpactSummaryVO impact = leavePolicyService.previewImpact(
                param.gvCmpnyCd(), param.toCommand(), param.gvAuthCd());
        return LeavePolicySaveResponse.builder()
                .policySeq(newPolicySeq)
                .impactSummary(impact)
                .build();
    }

    @Override
    public LeavePolicySaveResponse updatePolicy(Long policySeq, LeavePolicySaveParam param) {
        Long newPolicySeq = leavePolicyService.updatePolicy(
                param.gvCmpnyCd(), policySeq, param.toCommand(), param.gvAuthCd(), param.gvUserCd());
        ImpactSummaryVO impact = leavePolicyService.previewImpact(
                param.gvCmpnyCd(), param.toCommand(), param.gvAuthCd());
        return LeavePolicySaveResponse.builder()
                .policySeq(newPolicySeq)
                .impactSummary(impact)
                .build();
    }

    @Override
    public LeavePolicyHistoryListResponse getHistory(LeavePolicyHistoryListParam param) {
        PagedResult<LeavePolicyHistoryVO> history = leavePolicyService.findHistory(
                param.gvCmpnyCd(), param.gvAuthCd(), param.page(), param.size());
        return LeavePolicyHistoryListResponse.builder()
                .history(history)
                .build();
    }

    @Override
    public ImpactPreviewResponse previewImpact(LeavePolicySaveParam param) {
        ImpactSummaryVO impact = leavePolicyService.previewImpact(
                param.gvCmpnyCd(), param.toCommand(), param.gvAuthCd());
        return ImpactPreviewResponse.builder()
                .impactSummary(impact)
                .build();
    }

    @Override
    public AnalyzeImpactResponse analyzeImpact(LeavePolicySaveParam param) {
        AnalyzeImpactVO impact = leavePolicyService.analyzeImpact(
                param.gvCmpnyCd(), param.toCommand(), param.gvAuthCd());
        return AnalyzeImpactResponse.builder()
                .impact(impact)
                .build();
    }

    @Override
    public LeaveConversionResponse getConversion(LeaveConversionParam param) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 현재 적용값(오늘 기준 유효 행 — 미설정이면 기본 480) + 이력(적용일 내림차순, 미래 예약분 포함)
        int current = leaveConversionPolicyService.selectConversionMinutes(param.gvCmpnyCd(), today);
        List<LeaveConversionPolicyVO> history =
                leaveConversionPolicyService.findHistory(param.gvCmpnyCd(), param.gvAuthCd());
        // 현재 적용값의 적용일 = 이력 중 오늘 이하 최신 행(내림차순 첫 매치). 없으면 null(기본 480 적용 중).
        String currentApplyFromDate = history.stream()
                .filter(h -> h.applyFromDate() != null && h.applyFromDate().compareTo(today) <= 0)
                .map(LeaveConversionPolicyVO::applyFromDate)
                .findFirst()
                .orElse(null);
        return LeaveConversionResponse.builder()
                .currentConvMinutes(current)
                .currentApplyFromDate(currentApplyFromDate)
                .history(history)
                .build();
    }

    @Override
    public LeaveConversionSaveResponse saveConversion(LeaveConversionSaveParam param) {
        leaveConversionPolicyService.savePolicy(
                param.gvCmpnyCd(), param.applyFromDate(), param.dailyConvMinutes(),
                param.gvAuthCd(), param.gvUserCd());
        return LeaveConversionSaveResponse.builder()
                .applyFromDate(param.applyFromDate())
                .dailyConvMinutes(param.dailyConvMinutes())
                .message(CONVERSION_SAVE_MESSAGE)
                .build();
    }
}
