package com.prafta.common.exception;

import com.prafta.common.error.ApiErrorCode;

import java.util.Map;
import java.util.Objects;

public class ApiException extends RuntimeException {

    private final ApiErrorCode errorCode;

    /**
     * 기존: detailMessage가 있으면 enum 메시지를 "대체"하는 용도
     */
    private final String detailMessage;

    /**
     * 신규: enum 기본 메시지 뒤에 "추가"로 붙일 메시지
     * - 기존 코드 영향 최소화를 위해 별도 필드로 분리
     */
    private final String appendMessage;

    /**
     * prafta-app-011: 에러 응답 body 에 포함할 추가 데이터 (null 이면 응답에서 제외).
     * - 사용 예: 403 siteMismatch 시 { "userSiteName": "..." } 를 응답 JSON 에 병합.
     */
    private final Map<String, Object> extraData;

    public ApiException(ApiErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
        this.detailMessage = errorCode.message();
        this.appendMessage = null;
        this.extraData = null;
    }

    /**
     * 기존 동작 유지: detailMessage가 있으면 enum 메시지를 "대체"
     */
    public ApiException(ApiErrorCode errorCode, String detailMessage) {
        super(detailMessage); // RuntimeException message도 detail로
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
        this.appendMessage = null;
        this.extraData = null;
    }

    /**
     * 신규: append 용도 (생성자 직접 호출보단 append() 팩토리 사용 권장)
     * - message는 "기본 + append"로 넣어 디버깅/로그에서도 그대로 보이게 함
     */
    private ApiException(ApiErrorCode errorCode, String detailMessage, String appendMessage, boolean useAppendMode) {
        super(resolveMessage(errorCode, detailMessage, appendMessage, useAppendMode));
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
        this.appendMessage = appendMessage;
        this.extraData = null;
    }

    /**
     * prafta-app-011: extraData 포함 생성자.
     */
    private ApiException(ApiErrorCode errorCode, String detailMessage, String appendMessage,
                         boolean useAppendMode, Map<String, Object> extraData) {
        super(resolveMessage(errorCode, detailMessage, appendMessage, useAppendMode));
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
        this.appendMessage = appendMessage;
        this.extraData = extraData;
    }

    public ApiErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getExtraData() {
        return extraData;
    }

    /**
     * 최종 메시지 해석 규칙
     * 1) detailMessage가 있으면: detailMessage로 "대체" (기존 동작 유지)
     * 2) detailMessage 없고 appendMessage가 있으면: 기본 + "\n" + appendMessage
     * 3) 둘 다 없으면: 기본 메시지
     */
    public String getResolvedMessage() {
        // 1) 기존 대체 우선
        if (detailMessage != null && !detailMessage.isBlank()) {
            return detailMessage;
        }

        // 2) append 모드
        String append = Objects.toString(appendMessage, "").trim();
        if (!append.isBlank()) {
            return errorCode.message() + "\n" + append;
        }

        // 3) 기본
        return errorCode.message();
    }

    /**
     * append 모드로 예외 생성 (기본 메시지 + 추가 메시지)
     */
    public static ApiException append(ApiErrorCode errorCode, String appendMessage) {
        return new ApiException(errorCode, null, appendMessage, true);
    }

    /**
     * append 모드로 예외 생성 (String.format 스타일)
     * - 예: ApiException.appendf(code, "필수값 누락(\"%s\")", "TokenInfo is required")
     */
    public static ApiException appendf(ApiErrorCode errorCode, String format, Object... args) {
        String msg = (format == null) ? null : String.format(format, args);
        return new ApiException(errorCode, null, msg, true);
    }

    /**
     * prafta-app-011: extraData 포함 예외 생성 (응답 JSON 에 추가 키 병합용).
     * - 예: ApiException.withExtra(CHKLST_403_001, Map.of("userSiteName", siteNm))
     */
    public static ApiException withExtra(ApiErrorCode errorCode, Map<String, Object> extraData) {
        return new ApiException(errorCode, null, null, false, extraData);
    }

    private static String resolveMessage(ApiErrorCode errorCode,
                                         String detailMessage,
                                         String appendMessage,
                                         boolean useAppendMode) {

        // detailMessage는 "대체"이므로 그대로
        if (detailMessage != null && !detailMessage.isBlank()) {
            return detailMessage;
        }

        // append 모드면 기본+append로 RuntimeException message 세팅
        if (useAppendMode) {
            String append = Objects.toString(appendMessage, "").trim();
            if (!append.isBlank()) {
                return errorCode.message() + "\n" + append;
            }
        }

        return errorCode.message();
    }
}