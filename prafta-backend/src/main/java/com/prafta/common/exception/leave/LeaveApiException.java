package com.prafta.common.exception.leave;

import org.springframework.http.HttpStatus;

public class LeaveApiException extends RuntimeException {
	private final HttpStatus status;
	
	public LeaveApiException(String message) {
        super(message);
        this.status = HttpStatus.NOT_FOUND;   // 기존 기본값 유지
    }

    public LeaveApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
