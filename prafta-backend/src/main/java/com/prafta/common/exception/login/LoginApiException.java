package com.prafta.common.exception.login;

import org.springframework.http.HttpStatus;

public class LoginApiException extends RuntimeException {
	
	private final HttpStatus status;
	
	public LoginApiException(String message) {
        super(message);
        this.status = HttpStatus.NOT_FOUND;   // 기존 기본값 유지
    }

    public LoginApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
