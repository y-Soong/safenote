package com.prafta.common.error.acct;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 사고관리 도메인 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 */
public enum AcctErrorCode implements ApiErrorCode {

    // 400: 필수 입력값 누락(발생일/시각/재해자/등급/경위 등)
    ACCT_400_001(HttpStatus.BAD_REQUEST, "필수 입력값이 누락되었습니다.\n확인 후 다시 시도해주세요.")
    // 400: 선택한 재해자가 해당 사업장에 실재하지 않음(유령/타 사업장 참조 차단)
    , ACCT_400_002(HttpStatus.BAD_REQUEST, "선택한 재해자가 해당 사업장에 존재하지 않습니다.")
    // 404: 대상 사고가 존재하지 않거나 사업장 스코프 밖
    , ACCT_404_001(HttpStatus.NOT_FOUND, "대상 사고를 찾을 수 없습니다.")
    // 403: 해당 사업장에 대한 접근 권한 없음 (cross-site IDOR 차단)
    , ACCT_403_001(HttpStatus.FORBIDDEN, "해당 사업장에 대한 접근 권한이 없습니다.")
    // 403: 해당 작업에 필요한 역할 권한 없음 (BTN_NEW/SAVE/DELT 서버 강제)
    , ACCT_403_002(HttpStatus.FORBIDDEN, "해당 작업을 수행할 권한이 없습니다.")

    // ── prafta-065 재해자 다중 등록 ──
    // 400: 재해자 하한(D5) — 등록 victimList 비어 있음 / 제외 시 잔여 1명
    , ACCT_400_003(HttpStatus.BAD_REQUEST, "재해자는 최소 1명 이상이어야 합니다.")
    // 400: 배열 내 (userTypeCd,userCd) 중복 / 추가 시 UNIQUE 위반 번역
    , ACCT_400_004(HttpStatus.BAD_REQUEST, "동일한 재해자가 중복 선택되었습니다.")
    // 400: SYS084 미실재 또는 USE_YN 이 'Y' 아님
    , ACCT_400_005(HttpStatus.BAD_REQUEST, "재해 결과 코드가 유효하지 않습니다.")
    // 400: careDays/restDays 범위(null 허용, 0 이상 3650 이하)
    , ACCT_400_006(HttpStatus.BAD_REQUEST, "요양·휴업 일수는 0 이상 3650 이하의 정수여야 합니다.")
    // 400: 등록 배열 50 초과 / 추가 시 현재 인원 50 이상
    , ACCT_400_007(HttpStatus.BAD_REQUEST, "재해자는 최대 50명까지 등록할 수 있습니다.")
    // 400: 부상 부위(100자)·부상 내용(500자) 길이 초과 — DB varchar 한도를 서버가 선검증(security L-1)
    , ACCT_400_008(HttpStatus.BAD_REQUEST, "부상 부위는 100자, 부상 내용은 500자 이내여야 합니다.")
    // 404: victimSeq 가 해당 사고에 실재하지 않음(수정/제외/연계 조회)
    , ACCT_404_002(HttpStatus.NOT_FOUND, "대상 재해자를 찾을 수 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    AcctErrorCode(HttpStatus httpStatus, String message) {
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
