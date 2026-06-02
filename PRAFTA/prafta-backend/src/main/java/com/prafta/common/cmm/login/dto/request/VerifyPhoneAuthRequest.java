package com.prafta.common.cmm.login.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PRAFTA-036 — 휴대폰 인증대기 계정 활성화 요청 DTO.
 *
 * <p>사용자가 입력한 휴대폰번호와 SMS 인증번호로 본인 확인 후, 본 endpoint 가
 * ACCOUNT_STATUS 를 '01' 로 전이하고 정식 토큰을 발급한다. cmpnyCd/userCd 는
 * Authorization 헤더의 임시 scope 토큰에서만 추출하므로 본 DTO 에 포함하지 않는다.
 */
@Getter
@Setter
@NoArgsConstructor
public class VerifyPhoneAuthRequest {
    private String mblNo;
    private String certNo;
}
