package com.prafta.common.error.file;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 공통 파일 업로드 관련 에러코드.
 *
 * <p>규칙: {MODULE}_{HTTP}_{SEQ}
 */
public enum FileErrorCode implements ApiErrorCode {

    // 허용 확장자(텍스트/이미지/동영상/음성) 외 파일 업로드 차단.
    FILE_400_001(HttpStatus.BAD_REQUEST,
            "허용되지 않은 파일 형식입니다.\n텍스트/이미지/동영상/음성 파일만 업로드할 수 있습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    FileErrorCode(HttpStatus httpStatus, String message) {
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
