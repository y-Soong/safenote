package com.prafta.app.device.device01.application.command;

import com.prafta.app.device.device01.application.param.PushTokenParam;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 푸시 토큰 upsert Command.
 *
 * <p>WHERE 절은 DEVICE_UUID(PK) + USER_CD(JWT) 로 강제하여 본인 단말만 갱신한다(IDOR 차단).
 *    UPDATE_NO 는 갱신 수행자(USER_CD)로 기록한다.
 */
public record UpsertPushTokenCommand(
    String deviceUuid
    , String pushToken
    , String cmpnyCd
    , String userCd
){
    public static UpsertPushTokenCommand from(PushTokenParam param) {

        if (param == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);

        return new UpsertPushTokenCommand(
            param.deviceUuid()
            , param.pushToken()
            , param.gvCmpnyCd()
            , param.gvUserCd()
        );
    }
}
