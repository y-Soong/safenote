package com.prafta.common.error.auth;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

public enum AuthErrorCode implements ApiErrorCode {

    // 규칙 예시: {MODULE}_{HTTP}_{SEQ}
	AUTH_500_001(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다. 다시 로그인해 주세요.")
	, AUTH_500_002(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다. 다시 로그인해 주세요.")
	, AUTH_500_003(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다. 다시 로그인해 주세요.")
	// prafta-057: 다른 환경(다른 브라우저/PC)에서의 신규 로그인으로 현재 세션이 폐기됨.
	//   HTTP 409(CONFLICT) — 토큰 만료(401)와 구분하여 프론트가 refresh 시도 없이 즉시 안내 후 로그아웃한다.
	, AUTH_409_001(HttpStatus.CONFLICT, "다른 환경에서 로그인을 감지했습니다.")
	// prafta-app-033: 이용약관 동의 / 강제 비밀번호 변경 미완료 상태에서의 일반 API 접근.
	//   HTTP 403(FORBIDDEN) — 토큰 만료(401)와 구분하여 프론트가 refresh 시도 없이 즉시 로그아웃 후 재로그인을 유도한다.
	//   ★사용자에게 노출되는 메시지이므로 내부 용어("게이트") 없이 정확한 사유를 안내한다. 사유별 구체 메시지는
	//    throw 지점(AuthAspect)에서 detailMessage 로 덮어쓰며, 아래는 사유 미상 시의 폴백이다.
	, AUTH_403_001(HttpStatus.FORBIDDEN, "이용약관 동의 또는 비밀번호 변경이 필요합니다.\n다시 로그인해 주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    AuthErrorCode(HttpStatus httpStatus, String message) {
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