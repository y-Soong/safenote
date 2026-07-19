package com.prafta.common.exception;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import com.prafta.common.error.ApiErrorCode;
import com.prafta.common.error.common.CommonErrorCode;

import jakarta.validation.ConstraintViolationException;
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

    /**
     * 요청 본문/파라미터 bean-validation 실패(@Valid) 처리.
     *
     * <p>이전에는 검증 실패가 catch-all {@link #handleGeneralException(Exception)} 로 떨어져
     * 클라이언트가 500(COMMON_500_000 "서버 오류")을 받았다. 실제로는 사용자 입력 오류(400)이므로,
     * 검증 예외를 400(COMMON_400_002)으로 변환하고 실패 필드/사유를 메시지에 담아 반환한다.
     *
     * <p>대상:
     * <ul>
     *   <li>{@link MethodArgumentNotValidException} — {@code @Valid @RequestBody DTO}</li>
     *   <li>{@link HandlerMethodValidationException} — 메서드 파라미터/리스트 원소 {@code @Valid}(Spring 6.1+)</li>
     *   <li>{@link ConstraintViolationException} — {@code @Validated} 클래스 레벨 제약</li>
     * </ul>
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<Map<String, Object>> handleValidationException(Exception ex) {
        String detail = extractValidationDetail(ex);
        log.warn("[ValidationException] type={} detail={}", ex.getClass().getSimpleName(), detail);

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", CommonErrorCode.COMMON_400_002.code());
        // 실패 필드 사유가 있으면 함께 안내(어느 값이 문제인지 파악 가능), 없으면 표준 문구.
        response.put("message", (detail == null || detail.isBlank())
                ? CommonErrorCode.COMMON_400_002.message()
                : CommonErrorCode.COMMON_400_002.message() + "\n" + detail);
        return ResponseEntity.status(CommonErrorCode.COMMON_400_002.httpStatus()).body(response);
    }

    /** 검증 예외에서 사람이 읽을 수 있는 실패 사유(필드:메시지)를 안전하게 추출한다(최대 5건). */
    private String extractValidationDetail(Exception ex) {
        try {
            if (ex instanceof MethodArgumentNotValidException manv) {
                return manv.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fieldLabel(fe) + ": " + fe.getDefaultMessage())
                        .distinct().limit(5).collect(Collectors.joining("\n"));
            }
            if (ex instanceof HandlerMethodValidationException hmv) {
                return hmv.getAllErrors().stream()
                        .map(err -> err.getDefaultMessage())
                        .filter(m -> m != null && !m.isBlank())
                        .distinct().limit(5).collect(Collectors.joining("\n"));
            }
            if (ex instanceof ConstraintViolationException cve) {
                return cve.getConstraintViolations().stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .distinct().limit(5).collect(Collectors.joining("\n"));
            }
        } catch (Exception ignore) {
            // 상세 추출 실패는 무시하고 표준 문구로 폴백(핸들러 자체가 500 나지 않도록).
        }
        return null;
    }

    /** FieldError 의 필드명 반환(@FieldLabel 등 커스텀 라벨은 defaultMessage 에 포함되므로 여기선 필드명만). */
    private String fieldLabel(FieldError fe) {
        return fe.getField();
    }

    /**
     * 요청 JSON 바디 역직렬화 실패 처리(2026-07-16 QA D-1).
     *
     * <p>바디 타입 불일치(예: Integer 필드에 "abc")·malformed JSON 은 Jackson 이
     * {@link HttpMessageNotReadableException} 을 던지는데, 이전에는 catch-all
     * {@link #handleGeneralException(Exception)} 로 떨어져 500 을 반환했다.
     * 실제로는 사용자 입력 오류(400)이므로 COMMON_400_002 로 변환한다.
     *
     * <p>⚠️ 파싱 상세(내부 클래스명/필드 경로 등)는 정보 노출이 되므로 응답에는
     * 고정 안내 문구만 담고, 상세는 서버 warn 로그에만 기록한다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadableException(HttpMessageNotReadableException ex) {
        log.warn("[HttpMessageNotReadable] message={}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", CommonErrorCode.COMMON_400_002.code());
        response.put("message", CommonErrorCode.COMMON_400_002.message());
        return ResponseEntity.status(CommonErrorCode.COMMON_400_002.httpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        // [DIAG-A] 일반 예외는 래핑되어 표면 메시지만으로는 원인 파악이 어렵다
        // (예: CannotCreateTransactionException 의 "Could not open JDBC Connection"
        //  뒤에 가려진 SQLException "Access denied ... using password: NO").
        // 래핑 체인의 최하위 원인까지 함께 남겨 실제 원인을 식별한다.
        Throwable rootCause = ex;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        log.warn("[GeneralException] type={} message={} rootCauseType={} rootCauseMessage={}",
                ex.getClass().getName(), ex.getMessage(),
                rootCause.getClass().getName(), rootCause.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("errorCode", "COMMON_500_000");
        response.put("message", "서버 오류가 발생했습니다.");
        return ResponseEntity.status(500).body(response);
    }
}