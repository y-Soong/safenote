package com.prafta.common.error.safety;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 관리자 안전 관리(순회점검 결과/위험성평가) 도메인 에러코드 (prafta-app-025 J1-6).
 *
 * <p>규칙: {MODULE}_{HTTP}_{SEQ}. 신규 enum 카탈로그(DB 시드 아님).
 *
 * <p>⚠️ 앱 인터셉터가 토큰 에러로 오인하는 코드(AUTH_*, COMMON_400_600/003)와 겹치지 않도록
 * 400/403/404/409/422 만 사용한다(메모리 prafta-app-req07: 003/600 만 강제 로그아웃).
 *
 * <p>403/404 는 보안 민감(존재 여부/권한 누출 방지)으로 일반화 메시지를 사용하고,
 * 상세 사유(타사업장/스코프밖)는 서버 로그에만 기록한다.
 */
public enum SafetyErrorCode implements ApiErrorCode {

    // 400: 필수 입력값 누락(002 전환 시 개선예정일/임시조치 등)
    SAFETY_400_001(HttpStatus.BAD_REQUEST, "필수 입력값이 누락되었습니다.")
    // 403: 진입 게이트/사업장 접근 거부(일반화)
    , SAFETY_403_001(HttpStatus.FORBIDDEN, "안전 관리 권한이 없습니다.")
    // 404: 사업장 스코프 밖 리소스(IDOR 차단 — 일반화)
    , SAFETY_404_001(HttpStatus.NOT_FOUND, "대상을 찾을 수 없습니다.")
    // 409: 상태 가드 영향 0건(동시 전환 — 멱등 안내)
    , SAFETY_409_001(HttpStatus.CONFLICT, "이미 처리되어 상태가 변경되었습니다.\n새로고침 후 다시 시도해 주세요.")
    // 422: 전이표 외 전이
    , SAFETY_422_001(HttpStatus.UNPROCESSABLE_ENTITY, "허용되지 않는 상태 전환입니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    SafetyErrorCode(HttpStatus httpStatus, String message) {
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
