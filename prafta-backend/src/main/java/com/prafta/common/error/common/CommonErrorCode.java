package com.prafta.common.error.common;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

public enum CommonErrorCode implements ApiErrorCode {

    COMMON_400_001(HttpStatus.BAD_REQUEST, "요청 필수 파라미터가 누락되었습니다.")
    , COMMON_400_002(HttpStatus.BAD_REQUEST, "유효하지 않은값이 포함되었습니다.\n확인 후 다시 요청해주십시오.")
    , COMMON_400_003(HttpStatus.BAD_REQUEST, "토큰정보가 존재하지 않습니다.")
    , COMMON_400_004(HttpStatus.BAD_REQUEST, "계정정보가 존재하지 않습니다.")
    , COMMON_400_401(HttpStatus.NOT_FOUND, "조회결과가 없습니다.\n관리자에게 문의해주세요.")
    , COMMON_400_600(HttpStatus.UNAUTHORIZED, "인증되지 않은 토큰입니다.\n관리자에게 문의해주세요.")
    , COMMON_500_001(HttpStatus.INTERNAL_SERVER_ERROR, "처리 중 오류가 발생하였습니다.\\n관리자에게 문의해주십시오.")
    
    ;

    private final HttpStatus httpStatus;
    private final String message;

    CommonErrorCode(HttpStatus httpStatus, String message) {
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