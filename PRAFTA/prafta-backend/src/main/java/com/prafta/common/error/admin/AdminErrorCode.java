package com.prafta.common.error.admin;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * app 관리자 모드(셸/진입판정/현장전환) 전용 에러코드.
 *
 * <p>001 Phase 1 진입판정(access-context)에서 사용한다. 식별자는 JWT 클레임에서만 도출하므로
 * 클라이언트가 임의 사업장(siteCd)을 지정해 타 사업장 컨텍스트를 조회하려는 시도(IDOR)를 차단할 때 사용한다.
 */
public enum AdminErrorCode implements ApiErrorCode {

    /** 현장 전환 시 선택한 사업장에 대한 접근 권한(USE_YN='Y')이 없음. IDOR 차단. */
    ADMIN_403_001(HttpStatus.FORBIDDEN, "선택한 사업장에 대한 접근 권한이 없습니다.")

    ;

    private final HttpStatus httpStatus;
    private final String message;

    AdminErrorCode(HttpStatus httpStatus, String message) {
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
