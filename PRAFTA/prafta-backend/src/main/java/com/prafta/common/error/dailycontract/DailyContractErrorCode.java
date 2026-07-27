package com.prafta.common.error.dailycontract;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 일용직 근로계약서(dailycontract) 도메인 전용 에러코드.
 * 규칙: {MODULE}_{HTTP}_{SEQ}
 *
 * <p>출처: 일용직 계약서+승인제 plan §3-5. 계약서 관리(웹 User_07)·서명 게이트/저장(앱)·
 * 서명본 열람(앱/웹 User_08) API 에서 사용한다.
 */
public enum DailyContractErrorCode implements ApiErrorCode {

    // 서명 이미지 multipart 검증 실패(비이미지/크기 초과/디코딩 실패).
    DAILYCONTRACT_400_001(HttpStatus.BAD_REQUEST, "서명 이미지가 올바르지 않습니다.")
    // 현재 승인 사이클에 이미 서명 완료(멱등 가드 — append-only 중복 방지).
    , DAILYCONTRACT_400_002(HttpStatus.BAD_REQUEST, "이미 서명이 완료되었습니다.")
    // 해당 사업장에 활성 계약서 없음(서명/열람/중지 대상 부재).
    , DAILYCONTRACT_400_003(HttpStatus.BAD_REQUEST, "등록된 계약서가 없습니다.")
    // 관리자 계약서 업로드 검증 실패(이미지 형식/10MB 초과).
    , DAILYCONTRACT_400_004(HttpStatus.BAD_REQUEST, "계약서 파일이 올바르지 않습니다. (이미지 파일, 10MB 이하)")
    // 서명본 열람 인가 실패(본인 아님 + 해당 사업장 관리자 아님 — IDOR 차단).
    , DAILYCONTRACT_403_001(HttpStatus.FORBIDDEN, "열람 권한이 없습니다.")
    // 서명본 미존재(타 회사 서명 포함 — 존재 비노출).
    , DAILYCONTRACT_404_001(HttpStatus.NOT_FOUND, "서명본을 찾을 수 없습니다.")
    // 계약서 행은 존재하는데 디스크 원본 파일이 없음(DB 만 이관되고 업로드 파일은 미이관된 경우 등).
    //   400_003("등록된 계약서가 없습니다")로 뭉뚱그리면 운영자가 "미등록"으로 오판하므로 별도 코드로 구분한다.
    , DAILYCONTRACT_404_002(HttpStatus.NOT_FOUND, "계약서 원본 파일을 찾을 수 없습니다. 계약서를 다시 등록해 주세요.")
    // 계약서 동시 등록 경합(최초 등록 시 버전 채번 무잠금 — PK/기능성 유니크 충돌을 재시도 안내로 매핑, qa L-1).
    , DAILYCONTRACT_409_001(HttpStatus.CONFLICT, "동시 등록이 감지되었습니다. 잠시 후 다시 시도해 주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    DailyContractErrorCode(HttpStatus httpStatus, String message) {
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
