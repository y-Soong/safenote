package com.prafta.app.leavechange.leavechange01.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 근로자 연차 이동 발의 요청 (PRAFTA-COM-008-C, C-5a 앱).
 *
 * <p>이동만 가능(취소 불가). 대상 연차는 본인 소유 LEAVE_ID. 발의자는 토큰에서만 도출(IDOR 차단).
 *
 * <p>대문자 필드는 Jackson 프로퍼티 망글링(target_LEAVE_ID 등)을 막기 위해 @JsonProperty 로 JSON 키를 필드명 그대로 고정한다.
 */
@Getter
@Setter
public class LeaveChangeMoveRequest {

    /** 대상 연차 사용 ID (본인 소유, tb_user_leave_use.LEAVE_ID). */
    @NotBlank
    @JsonProperty("TARGET_LEAVE_ID")
    private String TARGET_LEAVE_ID;

    /** 이동 대상일 (YYYYMMDD). */
    @NotBlank
    @JsonProperty("MOVE_TARGET_DATE")
    private String MOVE_TARGET_DATE;

    /** 이동 사유 (필수). */
    @NotBlank
    @JsonProperty("REQ_REASON")
    private String REQ_REASON;
}
