package com.prafta.common.error.attd;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

public enum AttdErrorCode implements ApiErrorCode {

    // 규칙 예시: {MODULE}_{HTTP}_{SEQ}
    ATTD_400_001(HttpStatus.BAD_REQUEST, "이미 사용중인 연차코드입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    AttdErrorCode(HttpStatus httpStatus, String message) {
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