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

    // ===== 로그인 ID 전역 유일화 - 셀프 회원가입 ID 중복 =====
    , LOGIN_400_016(HttpStatus.BAD_REQUEST, "이미 사용 중인 아이디입니다. 다른 아이디를 입력해 주세요.")

    // ===== PRAFTA-SUBCON-T2-07 - 연동 미러 사업장 가입 차단 (미러 존재 사실 상세 비노출) =====
    , LOGIN_400_017(HttpStatus.BAD_REQUEST, "가입할 수 없는 사업장입니다.\n관리자에게 문의해 주세요.")

    // ===== 소정-04 - 셀프가입 승인제 (ACCOUNT_STATUS '06 가입승인대기' / '07 가입거부') =====
    // '06' 승인대기 계정 로그인 — 정식 토큰/임시 scope 토큰 모두 발급하지 않는다(승인 전 접근 차단).
    , LOGIN_400_018(HttpStatus.BAD_REQUEST, "가입 승인 대기 중인 계정입니다.\n관리자 승인 후 이용하실 수 있습니다.")
    // '07' 거부 계정 로그인 — 비밀번호 검증을 통과한 경우에만 안내한다(계정 존재 탐색 방지).
    , LOGIN_400_019(HttpStatus.BAD_REQUEST, "가입이 거부된 계정입니다.\n재가입을 원하시면 회원가입을 다시 진행해 주세요.")
    // 셀프가입 휴대폰 중복 — 회사 내 UNIQUE(UX_TB_USER_MBL_NO) 사전 진단(구 500 대체).
    , LOGIN_400_020(HttpStatus.BAD_REQUEST, "이미 사용 중인 휴대폰번호입니다.")
    // 재가입 재활용 대상이 아이디/휴대폰 기준으로 서로 다른 '07' 행을 가리키는 경우(병합 불가).
    , LOGIN_400_021(HttpStatus.BAD_REQUEST, "이전 가입 신청 정보와 충돌하여 재가입할 수 없습니다.\n관리자에게 문의해 주세요.")

    // ===== [security H-1] 셀프가입 휴대폰 본인인증 서버 강제 =====
    // 인증 기록 없음 / 인증 창(30분) 만료 / 이미 사용된 인증 — 셋을 구분하지 않는다(공격자에게 상태 힌트 금지).
    , LOGIN_400_022(HttpStatus.BAD_REQUEST, "휴대폰 본인인증 후 가입할 수 있습니다.\n인증을 다시 진행해 주세요.")

    // ===== 셀프가입 필수약관 동의 검증 =====
    // 정상 화면에서는 전 항목 체크 전까지 다음 단계로 넘어가지 못하므로 사용자가 볼 일이 거의 없다.
    // (EP 직접 호출 / 신버전 클라이언트의 계약 위반 시 도달)
    , LOGIN_400_023(HttpStatus.BAD_REQUEST, "필수 약관에 모두 동의해야 가입할 수 있습니다.")
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