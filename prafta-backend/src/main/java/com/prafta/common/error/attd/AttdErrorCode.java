package com.prafta.common.error.attd;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * attd(근태) 도메인 에러 카탈로그.
 *
 * 명명 규칙: {MODULE}_{HTTP}_{SEQ}.
 *
 * 사용자 노출 메시지 정책:
 * <ul>
 *   <li>입력 오류 / 비즈니스 룰 위반 등 사용자가 인지하고 조치할 수 있는 항목 →
 *       구체적인 한글 메시지를 노출하여 무엇이 문제인지 알린다.</li>
 *   <li>보안 민감 항목(권한 부족, 변조 탐지, scope 위반, 타입 혼동 등) →
 *       내부 사정을 노출하면 정보 누출이 되므로 일반화된 "요청을 처리할 수 없습니다."
 *       또는 "권한이 없습니다."로만 노출한다. 상세한 분기 사유는 서버 로그에만 기록한다.</li>
 * </ul>
 */
public enum AttdErrorCode implements ApiErrorCode {

    // ===== 관리 코드 중복 등록 (관리자 화면용 - 노출 OK) =====
      ATTD_400_001(HttpStatus.BAD_REQUEST, "이미 등록된 근태 코드입니다.")
    , ATTD_400_002(HttpStatus.BAD_REQUEST, "이미 등록된 초과근무 코드입니다.")
    , ATTD_400_003(HttpStatus.BAD_REQUEST, "이미 등록된 시간 코드입니다.")
    , ATTD_400_004(HttpStatus.BAD_REQUEST, "조회 기간은 3개월을 초과할 수 없습니다.")

    // 보안 민감 - 변조 탐지 / 타입 혼동: 일반 메시지만 노출 (서버 로그에 상세 기록)
    , ATTD_400_005(HttpStatus.BAD_REQUEST, "요청을 처리할 수 없습니다.")
    , ATTD_400_006(HttpStatus.BAD_REQUEST, "요청을 처리할 수 없습니다.")

    // ===== PRAFTA-003 - 초과근무 등록 (사용자 친화 메시지) =====
    , ATTD_400_010(HttpStatus.BAD_REQUEST, "초과근무 정보가 없습니다. 등록할 시간을 입력해 주세요.")
    , ATTD_400_011(HttpStatus.BAD_REQUEST, "초과근무 시간 범위가 올바르지 않습니다. 종료 시각이 시작 시각보다 뒤여야 합니다.")
    , ATTD_400_012(HttpStatus.BAD_REQUEST, "초과근무 시간이 스케줄 외 허용 범위를 벗어났습니다. 정규 근무 시간 외의 시간으로 입력해 주세요.")
    , ATTD_400_013(HttpStatus.BAD_REQUEST, "등록하려는 초과근무 시간이 서로 겹칩니다.\n시간이 겹치지 않도록 입력해 주세요.")
    , ATTD_400_014(HttpStatus.BAD_REQUEST, "출퇴근 기록이 완료된 후에 초과근무를 등록할 수 있습니다.")

    // ===== 권한 / 보안 (정보 누출 방지를 위해 일반 메시지) =====
    , ATTD_403_001(HttpStatus.FORBIDDEN, "본인의 근태 요청은 직접 승인할 수 없습니다.")
    , ATTD_403_002(HttpStatus.FORBIDDEN, "권한이 없습니다.")
    , ATTD_403_003(HttpStatus.FORBIDDEN, "본인의 초과근무는 직접 등록할 수 없습니다.")

    // ===== Not Found =====
    // 사용자가 일반적으로 알 수 있는 항목은 친화적 메시지 노출
    , ATTD_404_001(HttpStatus.NOT_FOUND, "해당 근태 요청을 찾을 수 없습니다.")
    , ATTD_404_010(HttpStatus.NOT_FOUND, "해당 근무일의 스케줄 정보를 찾을 수 없습니다.")
    // 보안 민감 - 사용자/ATTD scope 위반: scope 외 데이터 존재 여부를 노출하지 않음
    , ATTD_404_011(HttpStatus.NOT_FOUND, "요청을 처리할 수 없습니다.")
    , ATTD_404_012(HttpStatus.NOT_FOUND, "요청을 처리할 수 없습니다.")

    // ===== Conflict =====
    , ATTD_409_001(HttpStatus.CONFLICT, "이미 처리된 근태 요청입니다.")
    // PRAFTA-009-001 - 초과근무 등록 시 기존 행과 시간대가 겹치는 경우 (사용자 친화 메시지)
    , ATTD_409_002(HttpStatus.CONFLICT, "이미 등록된 시간대의 초과근무가 존재합니다. 시간이 겹치지 않도록 입력해 주세요.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    AttdErrorCode(HttpStatus httpStatus, String message) {
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
