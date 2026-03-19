package com.prafta.common.exception.chkLst;

import org.springframework.http.HttpStatus;

public class ChkLstApiException extends RuntimeException {
	private final HttpStatus status;
	
	public ChkLstApiException(String message) {
        super(message);
        this.status = HttpStatus.NOT_FOUND;   // 기존 기본값 유지
    }

    public ChkLstApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
