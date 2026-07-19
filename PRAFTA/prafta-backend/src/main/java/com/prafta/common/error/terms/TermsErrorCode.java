package com.prafta.common.error.terms;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 약관(Terms) 도메인 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 *
 * <p>앱 약관 게이트(필수약관 동의) / 마이페이지 선택약관 토글 전용.
 * <p>★주의: 앱 인터셉터는 COMMON_400_003 / COMMON_400_600 을 토큰 오류로 간주해 강제 로그아웃시킨다.
 *    따라서 진짜 인증 결함이 아닌 입력 검증 실패에는 본 TERMS_400_* 을 사용한다.
 */
public enum TermsErrorCode implements ApiErrorCode {

    // 400: 약관ID 누락 또는 동의여부(Y/N) 화이트리스트 위반
    TERMS_400_001(HttpStatus.BAD_REQUEST, "약관 정보가 올바르지 않습니다.")
    // 403: 선택약관 토글 경로로 필수약관/미사용약관을 변경하려는 시도(게이트 우회 차단)
    , TERMS_403_001(HttpStatus.FORBIDDEN, "해당 약관은 선택 동의 대상이 아닙니다.")
    // 404: 응답 대상 약관 미배포/미사용(USE_YN='N') 상태에서 동의 응답을 시도(SUBCON-T4)
    , TERMS_404_001(HttpStatus.NOT_FOUND, "해당 약관을 찾을 수 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    TermsErrorCode(HttpStatus httpStatus, String message) {
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
