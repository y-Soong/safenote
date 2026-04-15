package com.prafta.common.error;

import org.springframework.http.HttpStatus;

public interface ApiErrorCode {
    String code();           // ex) BAIM_404_001
    HttpStatus httpStatus(); // ex) NOT_FOUND
    String message();        // 기본 메시지
}