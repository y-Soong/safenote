package com.prafta.common.error.dailyentry;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 일용직 입장 승인요청(dailyentry) 도메인 전용 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 *
 * <p>출처: 일용직 계약서+승인제 plan §3-5. 관리자 승인/거부 처리 API 에서 사용한다.
 * 일용직 본인 로그인 흐름의 안내(승인 대기/거부)는 {@code DailyLoginErrorCode} 006/007 을 사용한다.
 */
public enum DailyEntryErrorCode implements ApiErrorCode {

    // 이미 승인/거부/만료/소진된 요청을 다시 처리하려는 경우(멱등 가드).
    DAILYENTRY_400_001(HttpStatus.BAD_REQUEST, "이미 처리된 요청입니다.")
    // 처리자(관리자)가 해당 요청의 사업장 권한을 보유하지 않은 경우(IDOR 차단).
    , DAILYENTRY_403_001(HttpStatus.FORBIDDEN, "해당 사업장의 처리 권한이 없습니다.")
    // 요청 미존재(타 회사 요청 포함 — 존재 비노출).
    , DAILYENTRY_404_001(HttpStatus.NOT_FOUND, "승인 요청을 찾을 수 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    DailyEntryErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String message() {
        return message;
    }
}
