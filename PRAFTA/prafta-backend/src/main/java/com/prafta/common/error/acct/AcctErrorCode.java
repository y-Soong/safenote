package com.prafta.common.error.acct;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 사고관리 도메인 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 */
public enum AcctErrorCode implements ApiErrorCode {

    // 400: 필수 입력값 누락(발생일/시각/재해자/등급/경위 등)
    ACCT_400_001(HttpStatus.BAD_REQUEST, "필수 입력값이 누락되었습니다.\n확인 후 다시 시도해주세요.")
    // 400: 선택한 재해자가 해당 사업장에 실재하지 않음(유령/타 사업장 참조 차단)
    , ACCT_400_002(HttpStatus.BAD_REQUEST, "선택한 재해자가 해당 사업장에 존재하지 않습니다.")
    // 404: 대상 사고가 존재하지 않거나 사업장 스코프 밖
    , ACCT_404_001(HttpStatus.NOT_FOUND, "대상 사고를 찾을 수 없습니다.")
    // 403: 해당 사업장에 대한 접근 권한 없음 (cross-site IDOR 차단)
    , ACCT_403_001(HttpStatus.FORBIDDEN, "해당 사업장에 대한 접근 권한이 없습니다.")
    // 403: 해당 작업에 필요한 역할 권한 없음 (BTN_NEW/SAVE/DELT 서버 강제)
    , ACCT_403_002(HttpStatus.FORBIDDEN, "해당 작업을 수행할 권한이 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    AcctErrorCode(HttpStatus httpStatus, String message) {
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
