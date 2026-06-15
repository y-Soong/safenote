package com.prafta.common.cmm.dailylogin.application.param;

import com.prafta.common.cmm.dailylogin.dto.request.DailyLoginRequest;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * PRAFTA-app-027-2 — 일용직 로그인 파라미터.
 */
public record DailyLoginParam(
    String userId
    , String userPw
    , String cmpnyCd
    , String clientType
) {
    public static DailyLoginParam from(DailyLoginRequest request, String clientType) {

        if (request == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (clientType == null) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        return new DailyLoginParam(
            request.getUserId()
            , request.getUserPw()
            , normalize(request.getCmpnyCd())
            , clientType
        );
    }

    /** 트림 후 빈값이면 null. cmpnyCd 미입력 시 USER_ID 단독 조회를 허용하기 위함. */
    private static String normalize(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.isEmpty() ? null : t;
    }
}
