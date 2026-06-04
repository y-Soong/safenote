package com.prafta.common.error.leaverefusal;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 노무수령거부 통지/감지/알림 도메인 에러코드 (PRAFTA-COM-001).
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 */
public enum LeaveRefusalErrorCode implements ApiErrorCode {

    // 403: 통지 발송 권한 없음 (master/hr 만 허용)
    LEAVEREFUSAL_403_001(HttpStatus.FORBIDDEN, "노무수령거부 통지 발송 권한이 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    LeaveRefusalErrorCode(HttpStatus httpStatus, String message) {
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
