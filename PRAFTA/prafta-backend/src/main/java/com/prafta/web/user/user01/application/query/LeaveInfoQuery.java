package com.prafta.web.user.user01.application.query;

import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.user.user01.application.param.LeaveInfoParam;

/**
 * 근태/연차 정보 조회 쿼리 (PRAFTA-017-4).
 * 회사 스코프는 토큰 값(gvCmpnyCd)만 사용한다.
 */
public record LeaveInfoQuery(
    String userCd
    , String gvCmpnyCd
) {
    public static LeaveInfoQuery from(LeaveInfoParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new LeaveInfoQuery(
            param.userCd()
            , param.gvCmpnyCd()
        );
    }
}
