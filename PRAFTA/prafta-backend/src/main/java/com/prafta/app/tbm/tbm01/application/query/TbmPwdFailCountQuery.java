package com.prafta.app.tbm.tbm01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * prafta-app-004-C(D4): 비밀번호 실패 잠금 카운트 Query.
 * <p>최근 lockWindowSeconds 초 이내 동일 세션/비번유형/시도자의 실패 건수를 센다.
 * <p>임계 도달(예: 5회) 시 lockWindowSeconds(예: 60초) 동안 입실/종료 시도를 거부한다.
 */
public record TbmPwdFailCountQuery(
    String cmpnyCd
    , String sessionCd
    , String pwdTypeCd      // SYS055 ENTRY/EXIT
    , String userCd
    , int lockWindowSeconds
) {
    public static TbmPwdFailCountQuery from(
            String cmpnyCd, String sessionCd, String pwdTypeCd, String userCd, int lockWindowSeconds) {

        if (cmpnyCd == null || sessionCd == null || pwdTypeCd == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new TbmPwdFailCountQuery(cmpnyCd, sessionCd, pwdTypeCd, userCd, lockWindowSeconds);
    }
}
