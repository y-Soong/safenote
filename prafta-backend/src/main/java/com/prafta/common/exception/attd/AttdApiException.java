package com.prafta.common.exception.attd;

import org.springframework.http.HttpStatus;

public class AttdApiException extends RuntimeException {
	private final HttpStatus status;
	
	public AttdApiException(String message) {
        super(message);
        this.status = HttpStatus.NOT_FOUND;   // 기존 기본값 유지
    }

    public AttdApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
