package com.prafta.common.error.risk;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 위험성평가-아차사고 참조 연계(prafta-054) 도메인 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 */
public enum RiskLinkErrorCode implements ApiErrorCode {

    // 403: 해당 사업장에 대한 접근 권한 없음 (cross-site IDOR 차단)
    RISKLINK_403_001(HttpStatus.FORBIDDEN, "해당 사업장에 대한 접근 권한이 없습니다.")
    // 404: 대상 위험성평가 건 없음 (또는 사업장 스코프 밖)
    , RISKLINK_404_001(HttpStatus.NOT_FOUND, "대상 위험성평가 건을 찾을 수 없습니다.")
    // 404: 대상 아차사고 없음 (또는 사업장 스코프 밖)
    , RISKLINK_404_002(HttpStatus.NOT_FOUND, "대상 아차사고를 찾을 수 없습니다.")
    // 422: 개선완료(003) 이후의 위험성평가는 참조 아차사고 편집 불가
    , RISKLINK_422_001(HttpStatus.UNPROCESSABLE_ENTITY, "개선완료된 위험성평가는 참조 아차사고를 편집할 수 없습니다.")
    // 422: 완료(400) 상태가 아닌 아차사고는 참조로 연결할 수 없음
    , RISKLINK_422_002(HttpStatus.UNPROCESSABLE_ENTITY, "완료된 아차사고만 참조로 연결할 수 있습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    RiskLinkErrorCode(HttpStatus httpStatus, String message) {
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
