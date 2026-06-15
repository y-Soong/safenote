package com.prafta.app.device.device01.application.param;

import com.prafta.app.device.device01.dto.request.PushTokenRequest;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.error.device.DeviceErrorCode;
import com.prafta.common.exception.ApiException;

/**
 * 푸시 토큰 등록 Param.
 *
 * <p>USER_CD 는 JWT 클레임(gv_userCd)에서만 도출(IDOR 차단). deviceUuid 는 본문 도착값(gv_deviceId).
 * <p>입력 검증:
 *   <ul>
 *     <li>tokenInfo null → COMMON_400_003 (진짜 인증 결함만 003 허용).</li>
 *     <li>pushToken 누락/공백/varchar(500) 초과 → DEVICE_400_001 (인터셉터 강제로그아웃 회피).</li>
 *     <li>deviceUuid(gv_deviceId) 미도착 → DEVICE_400_002.</li>
 *   </ul>
 */
public record PushTokenParam(
    String deviceUuid
    , String pushToken
    , String platform
    , String gvUserCd
){
    // PUSH_TOKEN 컬럼 길이(varchar(500)) 상한
    private static final int PUSH_TOKEN_MAX_LEN = 500;

    public static PushTokenParam from(PushTokenRequest request, TokenInfo tokenInfo) {

        if (request == null)
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        if (tokenInfo == null || tokenInfo.gv_userCd() == null || tokenInfo.gv_userCd().isBlank())
            throw new ApiException(CommonErrorCode.COMMON_400_003);

        String pushToken = request.getPushToken();
        if (pushToken == null || pushToken.isBlank() || pushToken.length() > PUSH_TOKEN_MAX_LEN)
            throw new ApiException(DeviceErrorCode.DEVICE_400_001);

        String deviceUuid = request.getDeviceId();
        if (deviceUuid == null || deviceUuid.isBlank())
            throw new ApiException(DeviceErrorCode.DEVICE_400_002);

        return new PushTokenParam(
            deviceUuid
            , pushToken
            , request.getPlatform()
            , tokenInfo.gv_userCd()
        );
    }
}
