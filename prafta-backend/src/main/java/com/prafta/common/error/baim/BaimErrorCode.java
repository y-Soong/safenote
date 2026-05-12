package com.prafta.common.error.baim;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

public enum BaimErrorCode implements ApiErrorCode {

    // 규칙 예시: {MODULE}_{HTTP}_{SEQ}
    BAIM_400_001(HttpStatus.BAD_REQUEST, "하위 부서가 존재하는 경우 삭제할 수 없습니다.")
    , BAIM_400_002(HttpStatus.BAD_REQUEST, "부서내 관리자가 ")
    , BAIM_400_003(HttpStatus.BAD_REQUEST, "동일한 핸드폰번호를 사용중인\n일일사용자 계정이 존재합니다.")
    , BAIM_500_001(HttpStatus.INTERNAL_SERVER_ERROR, "약관 데이터 생성 오류 !\n관리자에게 문의해주세요.")
    , BAIM_500_002(HttpStatus.INTERNAL_SERVER_ERROR, "조직정보 삭제에 실패했습니다.\n관리자에게 문의해주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    BaimErrorCode(HttpStatus httpStatus, String message) {
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