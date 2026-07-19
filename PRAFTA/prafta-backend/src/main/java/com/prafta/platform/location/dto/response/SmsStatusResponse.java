package com.prafta.platform.location.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * SMS 인증 상태 응답 DTO (GET /platformApi/location/sms-status).
 *
 * <p>화면 재진입 시 불필요한 재인증 방지용 — 실제 게이트는 gps-lists 서버 판정이 강제한다.
 */
@Value
@Builder
public class SmsStatusResponse {

    /** 현재 유효(10분 창 내) 인증 존재 여부. */
    boolean verified;

    /** 인증 상태 잔여 초(미인증이면 0). */
    long remainSec;
}
