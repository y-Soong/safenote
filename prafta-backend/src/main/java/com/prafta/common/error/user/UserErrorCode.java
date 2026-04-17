package com.prafta.common.error.user;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

public enum UserErrorCode implements ApiErrorCode {

    // 占쏙옙칙 占쏙옙占쏙옙: {MODULE}_{HTTP}_{SEQ}
    USER_400_001(HttpStatus.BAD_REQUEST, "부서의 관리자는 소속부서를 변경할 수 없습니다.\n부서 관리자 직위 해제 후 다시 시도해주세요.")
    , USER_400_002(HttpStatus.BAD_REQUEST, "부서의 관리자는 미사용 처리할 수 없습니다.\n부서 관리자 직위 해제 후 다시 시도해주세요.")
    , USER_400_003(HttpStatus.BAD_REQUEST, "현재 비밀번호가 맞지 않습니다.")
    , USER_400_004(HttpStatus.BAD_REQUEST, "비밀번호는 숫자, 영문자, 특수문자 중 2가지\n이상을 포함하여 6~15자로 작성해 주세요.")
    , USER_400_005(HttpStatus.BAD_REQUEST, "부서의 관리자는 탈퇴 처리할 수 없습니다.\n부서 관리자 직위 해제 후 다시 시도해주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    UserErrorCode(HttpStatus httpStatus, String message) {
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