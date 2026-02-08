package com.prafta.common.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
	
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "서버 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    @ExceptionHandler(BaimApiException.class)
    public ResponseEntity<Map<String, Object>> BaimApiException(BaimApiException ex) {
    	Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }
    
    @ExceptionHandler(LoginFailException.class)
    public ResponseEntity<Map<String, Object>> LoginFailException(LoginFailException ex) {
    	Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }
    
    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> FileNotFoundException(FileNotFoundException ex) {
    	Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }
    
    @ExceptionHandler(CmmApiException.class)
    public ResponseEntity<Map<String, Object>> CmmApiException(CmmApiException ex) {
    	Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }
    
    @ExceptionHandler(ChkLstApiException.class)
    public ResponseEntity<Map<String, Object>> ChkLstApiException(ChkLstApiException ex) {
    	Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }
    
    @ExceptionHandler(RiskApiException.class)
    public ResponseEntity<Map<String, Object>> RiskApiException(RiskApiException ex) {
    	Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }
    
    @ExceptionHandler(TbmApiException.class)
    public ResponseEntity<Map<String, Object>> TbmApiException(TbmApiException ex) {
    	Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(response);
    }
}