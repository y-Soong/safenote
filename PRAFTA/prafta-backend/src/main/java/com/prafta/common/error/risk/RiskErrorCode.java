package com.prafta.common.error.risk;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

public enum RiskErrorCode implements ApiErrorCode {

    // 규칙 예시: {MODULE}_{HTTP}_{SEQ}
    RISK_400_001(HttpStatus.BAD_REQUEST, "위험요인 구분 하위에 유해요인이 존재합니다.\n확인 후 다시 시도해주세요."),
    // T6-14B-1: 개선완료(003) 저장은 개선 후 위험도가 "매우낮음"(1~3)일 때만 허용
    RISK_400_002(HttpStatus.BAD_REQUEST, "개선완료는 개선 후 위험도가 \"매우낮음\"(1~3)일 때만 저장할 수 있습니다."),
    // T6 Low-B: 첨부 파일 크기 상한(10MB) 초과
    RISK_400_003(HttpStatus.BAD_REQUEST, "첨부 파일 크기가 허용 한도(10MB)를 초과했습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    RiskErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public String code() {
        return name(); // enum 이름을 그대로 코드로 쓰면 관리 쉬움
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