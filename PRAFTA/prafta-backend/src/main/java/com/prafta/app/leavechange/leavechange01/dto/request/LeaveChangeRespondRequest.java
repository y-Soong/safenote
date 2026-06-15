package com.prafta.app.leavechange.leavechange01.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 근로자 연차 변경 요청 응답(동의/거부) 요청 (PRAFTA-COM-008-C, C-2 앱).
 *
 * <p>대상 요청은 path 변수로 받는다. 응답자(근로자)는 토큰에서만 도출(IDOR 차단).
 *
 * <p>대문자 필드는 Jackson 프로퍼티 망글링(worker_RESPONSE 등)을 막기 위해 @JsonProperty 로 JSON 키를 필드명 그대로 고정한다.
 */
@Getter
@Setter
public class LeaveChangeRespondRequest {

    /** 응답 값 AGREE:동의 / REJECT:거부. */
    @NotBlank
    @JsonProperty("WORKER_RESPONSE")
    private String WORKER_RESPONSE;

    /** 응답 사유 (REJECT 시 필수). */
    @JsonProperty("RESPONSE_REASON")
    private String RESPONSE_REASON;
}
