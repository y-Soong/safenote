package com.prafta.common.error.device;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 단말(디바이스) 도메인 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 *
 * <p>★주의: 앱 인터셉터는 COMMON_400_003 / COMMON_400_600 을 토큰 오류로 간주해 강제 로그아웃시킨다
 *    (메모리 project_prafta_app_req07_token_logout_bug). 따라서 body 검증 실패(pushToken 누락/형식,
 *    deviceId 미도착)에는 절대 COMMON_400_003/600 을 쓰지 않고 본 DEVICE_400_* 을 사용한다.
 */
public enum DeviceErrorCode implements ApiErrorCode {

    // 400: 푸시 토큰 누락 또는 형식 오류(빈값/varchar(500) 초과)
    DEVICE_400_001(HttpStatus.BAD_REQUEST, "푸시 토큰이 유효하지 않습니다.")
    // 400: 디바이스 식별자(gv_deviceId) 미도착
    , DEVICE_400_002(HttpStatus.BAD_REQUEST, "디바이스 정보가 확인되지 않습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    DeviceErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String message() {
        return message;
    }
}
