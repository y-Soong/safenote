package com.prafta.common.error.worktime;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * prafta-app-022: 근무중 게이트(WorktimeGate) 전용 에러코드.
 *
 * <p>안전점검·위험성발굴 등록 / TBM 입실 등 "근무 중에만 허용"하는 안전활동에서,
 * 호출 시점에 본인이 근무중(당일 출근·미퇴근)이 아니면 본 코드로 차단한다.
 * 아차사고 보고(즉시성 예외)·관리/조회 기능은 본 게이트 대상이 아니다(정책: safety §2).
 */
public enum WorktimeErrorCode implements ApiErrorCode {

    // 규칙: {MODULE}_{HTTP}_{SEQ}
    WORKTIME_403_001(HttpStatus.FORBIDDEN, "근무 중에만 이용할 수 있어요. 출근 후 다시 시도해 주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    WorktimeErrorCode(HttpStatus httpStatus, String message) {
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
