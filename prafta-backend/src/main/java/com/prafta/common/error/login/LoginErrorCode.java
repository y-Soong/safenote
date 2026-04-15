package com.prafta.common.error.login;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

public enum LoginErrorCode implements ApiErrorCode {

    // 규칙 예시: {MODULE}_{HTTP}_{SEQ}
    LOGIN_400_001(HttpStatus.BAD_REQUEST, "아이디 혹은 비밀번호를 확인해주세요.")
    , LOGIN_400_002(HttpStatus.BAD_REQUEST, "사용자 정보가 존재하지 않습니다.")
    , LOGIN_400_003(HttpStatus.BAD_REQUEST, "비밀번호 인증 실패로 계정이 잠겨진 상태입니다.")
    , LOGIN_500_001(HttpStatus.BAD_REQUEST, "약관동의에 실패했습니다.\n관리자에게 문의해주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    LoginErrorCode(HttpStatus httpStatus, String message) {
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