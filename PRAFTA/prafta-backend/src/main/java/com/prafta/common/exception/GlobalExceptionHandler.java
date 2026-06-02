package com.prafta.common.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.prafta.common.error.ApiErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
	    ApiErrorCode ec = ex.getErrorCode();

	    // [DIAG-A] mojibake 영향 없이 정확한 errorCode를 식별하기 위한 진단 로그
	    log.warn("[ApiException] code={} status={} resolvedMessage={}",
	            ec.code(), ec.httpStatus(), ex.getResolvedMessage());

	    Map<String, Object> response = new HashMap<>();
	    response.put("success", false);
	    response.put("errorCode", ec.code());
	    response.put("message", ex.getResolvedMessage());

	    // prafta-app-011: extraData 가 있으면 응답 JSON 에 병합 (null 키 제외)
	    if (ex.getExtraData() != null) {
	        ex.getExtraData().forEach((k, v) -> {
	            if (k != null) response.put(k, v);
	        });
	    }

	    return ResponseEntity.status(ec.httpStatus()).body(response);
	}

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        // [DIAG-A] 일반 예외도 클래스/메시지를 로그로 남겨 정체 파악
        log.warn("[GeneralException] type={} message={}",
                ex.getClass().getName(), ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", "COMMON_500_000");
        response.put("message", "서버 오류가 발생했습니다.");
        return ResponseEntity.status(500).body(response);
    }
}