package com.prafta.common.exception.advice;

import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.prafta.common.annotation.FieldLabel;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class ValidationExceptionHandler {

    // 제약코드 -> 사용자 노출 문구
    private static final Map<String, String> CONSTRAINT_LABEL = Map.of(
            "NotBlank", "필수값 누락",
            "NotNull", "필수값 누락",
            "NotEmpty", "필수값 누락",
            "Length", "길이값 검증 실패",
            "Size", "길이값 검증 실패",
            "Pattern", "형식 검증 실패",
            "Email", "이메일 형식 검증 실패",
            "Min", "최소값 검증 실패",
            "Max", "최대값 검증 실패"
    );

    /** JSON Body(@RequestBody @Valid) */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        return buildValidationResponse(ex.getBindingResult());
    }

    /** ModelAttribute(@ModelAttribute @Valid) */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(BindException ex) {
        return buildValidationResponse(ex.getBindingResult());
    }

    /** @RequestParam/@PathVariable + @Validated */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        // 파라미터 검증은 FieldError가 아니라 ConstraintViolation 기반
        Map<String, LinkedHashSet<String>> grouped = new LinkedHashMap<>();

        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String constraint = v.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(); // NotBlank, Min...
            String groupLabel = CONSTRAINT_LABEL.getOrDefault(constraint, constraint);

            // propertyPath 예: "login.arg0" 이런 식으로 나오기도 해서, 마지막 노드만 쓰는 식으로 단순화
            String path = v.getPropertyPath().toString();
            String paramName = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;

            grouped.computeIfAbsent(groupLabel, k -> new LinkedHashSet<>())
                    .add("'" + paramName + "'");
        }

        String message = buildMessage(grouped);

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("errorCode", "COMMON_400_VALIDATION");
        body.put("message", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private ResponseEntity<Map<String, Object>> buildValidationResponse(BindingResult bindingResult) {
        Object target = bindingResult.getTarget();
        Class<?> dtoClass = (target != null) ? target.getClass() : null;

        Map<String, LinkedHashSet<String>> grouped = new LinkedHashMap<>();

        for (FieldError fe : bindingResult.getFieldErrors()) {
            String constraintCode = extractConstraintCode(fe);
            String groupLabel = CONSTRAINT_LABEL.getOrDefault(constraintCode, constraintCode);

            String displayName = resolveDisplayName(dtoClass, fe.getField());
            grouped.computeIfAbsent(groupLabel, k -> new LinkedHashSet<>())
                    .add("'" + displayName + "'");
        }

        String message = buildMessage(grouped);

        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("errorCode", "COMMON_400_VALIDATION");
        body.put("message", message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    private String buildMessage(Map<String, LinkedHashSet<String>> grouped) {
        // "필수값 누락: '사용자ID', '사용자PW', 길이값 검증 실패: '휴대폰번호'"
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (var e : grouped.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(e.getKey()).append(": ").append(String.join(", ", e.getValue()));
        }
        return sb.toString();
    }

    private String extractConstraintCode(FieldError fe) {
        if (fe.getCode() != null && !fe.getCode().isBlank()) return fe.getCode();

        String[] codes = fe.getCodes();
        if (codes != null) {
            for (String c : codes) {
                if (c == null) continue;
                int dot = c.indexOf('.');
                String candidate = (dot > 0) ? c.substring(0, dot) : c;
                if (!candidate.isBlank()) return candidate;
            }
        }
        return "Invalid";
    }

    /** @FieldLabel("사용자ID") 기반 표시명 */
    private String resolveDisplayName(Class<?> dtoClass, String fieldPath) {
        if (dtoClass == null || fieldPath == null || fieldPath.isBlank()) return fieldPath;

        String topField = fieldPath;
        int dot = topField.indexOf('.');
        if (dot > 0) topField = topField.substring(0, dot);
        int bracket = topField.indexOf('[');
        if (bracket > 0) topField = topField.substring(0, bracket);

        // record 지원
        if (dtoClass.isRecord()) {
            for (RecordComponent rc : dtoClass.getRecordComponents()) {
                if (rc.getName().equals(topField)) {
                    FieldLabel label = rc.getAnnotation(FieldLabel.class);
                    if (label != null && !label.value().isBlank()) return label.value();
                    return topField;
                }
            }
        }

        // class 지원(현재 LoginRequest는 class라서 여기 추가!)
        try {
            var field = dtoClass.getDeclaredField(topField);
            FieldLabel label = field.getAnnotation(FieldLabel.class);
            if (label != null && !label.value().isBlank()) return label.value();
        } catch (NoSuchFieldException ignore) {
        }

        return topField;
    }
}