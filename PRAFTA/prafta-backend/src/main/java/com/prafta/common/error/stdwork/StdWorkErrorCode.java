package com.prafta.common.error.stdwork;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 소정-02: 근로자별 소정근로시간(TB_USER_STD_WORK_HOURS) 전용 에러코드.
 *
 * <p>지시서 {@code 작업지시서_근로자별-소정근로시간-관리-도입.md} §0단계 / plan §1.1 의
 * 등록·변경 검증 규칙에 대응한다.
 *
 * <p>주의: 주 15시간 미만·육아기 범위 밖은 <b>경고</b>이지 차단이 아니다(plan §8 Q4 확정).
 * 경고는 예외가 아니라 {@code StdWorkHoursSaveResult.warnings} 로 반환한다.
 */
public enum StdWorkErrorCode implements ApiErrorCode {

    // 규칙: {MODULE}_{HTTP}_{SEQ}
    STDWORK_400_001(HttpStatus.BAD_REQUEST, "소정근로시간 등록에 필요한 정보가 누락되었습니다.")
    , STDWORK_400_002(HttpStatus.BAD_REQUEST, "적용 기간 형식이 올바르지 않습니다. (YYYYMMDD)")
    , STDWORK_400_003(HttpStatus.BAD_REQUEST, "적용 종료일은 적용 시작일보다 빠를 수 없습니다.")
    , STDWORK_400_004(HttpStatus.BAD_REQUEST, "주 소정근로시간 값이 올바르지 않습니다.")
    , STDWORK_400_005(HttpStatus.BAD_REQUEST, "사용할 수 없는 소정근로시간 사유코드입니다.")
    , STDWORK_400_006(HttpStatus.BAD_REQUEST, "단축 근무 사유는 적용 종료일이 반드시 필요합니다.")
    // 통상근로시간 기준값(회사/사업장) 전용 — 근로기준법 제50조(1주 40시간) 상한.
    //   주 44시간 사업장 = 소정 40h + 연장 4h 이며, 연장분은 고정연장근무(근무타입)로 잡는다.
    , STDWORK_400_007(HttpStatus.BAD_REQUEST,
            "통상근로시간은 주 40시간(2400분)을 초과할 수 없습니다.\n근로기준법 제50조상 1주 소정근로시간의 법정 상한입니다.\n40시간을 넘는 근무는 연장근로이므로 고정연장근무 근무타입으로 등록해 주세요.")
    , STDWORK_403_001(HttpStatus.FORBIDDEN, "일용직 회원은 소정근로시간을 등록할 수 없습니다.")
    , STDWORK_404_001(HttpStatus.NOT_FOUND, "대상 사용자를 찾을 수 없습니다.")
    , STDWORK_404_002(HttpStatus.NOT_FOUND, "정정할 소정근로시간 이력이 없습니다.")
    , STDWORK_409_001(HttpStatus.CONFLICT, "같은 적용 시작일의 소정근로시간 이력이 이미 있습니다.")
    , STDWORK_409_002(HttpStatus.CONFLICT, "기존 소정근로시간 이력과 적용 기간이 겹칩니다.")
    , STDWORK_409_003(HttpStatus.CONFLICT, "바로 뒤에 단축 근무 이력이 있어 종료일을 정정할 수 없습니다.\n단축 근무 이력을 먼저 정정해 주세요.")
    , STDWORK_409_004(HttpStatus.CONFLICT, "종료일을 이만큼 미루면 뒤따르는 소정근로시간 이력이 사라집니다.\n후속 이력을 먼저 정리해 주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    StdWorkErrorCode(HttpStatus httpStatus, String message) {
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
