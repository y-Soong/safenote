package com.prafta.common.error.dailyjoin;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 일일사용자 회원가입(dailyjoin) 도메인 전용 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 */
public enum DailyJoinErrorCode implements ApiErrorCode {

    // 회사/사업장 링크 검증
    DAILYJOIN_404_001(HttpStatus.NOT_FOUND, "유효하지 않은 회원가입 링크입니다.\n사업장 관리자에게 문의해주세요.")

    // 회원가입 차단/중복
    , DAILYJOIN_400_001(HttpStatus.BAD_REQUEST, "이미 사용중인 아이디입니다.")
    , DAILYJOIN_400_002(HttpStatus.BAD_REQUEST, "동일한 휴대폰번호로 가입된\n일일사용자 계정이 존재합니다.")
    , DAILYJOIN_400_003(HttpStatus.BAD_REQUEST, "휴대폰 인증이 완료되지 않았습니다.\n인증 후 다시 시도해주세요.")
    , DAILYJOIN_400_004(HttpStatus.BAD_REQUEST, "현재 회원가입을 받고 있지 않습니다.\n사업장 관리자에게 문의해주세요.")
    , DAILYJOIN_400_005(HttpStatus.BAD_REQUEST, "발급 가능한 계정이 모두 소진되었습니다.\n사업장 관리자에게 문의해주세요.")

    // 입력값 형식/길이 검증 실패
    , DAILYJOIN_400_006(HttpStatus.BAD_REQUEST, "입력값 형식이 올바르지 않습니다.\n다시 확인해주세요.")

    // 약관 데이터 오류
    , DAILYJOIN_500_001(HttpStatus.INTERNAL_SERVER_ERROR, "약관 데이터 생성 오류!\n관리자에게 문의해주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    DailyJoinErrorCode(HttpStatus httpStatus, String message) {
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
