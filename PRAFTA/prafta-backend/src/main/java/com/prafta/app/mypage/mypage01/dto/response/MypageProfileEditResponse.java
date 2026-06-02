package com.prafta.app.mypage.mypage01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-010-01b: 개인정보 수정 진입 전용 응답 (복호화 전체, D1).
 *
 * <p>휴대폰/이메일/생년월일을 복호화 평문으로 프리필한다. 컨트롤러가 응답에
 * {@code Cache-Control: no-store} 헤더를 붙이며, 프론트는 store 영속화 없이 폼 로컬 ref 로만 유지한다.
 * 평문 PII 는 로그에 남기지 않는다.
 */
@Getter
@Builder
public class MypageProfileEditResponse {

    private final String userId;
    private final String userNm;
    private final String siteNm;
    private final String nodeNm;
    private final String hireDate;   // YYYYMMDD
    private final String mblNo;      // 복호화 평문(정규화 숫자 또는 원본)
    private final String email;      // 복호화 평문
    private final String genderCode; // SYS004 100/200 또는 null
    private final String birthDate;  // 복호화 평문 YYYYMMDD 또는 null
    private final String lastLoginDtime;
}
