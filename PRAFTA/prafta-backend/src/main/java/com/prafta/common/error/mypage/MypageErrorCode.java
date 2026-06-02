package com.prafta.common.error.mypage;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 모바일 앱 마이페이지(prafta-app-010) 전용 에러코드.
 *
 * <p>검증 실패는 HTTP 422(UNPROCESSABLE_ENTITY)로 통일하고, {@code errorCode}(=enum 이름)로
 * 프론트가 어느 필드/원인인지 분기한다(plan §4 명세).
 *
 * <p>code() 는 enum 이름을 그대로 반환하므로 프론트 계약 문자열과 동일해야 한다
 * (예: MOBILE_DUP, INVALID_CODE).
 */
public enum MypageErrorCode implements ApiErrorCode {

    // ===== 프로필 저장(010-02) 입력 검증 =====
    INVALID_USER_NM(HttpStatus.UNPROCESSABLE_ENTITY, "이름을 1~50자로 입력해 주세요.")
    , INVALID_GENDER(HttpStatus.UNPROCESSABLE_ENTITY, "성별 값이 올바르지 않습니다.")
    , INVALID_BIRTH_DATE(HttpStatus.UNPROCESSABLE_ENTITY, "생년월일을 올바르게 입력해 주세요.")
    , INVALID_EMAIL(HttpStatus.UNPROCESSABLE_ENTITY, "이메일 형식이 올바르지 않습니다.")
    , INVALID_MOBILE(HttpStatus.UNPROCESSABLE_ENTITY, "휴대폰 번호 형식이 올바르지 않습니다.")
    , MOBILE_VERIFICATION_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "휴대폰 번호 변경 시 본인 인증이 필요합니다.")
    , MOBILE_VERIFICATION_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "휴대폰 인증 정보가 유효하지 않습니다. 다시 인증해 주세요.")

    // ===== 휴대폰 변경 인증(010-03) =====
    , MOBILE_DUP(HttpStatus.UNPROCESSABLE_ENTITY, "이미 사용 중인 휴대폰 번호입니다.")
    , INVALID_CODE(HttpStatus.UNPROCESSABLE_ENTITY, "인증번호가 올바르지 않습니다.")
    , EXPIRED(HttpStatus.UNPROCESSABLE_ENTITY, "인증번호가 만료되었습니다. 다시 요청해 주세요.")
    , TOO_MANY_ATTEMPTS(HttpStatus.UNPROCESSABLE_ENTITY, "인증 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요.")

    // ===== 비밀번호 변경(010-04) =====
    , INVALID_CURRENT_PASSWORD(HttpStatus.UNPROCESSABLE_ENTITY, "현재 비밀번호가 맞지 않습니다.")
    , PASSWORD_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "비밀번호 규칙을 충족하지 않습니다.")
    , SAME_AS_CURRENT(HttpStatus.UNPROCESSABLE_ENTITY, "새 비밀번호는 현재 비밀번호와 동일할 수 없습니다.")

    // ===== 결재선 프리셋(010-05) =====
    , PRESET_NAME_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "프리셋 이름을 입력해 주세요.")
    , PRESET_NAME_DUPLICATED(HttpStatus.UNPROCESSABLE_ENTITY, "이미 같은 이름의 프리셋이 있습니다.")
    , PRESET_APPROVER_REQUIRED(HttpStatus.UNPROCESSABLE_ENTITY, "결재자를 1명 이상 지정해 주세요.")
    , PRESET_APPROVER_DUPLICATED(HttpStatus.UNPROCESSABLE_ENTITY, "결재자에 동일한 사용자를 중복 지정할 수 없습니다.")
    , PRESET_SELF_APPROVAL(HttpStatus.UNPROCESSABLE_ENTITY, "본인을 결재자로 지정할 수 없습니다.")
    , PRESET_APPROVER_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "결재자 후보에 없는 사용자가 포함되어 있습니다.")
    , PRESET_NOT_FOUND(HttpStatus.NOT_FOUND, "프리셋을 찾을 수 없습니다.")
    , PRESET_FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    MypageErrorCode(HttpStatus httpStatus, String message) {
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
