package com.prafta.common.exception.tbm;

import org.springframework.http.HttpStatus;

public class TbmApiException extends RuntimeException {
	private final HttpStatus status;
	
	public TbmApiException(String message) {
        super(message);
        this.status = HttpStatus.NOT_FOUND;   // 기존 기본값 유지
    }

    public TbmApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
