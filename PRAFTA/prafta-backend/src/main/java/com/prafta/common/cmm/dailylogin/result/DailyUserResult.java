package com.prafta.common.cmm.dailylogin.result;

/**
 * PRAFTA-app-027-2 — 일용직 로그인 조회 결과(TB_DAILY_USER + TB_SITE).
 *
 * <p>NODE_CD/AUTH_CD/PWD_CHG_DTIME 컬럼은 TB_DAILY_USER 에 존재하지 않으므로 포함하지 않는다.
 * 휴대폰 등 PII 는 로그인 흐름에 불필요하여 조회하지 않는다(§11.1).
 */
public record DailyUserResult(
    String cmpnyCd
    , String userCd
    , String userId
    , String userNm
    , String userPw
    , String siteCd
    , String siteNm
    , String useYn
    , String accountStatus
    , String workExpireDate
    , String withdrawalDate
    , String pwdLockYn
    , String pwdFailCnt
    , String pwdLockExpireDtime
) {

}
