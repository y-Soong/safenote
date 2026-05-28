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

    // ===== PRAFTA-017 - 연차 타입 자동부여 규칙 검증 (사용자 친화 메시지) =====
    , ATTD_400_015(HttpStatus.BAD_REQUEST, "자동부여 규칙이 올바르지 않습니다. 기준일에 맞는 값을 입력해 주세요.")

    // ===== PRAFTA-018 - 법정 연차 정책(7개 axis) 검증 (사용자 친화 메시지) =====
    // 정책서 §8.5.3 Cross-axis 활성 매트릭스 위반.
    // 어느 조합이 위반되었는지는 서버 로그에 기록하고, 사용자에게는 일반화된 메시지만 노출한다.
    , ATTD_400_020(HttpStatus.BAD_REQUEST, "정책 axis 조합이 유효하지 않습니다.")

    // ===== PRAFTA-018 - 정책 변경 영향 분석 (화면 8) =====
    // 현재 정책과 변경할 정책이 동일하여 분석할 변경 사항이 없는 경우 (§9.10-4 / TC-01).
    , ATTD_400_021(HttpStatus.BAD_REQUEST, "변경 사항이 없습니다. 연차 정책 화면에서 정책을 변경한 뒤 다시 시도해 주세요.")

    // ===== PRAFTA-017-2 - 연차 수동 부여 입력 검증 (화면 attd09, 사용자 친화 메시지) =====
    , ATTD_400_030(HttpStatus.BAD_REQUEST, "부여 유형을 선택해 주세요.")
    , ATTD_400_031(HttpStatus.BAD_REQUEST, "부여 일수는 0보다 큰 0.5일 단위로 입력해 주세요.")
    , ATTD_400_032(HttpStatus.BAD_REQUEST, "사용 가능일을 올바르게 입력해 주세요.")
    , ATTD_400_033(HttpStatus.BAD_REQUEST, "부여 대상 직원을 선택해 주세요.")
    , ATTD_400_034(HttpStatus.BAD_REQUEST, "부여 사유는 500자 이내로 입력해 주세요.")
    , ATTD_400_035(HttpStatus.BAD_REQUEST, "부여 일수가 허용 범위를 초과했습니다. 365일 이내로 입력해 주세요.")

    // ===== PRAFTA-019-C - 근태 마감 (정책서 §13.3 차단 조건) =====
    , ATTD_400_040(HttpStatus.BAD_REQUEST, "미결 항목이 남아 있어 근태 마감을 할 수 없습니다. 미결 요청·GPS 미확인·미승인 추가근무를 먼저 처리해 주세요.")
    , ATTD_400_041(HttpStatus.BAD_REQUEST, "마감되지 않은 기간은 마감 해제할 수 없습니다.")
    // PRAFTA-028 - 마감된 기간(부서)의 데이터 변경/요청 차단
    , ATTD_400_042(HttpStatus.BAD_REQUEST, "마감된 기간의 데이터는 수정할 수 없습니다. 마감 해제 후 다시 시도해 주세요.")

    // ===== 권한 / 보안 (정보 누출 방지를 위해 일반 메시지) =====
    , ATTD_403_001(HttpStatus.FORBIDDEN, "본인의 근태 요청은 직접 승인할 수 없습니다.")
    , ATTD_403_002(HttpStatus.FORBIDDEN, "권한이 없습니다.")
    , ATTD_403_003(HttpStatus.FORBIDDEN, "본인의 초과근무는 직접 등록할 수 없습니다.")

    // ===== PRAFTA-018 - 시스템 시드 연차 타입 readonly (사용자 친화 메시지) =====
    // tb_leave_type_mgmt.SYSTEM_YN='Y' 행은 attd03 화면에서 편집/삭제 불가 (정책서 §8.5.5)
    , ATTD_403_010(HttpStatus.FORBIDDEN, "시스템 시드 휴가 유형은 수정/삭제할 수 없습니다.")

    // ===== PRAFTA-018 - 정책 변경 권한 부족 (보안 민감 - 일반 메시지) =====
    // 정책서 §8.5.7 권한 매핑: 정책 변경 POST는 AUTH_MASTER 또는 AUTH_HR_MANAGER 필요.
    , ATTD_403_011(HttpStatus.FORBIDDEN, "권한이 없습니다.")

    // ===== PRAFTA-017-2 - 연차 수동 부여 권한 부족 (보안 민감 - 일반 메시지) =====
    // 정책서 §8.5.7: 수동 부여 POST는 AUTH_MASTER 또는 AUTH_HR_MANAGER 필요.
    , ATTD_403_020(HttpStatus.FORBIDDEN, "권한이 없습니다.")

    // ===== Not Found =====
    // 사용자가 일반적으로 알 수 있는 항목은 친화적 메시지 노출
    , ATTD_404_001(HttpStatus.NOT_FOUND, "해당 근태 요청을 찾을 수 없습니다.")
    , ATTD_404_010(HttpStatus.NOT_FOUND, "해당 근무일의 스케줄 정보를 찾을 수 없습니다.")
    // 보안 민감 - 사용자/ATTD scope 위반: scope 외 데이터 존재 여부를 노출하지 않음
    , ATTD_404_011(HttpStatus.NOT_FOUND, "요청을 처리할 수 없습니다.")
    , ATTD_404_012(HttpStatus.NOT_FOUND, "요청을 처리할 수 없습니다.")
    // PRAFTA-017-2 - 연차 상세/수동부여 대상 직원이 스코프 밖이거나 없는 경우 (보안 민감 - 일반 메시지)
    , ATTD_404_020(HttpStatus.NOT_FOUND, "요청을 처리할 수 없습니다.")

    // ===== Conflict =====
    , ATTD_409_001(HttpStatus.CONFLICT, "이미 처리된 근태 요청입니다.")
    // PRAFTA-009-001 - 초과근무 등록 시 기존 행과 시간대가 겹치는 경우 (사용자 친화 메시지)
    , ATTD_409_002(HttpStatus.CONFLICT, "이미 등록된 시간대의 초과근무가 존재합니다. 시간이 겹치지 않도록 입력해 주세요.")

    // ===== PRAFTA-018 - 정책 동시 변경 충돌 (사용자 친화 메시지) =====
    // SELECT ... FOR UPDATE 락 획득 실패 또는 락 대기 시간 초과 시 재시도를 유도한다.
    , ATTD_409_010(HttpStatus.CONFLICT, "정책 변경이 동시에 진행 중입니다. 잠시 후 다시 시도하세요.")

    // ===== PRAFTA-019-C - 근태 마감 충돌 =====
    , ATTD_409_020(HttpStatus.CONFLICT, "이미 마감된 기간입니다.")

    // ===== PRAFTA-019-E - 연차 신청·결재 흐름 =====
    , ATTD_400_050(HttpStatus.BAD_REQUEST, "마감된 기간에는 연차를 신청할 수 없습니다. 근태 보정 요청을 이용해 주세요.")
    , ATTD_400_051(HttpStatus.BAD_REQUEST, "잔여 연차가 부족합니다.")
    , ATTD_400_052(HttpStatus.BAD_REQUEST, "신청 시각이 근무 스케줄 범위를 벗어났거나 스케줄이 없습니다.")
    , ATTD_400_053(HttpStatus.BAD_REQUEST, "이전 결재 단계가 아직 완료되지 않았습니다.")
    , ATTD_400_054(HttpStatus.BAD_REQUEST, "허용되지 않은 연차 사용 단위입니다.")
    , ATTD_400_055(HttpStatus.BAD_REQUEST, "신청 시간대가 휴게시간을 가로지를 수 없습니다. 휴게 전/후 시간대로 신청해 주세요.")
    , ATTD_400_056(HttpStatus.BAD_REQUEST, "본인을 결재자로 지정할 수 없습니다. (자체근태승인 미설정 부서)")
    , ATTD_400_057(HttpStatus.BAD_REQUEST, "반려 사유를 입력해 주세요.")
    , ATTD_400_058(HttpStatus.BAD_REQUEST, "입사일이 입력되지 않은 직원이 있습니다.")
    , ATTD_400_059(HttpStatus.BAD_REQUEST, "부여에 필요한 시스템 연차 종류가 설정되지 않았습니다.")
    , ATTD_400_060(HttpStatus.BAD_REQUEST, "한 번에 부여할 수 있는 인원 수를 초과했습니다.")
    , ATTD_403_030(HttpStatus.FORBIDDEN, "해당 결재 단계의 결재자가 아닙니다.")
    , ATTD_404_030(HttpStatus.NOT_FOUND, "해당 연차 요청 또는 결재 단계를 찾을 수 없습니다.")

    // ===== PRAFTA-031 - 관리자 수동 부여 연차 회수(soft cancel) (화면 attd09) =====
    // 정책서 §8.5.7(권한) / §8.5.8(소프트 취소·사용이력 불변).
    // 입력/비즈니스 룰 위반은 사용자 친화 메시지, 스코프/경합은 일반화 메시지.
    , ATTD_400_070(HttpStatus.BAD_REQUEST, "회수 사유를 입력해 주세요.")
    , ATTD_400_071(HttpStatus.BAD_REQUEST, "수동 부여된 연차만 회수할 수 있습니다.")
    , ATTD_400_072(HttpStatus.BAD_REQUEST, "이미 사용된 연차는 회수할 수 없습니다.")
    // 회수 대상이 스코프 밖이거나 존재하지 않는 경우 (보안 민감 - 일반 메시지)
    , ATTD_404_070(HttpStatus.NOT_FOUND, "요청을 처리할 수 없습니다.")
    // 이미 취소/만료/소진되어 ACTIVE 상태가 아닌 경우
    , ATTD_409_070(HttpStatus.CONFLICT, "이미 취소·만료·소진된 연차는 회수할 수 없습니다.")
    // 동시 처리로 회수 대상 행 상태가 바뀐 경합 (재시도 유도)
    , ATTD_409_071(HttpStatus.CONFLICT, "처리 중 상태가 변경되었습니다. 잠시 후 다시 시도해 주세요.")
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
