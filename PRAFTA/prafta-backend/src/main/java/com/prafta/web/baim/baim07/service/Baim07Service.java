package com.prafta.web.baim.baim07.service;

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

/**
 * baim07 — 회사 법정 연차 부여 정책 관리 (PRAFTA-018).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.5
 *
 * <p>본 모듈은 단순 어댑터(Controller↔Service)로, 비즈니스 로직은
 * {@code com.prafta.common.cmm.leave.service.LeavePolicyService}에 위임한다.
 */
public interface Baim07Service {

    /** 활성 정책 단건 조회 (정책서 §8.5.7 — 인증 사용자) */
    LeavePolicyResponse getActivePolicy(ActivePolicyParam param);

    /** 정책 생성 (정책서 §8.5.7 — AUTH_MASTER OR AUTH_HR_MANAGER) */
    LeavePolicySaveResponse createPolicy(LeavePolicySaveParam param);

    /** 정책 변경 */
    LeavePolicySaveResponse updatePolicy(Long policySeq, LeavePolicySaveParam param);

    /** 정책 변경 이력 페이징 조회 */
    LeavePolicyHistoryListResponse getHistory(LeavePolicyHistoryListParam param);

    /** 영향 분석 미리보기 (저장 없음) */
    ImpactPreviewResponse previewImpact(LeavePolicySaveParam param);

    /** 정책 변경 영향 분석 (화면 8, 읽기 전용 시뮬레이션 — 저장 없음) */
    AnalyzeImpactResponse analyzeImpact(LeavePolicySaveParam param);

    /** 시간차 1일 환산시간 조회 — 현재 적용값 + 변경 이력 (LC-02, AUTH_MASTER OR AUTH_HR_MANAGER) */
    LeaveConversionResponse getConversion(LeaveConversionParam param);

    /** 시간차 1일 환산시간 저장 — 신규 적용일 INSERT / 같은 적용일 UPDATE (LC-02) */
    LeaveConversionSaveResponse saveConversion(LeaveConversionSaveParam param);
}
