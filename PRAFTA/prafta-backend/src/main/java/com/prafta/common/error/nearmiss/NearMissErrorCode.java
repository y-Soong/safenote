package com.prafta.common.error.nearmiss;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 아차사고/사건 보고 도메인 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 */
public enum NearMissErrorCode implements ApiErrorCode {

    // 422: 허용되지 않은 상태 전이 (100->200->300->400, 어디서든 900)
    NEARMISS_422_001(HttpStatus.UNPROCESSABLE_ENTITY, "허용되지 않은 상태 전환입니다.\n확인 후 다시 시도해주세요.")
    // 422: 이미 처리되었거나 이관된 위험성평가 건 재분류 차단 (중복 전환 방지)
    , NEARMISS_422_002(HttpStatus.UNPROCESSABLE_ENTITY, "이미 처리되었거나 이관된 위험성평가 건입니다.")
    // 404: 대상 사건이 존재하지 않거나 사업장 스코프 밖
    , NEARMISS_404_001(HttpStatus.NOT_FOUND, "대상 사건을 찾을 수 없습니다.")
    // 404: 재분류 원본 위험성평가 건 없음
    , NEARMISS_404_002(HttpStatus.NOT_FOUND, "재분류 대상 위험성평가 건을 찾을 수 없습니다.")
    // 400: 반려 시 사유 누락
    , NEARMISS_400_001(HttpStatus.BAD_REQUEST, "반려 사유를 입력해주세요.")
    // 403: 해당 사업장에 대한 접근 권한 없음 (cross-site IDOR 차단)
    , NEARMISS_403_001(HttpStatus.FORBIDDEN, "해당 사업장에 대한 접근 권한이 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    NearMissErrorCode(HttpStatus httpStatus, String message) {
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
