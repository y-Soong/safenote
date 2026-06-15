package com.prafta.common.cmm.dailylogin.dto.response;

import com.prafta.common.cmm.dailylogin.result.DailyUserResult;
import com.prafta.common.cmm.login.result.UserResult;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * PRAFTA-app-027-2 — 일용직 로그인 응답.
 *
 * <p>PRAFTA-app-027-2'(통합형) — 일용직도 TB_USER 정식 사용자가 되어 정규와 동일한 세션 값을
 * 저장하도록 authCd/authLevel/siteNo/employmentType 를 추가로 노출한다(앱이 세션 저장).
 * NODE_CD 는 NULL(일용직 무소속)이라 nodeCd/nodeNm 필드는 두지 않는다.
 * userTrack='DAILY' 는 하위호환으로 유지한다. PII(휴대폰/이메일)는 응답에 포함하지 않는다(§11.1).
 */
public record DailyLoginResponse(
    String cmpnyCd
    , String userCd
    , String userId
    , String userNm
    , String siteCd
    , String siteNo
    , String siteNm
    , String authCd
    , String authLevel
    , String employmentType
    , String userTrack
    , String refreshToken
    , String token
) {
    /** 일용직 트랙 식별자(정규 사용자와 구분). */
    public static final String USER_TRACK_DAILY = "DAILY";

    /**
     * PRAFTA-app-027-2'(통합형) — 인증=TB_DAILY_USER(DailyUserResult), 인가/표시=TB_USER(UserResult) 기반.
     * siteNo/authCd/authLevel/employmentType 는 TB_USER 행에서 가져온다(정규 토큰과 동일 출처).
     */
    public static DailyLoginResponse from(DailyUserResult userResult, UserResult tbUser,
                                          String refreshToken, String token) {

        if (userResult == null || tbUser == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (refreshToken == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (token == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new DailyLoginResponse(
            userResult.cmpnyCd()
            , userResult.userCd()
            , userResult.userId()
            , userResult.userNm()
            , userResult.siteCd()
            , tbUser.siteNo()
            , userResult.siteNm()
            , tbUser.authCd()
            , tbUser.authLevel()
            , tbUser.employmentType()
            , USER_TRACK_DAILY
            , refreshToken
            , token
        );
    }
}
