package com.prafta.app.mypage.mypage01.dto.request;

import lombok.Data;

/**
 * prafta-app-010-02: 프로필 저장 요청.
 *
 * <p>식별자(cmpnyCd/userCd)는 바디로 받지 않는다(JWT 출처, IDOR 차단).
 * 휴대폰 변경 시 {@code mobileVerificationToken}(scope=PHONE_CHANGE_AUTH, 010-03b 발급)이 필수.
 * 검증/정규화는 service 에서 수행한다.
 */
@Data
public class ProfileUpdateRequest {
    private String userNm;
    private String genderCode;             // SYS004 100/200 또는 빈값(=NULL)
    private String birthDate;              // YYYYMMDD 또는 빈값
    private String email;                  // 빈값 허용
    private String mblNo;                  // 변경 시에만 검증 토큰 필요
    private String mobileVerificationToken; // 휴대폰 변경 시 필수
}
