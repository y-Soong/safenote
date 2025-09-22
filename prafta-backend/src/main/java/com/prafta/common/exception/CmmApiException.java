package com.prafta.common.exception;

public class CmmApiException extends RuntimeException {
//	private final int errorCode;
//    private final String messageCode;
    
    public CmmApiException(String message) {
        super(message);
    }
//
//    public CmmApiException(String message, Throwable cause, int errorCode, String messageCode) {
//        super(message, cause);
//        this.errorCode = errorCode;
//        this.messageCode = messageCode;
//    }
//
//    public CmmApiException(String messageCode, int errorCode, String message) {
//        super(message);
//        this.errorCode = errorCode;
//        this.messageCode = messageCode;
//    }
    
    
}
