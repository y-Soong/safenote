package com.prafta.common.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends RuntimeException {
    
    private final HttpStatus status;
	
	public UnauthorizedException(String message) {
        super(message);
        this.status = HttpStatus.NOT_FOUND;   // 기존 기본값 유지
    }

    public UnauthorizedException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
