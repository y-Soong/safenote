package com.prafta.platform.location.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * SMS 인증번호 검증 요청 DTO (POST /platformApi/location/sms-verify).
 *
 * <p>휴대폰번호는 받지 않는다 — 서버가 토큰의 운영자 본인 등록 휴대폰으로만 매칭(위조 불가).
 */
@Getter
@Setter
@NoArgsConstructor
public class SmsVerifyRequest {

    /** 인증번호(6자리). */
    private String certNo;
}
