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
    // ===== 디바이스 식별/메타(단일 활성기기 규칙 적재용) — 모두 nullable =====
    , String deviceId
    , String deviceType
    , String deviceModel
    , String osVersion
    , String appVersion
    // prafta-com-015 4-1 — 서버 추출 로그인 IP(부정탐지 baseline 정합). nullable.
    , String ipAddr
) {
    public static DailyLoginParam from(DailyLoginRequest request, String clientType, String ipAddr) {

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
            , normalize(request.getDeviceId(), 100)
            , normalize(request.getDeviceType(), 20)
            , normalize(request.getDeviceModel(), 50)
            , normalize(request.getOsVersion(), 20)
            , normalize(request.getAppVersion(), 20)
            , ipAddr
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

    /** 트림 후 빈값이면 null, maxLen 초과 시 절단(DB 컬럼 길이 가드, 정규 LoginParam 미러). */
    private static String normalize(String v, int maxLen) {
        String t = normalize(v);
        if (t == null) {
            return null;
        }
        return t.length() > maxLen ? t.substring(0, maxLen) : t;
    }
}
