package com.prafta.common.error.login;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

public enum LoginErrorCode implements ApiErrorCode {

    // 규칙 예시: {MODULE}_{HTTP}_{SEQ}
    LOGIN_400_001(HttpStatus.BAD_REQUEST, "아이디 혹은 비밀번호를 확인해주세요.")
    , LOGIN_400_002(HttpStatus.BAD_REQUEST, "사용자 정보가 존재하지 않습니다.")
    , LOGIN_400_003(HttpStatus.BAD_REQUEST, "비밀번호 인증 실패로 계정이 잠겨진 상태입니다.")
    , LOGIN_500_001(HttpStatus.BAD_REQUEST, "약관동의에 실패했습니다.\n관리자에게 문의해주세요.")

    // ===== PRAFTA-036 - 인증대기 분기 / 휴대폰 인증 후 활성화 =====
    , LOGIN_400_010(HttpStatus.BAD_REQUEST, "휴대폰 본인인증이 필요한 계정입니다.")
    , LOGIN_400_011(HttpStatus.BAD_REQUEST, "이미 다른 계정에서 사용 중인 휴대폰번호입니다.")
    , LOGIN_400_012(HttpStatus.BAD_REQUEST, "인증 요청 토큰이 유효하지 않습니다. 다시 로그인해 주세요.")
    , LOGIN_400_013(HttpStatus.BAD_REQUEST, "인증대기 상태의 계정이 아닙니다.")
    , LOGIN_400_014(HttpStatus.BAD_REQUEST, "탈퇴 처리된 계정입니다.")

    // ===== PRAFTA-046 - 노드-관리자 정합성 가드 (가입자 친화 메시지, 내부구조 노출 차단) =====
    , LOGIN_400_015(HttpStatus.BAD_REQUEST, "선택하신 부서는 현재 가입할 수 없습니다.\n관리자에게 문의해 주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    LoginErrorCode(HttpStatus httpStatus, String message) {
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