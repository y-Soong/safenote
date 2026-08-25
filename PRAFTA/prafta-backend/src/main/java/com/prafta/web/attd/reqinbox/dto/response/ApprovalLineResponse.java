package com.prafta.web.attd.reqinbox.dto.response;

import java.util.List;

import com.prafta.common.cmm.approval.vo.ApprovalStepVO;

import lombok.Builder;
import lombok.Value;

/**
 * 결재 진행 타임라인 응답 (근태결재선통합 P3-1, 웹 {@code Attd_10.vue} 상세 패널 소비).
 *
 * <p>{@code canProcess}/{@code currentApproverUserNm} 은 읽기 전용으로 산출한다
 * ({@code ApprovalStepGateService.resolveProcessableStep} 미사용 — GET 부작용 금지, plan §1 결정 D).
 */
@Value
@Builder
public class ApprovalLineResponse {
    List<ApprovalStepVO> steps;
    boolean canProcess;
    String currentApproverUserNm;
}
