package com.prafta.common.error.risk;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

public enum RiskErrorCode implements ApiErrorCode {

    // 규칙 예시: {MODULE}_{HTTP}_{SEQ}
    RISK_400_001(HttpStatus.BAD_REQUEST, "위험요인 구분 하위에 유해요인이 존재합니다.\n확인 후 다시 시도해주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    RiskErrorCode(HttpStatus httpStatus, String message) {
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