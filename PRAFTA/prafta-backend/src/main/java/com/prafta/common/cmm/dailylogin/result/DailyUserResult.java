package com.prafta.common.cmm.dailylogin.result;

/**
 * PRAFTA-app-027-2 — 일용직 로그인 조회 결과(TB_DAILY_USER + TB_SITE).
 *
 * <p>NODE_CD/AUTH_CD/PWD_CHG_DTIME 컬럼은 TB_DAILY_USER 에 존재하지 않으므로 포함하지 않는다.
 *
 * <p>prafta-app-032 B: 비활성 일용직 로그인 자동 재활성 시, 기존 행의 휴대폰 파생값(암호문/HMAC/마지막4자리)을
 * 그대로 write-back 해 정보 손실 없이 재활성하기 위해 휴대폰 파생 컬럼을 포함한다.
 * 평문이 아닌 암호문/HMAC 만 담으며(§11.1) 로그에는 남기지 않는다.
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
    // prafta-app-032 B — 재활성 write-back 용 휴대폰 파생값(암호문/HMAC). 로그인 표시엔 미사용.
    , String mblNoEnc
    , String mblNoHmac
    , String mblNoLast4
) {

}
