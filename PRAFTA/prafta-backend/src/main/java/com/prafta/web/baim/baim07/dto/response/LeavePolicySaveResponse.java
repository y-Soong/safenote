package com.prafta.web.baim.baim07.dto.response;

import com.prafta.common.cmm.leave.vo.ImpactSummaryVO;

import lombok.Builder;
import lombok.Value;

/**
 * 정책 생성/변경 응답.
 *
 * <p>새 POLICY_SEQ와 영향 분석 결과를 함께 반환하여 화면 즉시 갱신 가능하게 한다.
 */
@Value
@Builder
public class LeavePolicySaveResponse {

    Long policySeq;
    ImpactSummaryVO impactSummary;
}
