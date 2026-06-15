package com.prafta.web.attd.attd13.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 관리자 연차 변경 요청 반려 요청 (PRAFTA-COM-008-C 작업2,
 * POST /webApi/attd13/change-requests/{changeReqId}/reject).
 *
 * <p>주로 WORKER 발의(생성 즉시 AGREED)건의 관리자 반려에 사용. 원 연차는 불변(상태만 REJECTED).
 *
 * <p>대문자 필드는 Jackson 프로퍼티 망글링(reject_REASON 등)을 막기 위해 @JsonProperty 로 JSON 키를 필드명 그대로 고정한다.
 */
@Getter
@Setter
public class ChangeRequestRejectRequest {

    /** 반려 사유 (필수). */
    @NotBlank
    @JsonProperty("REJECT_REASON")
    private String REJECT_REASON;
}
