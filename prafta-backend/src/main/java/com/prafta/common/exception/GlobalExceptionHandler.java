package com.prafta.common.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.prafta.common.error.ApiErrorCode;

@ControllerAdvice
public class GlobalExceptionHandler {
	
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
	    ApiErrorCode ec = ex.getErrorCode();

	    Map<String, Object> response = new HashMap<>();
	    response.put("success", false);
	    response.put("errorCode", ec.code());
	    response.put("message", ex.getResolvedMessage());

	    return ResponseEntity.status(ec.httpStatus()).body(response);
	}

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", "COMMON_500_000");
        response.put("message", "서버 오류가 발생했습니다.");
        return ResponseEntity.status(500).body(response);
    }
}