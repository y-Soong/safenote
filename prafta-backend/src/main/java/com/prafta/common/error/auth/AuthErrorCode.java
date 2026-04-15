package com.prafta.common.error.auth;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

public enum AuthErrorCode implements ApiErrorCode {

    // 규칙 예시: {MODULE}_{HTTP}_{SEQ}
	AUTH_500_001(HttpStatus.UNAUTHORIZED, "NO_REFRESH_TOKEN !\n관리자에게 문의해주세요.")
	, AUTH_500_002(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN !\n관리자에게 문의해주세요.")
	, AUTH_500_003(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND !\n관리자에게 문의해주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    AuthErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String code() {
        return name(); // enum 이름을 그대로 코드로 쓰면 관리 쉬움
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