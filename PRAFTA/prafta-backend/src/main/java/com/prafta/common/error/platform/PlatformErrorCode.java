package com.prafta.common.error.platform;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 플랫폼 운영자(/platformApi) 영역 전용 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 *
 * <p>신규 고객사 프로비저닝·글로벌 약관 관리 등 초고권한 엔드포인트의 접근 차단 사유.
 * 메뉴 숨김은 UX 일 뿐이고, 실제 차단은 본 코드들을 던지는 게이트(서버측)가 강제한다.
 */
public enum PlatformErrorCode implements ApiErrorCode {

    // 플랫폼 운영자 권한 아님(토큰 회사코드 ≠ prafta_system_admin / 토큰 부재).
    PLATFORM_403_001(HttpStatus.FORBIDDEN, "플랫폼 운영자만 접근할 수 있습니다.")
    // 허용되지 않은 IP 에서의 접근(IP 허용목록 미일치).
    , PLATFORM_403_002(HttpStatus.FORBIDDEN, "허용되지 않은 접근 경로입니다.")

    // ===== 신규 고객사 프로비저닝(POST /platformApi/company) 입력/처리 오류 =====
    // 필수 입력값(회사명/사업자번호/관리자명/관리자ID/관리자 휴대폰) 누락.
    , PLATFORM_400_001(HttpStatus.BAD_REQUEST, "필수 입력값이 누락되었습니다.")
    // 사업자번호 형식 오류(숫자 10자리).
    , PLATFORM_400_002(HttpStatus.BAD_REQUEST, "사업자번호 형식이 올바르지 않습니다.")
    // 관리자 휴대폰번호 형식 오류(숫자 10~11자리).
    , PLATFORM_400_003(HttpStatus.BAD_REQUEST, "관리자 휴대폰번호 형식이 올바르지 않습니다.")
    // 계약 종료일 형식 오류(YYYYMMDD 8자리).
    , PLATFORM_400_004(HttpStatus.BAD_REQUEST, "계약 종료일 형식이 올바르지 않습니다.")
    // 회사코드(CMPNY_CD) 발급 실패(충돌 재시도 한도 초과 — 일시적 오류, 재시도 권장).
    , PLATFORM_400_005(HttpStatus.BAD_REQUEST, "회사코드 발급에 실패했습니다. 잠시 후 다시 시도해 주세요.")
    // 관리자 ID 길이 오류(3~50자).
    , PLATFORM_400_006(HttpStatus.BAD_REQUEST, "관리자 ID는 3~50자여야 합니다.")
    // 관리자 ID 형식 오류(영문/숫자/특수문자(_,-,.) 조합).
    , PLATFORM_400_007(HttpStatus.BAD_REQUEST, "관리자 ID는 영문, 숫자, 언더스코어(_), 하이픈(-), 점(.)만 사용 가능합니다.")
    // 휴대폰번호 중복(이미 다른 고객사의 master 계정으로 사용 중).
    , PLATFORM_400_008(HttpStatus.BAD_REQUEST, "이 휴대폰번호는 이미 다른 고객사의 관리자 계정으로 등록되어 있습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    PlatformErrorCode(HttpStatus httpStatus, String message) {
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
