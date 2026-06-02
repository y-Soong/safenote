package com.prafta.app.mypage.mypage01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * prafta-app-010-01: 마이페이지 메인 프로필 응답 (마스킹, D1).
 *
 * <p>PII 는 평문을 절대 싣지 않는다. 휴대폰은 LAST4 기반 마스킹(010-****-8295),
 * 이메일/생년월일도 마스킹 문자열만 노출한다. 성별은 SYS004 코드값(100/200)과 라벨,
 * NULL 이면 둘 다 null.
 */
@Getter
@Builder
public class MypageProfileResponse {

    private final String userId;
    private final String userNm;
    private final String siteNm;
    private final String nodeNm;
    private final String hireDate;        // YYYYMMDD 원본(FE 에서 포맷)
    private final String mblNoMasked;     // 010-****-8295
    private final String emailMasked;     // t***@test.com
    private final String genderCode;      // SYS004 100/200 또는 null
    private final String genderNm;        // 라벨 또는 null
    private final String birthDateMasked; // 1993-**-**
    private final String lastLoginDtime;
    private final int presetCount;
}
