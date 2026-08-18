package com.prafta.app.leavechange.leavechange01.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 근로자 연차 취소(삭제) 발의 요청 (2026-08-18 개방 — 008-C §3-2 개정).
 *
 * <p>대상 연차는 본인 소유 LEAVE_ID. 발의자는 토큰에서만 도출(IDOR 차단). DELETE 전용 — 이동 대상일 없음.
 *
 * <p>대문자 필드는 Jackson 프로퍼티 망글링(target_LEAVE_ID 등)을 막기 위해 @JsonProperty 로 JSON 키를 필드명 그대로 고정한다.
 */
@Getter
@Setter
public class LeaveChangeDeleteRequest {

    /** 대상 연차 사용 ID (본인 소유, tb_user_leave_use.LEAVE_ID). */
    @NotBlank
    @JsonProperty("TARGET_LEAVE_ID")
    private String TARGET_LEAVE_ID;

    /** 취소 사유 (필수, DB varchar(500) — SEC-M1 서버측 상한 검증). */
    @NotBlank
    @Size(max = 500)
    @JsonProperty("REQ_REASON")
    private String REQ_REASON;
}
