package com.prafta.common.exception.risk;

import org.springframework.http.HttpStatus;

public class RiskApiException extends RuntimeException {
	private final HttpStatus status;
	
	public RiskApiException(String message) {
        super(message);
        this.status = HttpStatus.NOT_FOUND;   // 기존 기본값 유지
    }

    public RiskApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
