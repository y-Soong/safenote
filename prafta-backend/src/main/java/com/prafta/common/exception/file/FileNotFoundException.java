package com.prafta.common.exception.file;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class FileNotFoundException extends RuntimeException {
    
	private final HttpStatus status;
	
	public FileNotFoundException(String message) {
        super(message);
        this.status = HttpStatus.NOT_FOUND;   // 기존 기본값 유지
    }

    public FileNotFoundException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
