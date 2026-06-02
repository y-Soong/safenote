package com.prafta.app.mypage.mypage01.result;

/**
 * 마이페이지 프로필 1행 (prafta-app-010, tb_user JOIN tb_site + SYS004 라벨).
 *
 * <p>PII 컬럼(mblNo/email/birthDate)은 SELECT 자동 복호화 TypeHandler
 * ({@code AesGcmDecryptTypeHandler})로 <b>평문</b>으로 매핑된다.
 * 마스킹 응답(010-01)은 service 에서 평문을 마스킹하여 노출하고, 복호화 응답(010-01b)에서만
 * 평문을 그대로 노출한다. 어느 경우에도 평문을 로그에 남기지 않는다.
 */
public record UserProfileResult(
      String userId
    , String userNm
    , String siteNm
    , String nodeNm
    , String hireDate
    , String mblNo          // 복호화 평문 (MBL_NO_ENC)
    , String mblNoLast4     // MBL_NO_LAST4 (마스킹 조립용)
    , String email          // 복호화 평문 (EMAIL_ENC)
    , String emailDomain    // EMAIL_DOMAIN (마스킹 조립용)
    , String birthDate      // 복호화 평문 YYYYMMDD (BIRTH_DT_ENC)
    , String genderCode     // GENDER (SYS004 100/200, NULL 허용)
    , String genderNm       // SYS004 라벨
    , String lastLoginDtime
) {
}
