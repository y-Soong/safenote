package com.prafta.common.error.chkLst;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

public enum ChkLstErrorCode implements ApiErrorCode {

    // 규칙 예시: {MODULE}_{HTTP}_{SEQ}
    CHKLST_400_001(HttpStatus.BAD_REQUEST, "등록된 점검항목이 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    ChkLstErrorCode(HttpStatus httpStatus, String message) {
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