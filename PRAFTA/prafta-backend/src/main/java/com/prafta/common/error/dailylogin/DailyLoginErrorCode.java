package com.prafta.common.error.dailylogin;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 일용직 직접 로그인(dailylogin) 도메인 전용 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 *
 * <p>정책서: {@code .claude/context/policies/common/03-account-auth.md} §3.5(차단), §3.2(로그인).
 * 보안: 비활성/만료/오ID/오비번/탈퇴를 사유별로 구분 노출하지 않고 통합 메시지(001)로 차단하여
 *       계정 존재 여부를 노출하지 않는다. 잠금(002)만 사용자 안내 목적상 별도 메시지를 둔다.
 */
public enum DailyLoginErrorCode implements ApiErrorCode {

    // 통합 차단 메시지(아이디/비번/비활성/만료/탈퇴 — 계정 존재 비노출).
    DAILYLOGIN_400_001(HttpStatus.BAD_REQUEST, "아이디 혹은 비밀번호를 확인해주세요.")
    // 비밀번호 인증 실패 잠금.
    , DAILYLOGIN_400_002(HttpStatus.BAD_REQUEST, "비밀번호 인증 실패로 계정이 잠겨진 상태입니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    DailyLoginErrorCode(HttpStatus httpStatus, String message) {
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
