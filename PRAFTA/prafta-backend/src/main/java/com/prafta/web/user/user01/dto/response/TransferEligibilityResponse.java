package com.prafta.web.user.user01.dto.response;

import java.util.List;

import com.prafta.web.user.user01.result.TransferBlockReason;

import lombok.Builder;
import lombok.Value;

/**
 * 소속이동 가능 여부 사전 판정 응답 — PRAFTA-WEB_001-1.
 *
 * <p>eligible=false 이면 blockReasons 에 불가 사유(다중 가능)가 담긴다.
 * 일용직은 5종 불가케이스를 적용하지 않으므로(eligible=true) 화면에서 분기 표시한다.
 */
@Value
@Builder
public class TransferEligibilityResponse {
    String userCd;
    String employmentType;
    boolean eligible;
    List<TransferBlockReason> blockReasons;
}
