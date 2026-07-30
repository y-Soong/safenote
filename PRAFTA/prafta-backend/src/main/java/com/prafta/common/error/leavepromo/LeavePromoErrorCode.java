package com.prafta.common.error.leavepromo;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 연차 사용촉진(1차 앱 계획서 / 2차 웹 직권지정) 도메인 에러코드 (PRAFTA-COM-008-A-3/A-4).
 *
 * <p>규칙: {MODULE}_{HTTP}_{SEQ}. ★토큰오류 코드(COMMON_400_003/600 류) 재사용 금지(앱 인터셉터가
 * 그 코드를 강제 로그아웃으로 처리 — 메모리 app_req07_token_logout_bug). 일반 검증 실패는 400_xxx 신규 채번.
 */
public enum LeavePromoErrorCode implements ApiErrorCode {

    // 400: 요청 본문 검증 실패(날짜 형식/빈 목록 등)
    LEAVEPROMO_400_001(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),
    // 400: 등록 가능한 촉진 연차가 없습니다(잔여 부족/마감/비근무일 등 — 일부/전부 실패 집계)
    LEAVEPROMO_400_002(HttpStatus.BAD_REQUEST, "지정할 수 있는 연차일이 없습니다."),
    // 400: 촉진 회차 종료(D8) — 기준 만료일 2개월 전이 지나 절차 진행 불가(근로기준법 §61 2차 통보 기한)
    LEAVEPROMO_400_003(HttpStatus.BAD_REQUEST, "촉진 기한(만료 2개월 전)이 지나 2차 직권지정을 할 수 없습니다."),
    // 403: 2차 직권지정 권한 없음(master/hr/safe 또는 노드 관리자만)
    LEAVEPROMO_403_001(HttpStatus.FORBIDDEN, "연차 사용촉진 지정 권한이 없습니다."),
    // 404: 대상 사용자 없음/스코프 밖
    LEAVEPROMO_404_001(HttpStatus.NOT_FOUND, "대상 사용자를 찾을 수 없습니다."),
    ;

    private final HttpStatus httpStatus;
    private final String message;

    LeavePromoErrorCode(HttpStatus httpStatus, String message) {
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
