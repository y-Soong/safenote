package com.prafta.common.exception;

import org.springframework.http.HttpStatus;

public class BaimApiException extends RuntimeException {
	private final HttpStatus status;
	
	public BaimApiException(String message) {
        super(message);
        this.status = HttpStatus.NOT_FOUND;   // 기존 기본값 유지
    }

    public BaimApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
