package com.prafta.common.error.user;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

public enum UserErrorCode implements ApiErrorCode {

    USER_400_001(HttpStatus.BAD_REQUEST, "부서의 관리자는 소속부서를 변경할 수 없습니다.\n부서 관리자 직위 해제 후 다시 시도해주세요.")
    , USER_400_002(HttpStatus.BAD_REQUEST, "부서의 관리자는 미사용 처리할 수 없습니다.\n부서 관리자 직위 해제 후 다시 시도해주세요.")
    , USER_400_003(HttpStatus.BAD_REQUEST, "현재 비밀번호가 맞지 않습니다.")
    , USER_400_004(HttpStatus.BAD_REQUEST, "비밀번호는 숫자, 영문자, 특수문자 중 2가지\n이상을 포함하여 6~15자로 작성해 주세요.")
    , USER_400_005(HttpStatus.BAD_REQUEST, "부서의 관리자는 탈퇴 처리할 수 없습니다.\n부서 관리자 직위 해제 후 다시 시도해주세요.")

    // ===== PRAFTA-017-4 - 입력 검증 (사용자 친화 메시지) =====
    , USER_400_006(HttpStatus.BAD_REQUEST, "변경 사유를 입력해 주세요.")
    , USER_400_007(HttpStatus.BAD_REQUEST, "변경할 입사일을 올바르게 입력해 주세요.")
    , USER_400_008(HttpStatus.BAD_REQUEST, "인정 개월 수는 0 이상으로 입력해 주세요.")
    , USER_400_009(HttpStatus.BAD_REQUEST, "상세 설명은 500자 이내로 입력해 주세요.")

    // ===== PRAFTA-017-4 - 권한 부족 (보안 민감 - 일반 메시지) =====
    // 정책서 §8.5.7 권한 매핑: 입사일 변경/경력 인정/근태·연차 조회 POST·GET은 AUTH_MASTER 또는 AUTH_HR_MANAGER 필요.
    , USER_403_001(HttpStatus.FORBIDDEN, "권한이 없습니다.")

    // ===== PRAFTA-032 - 입사일 변경 수동 연차 조정 (사용자 친화 메시지) =====
    // D3 회수 가능량 초과 차단 메시지는 동적(N일)이라 서비스에서 detailMessage로 대체해 노출한다.
    , USER_400_030(HttpStatus.BAD_REQUEST, "회수 가능한 연차를 초과하여 회수할 수 없습니다.")
    , USER_400_031(HttpStatus.BAD_REQUEST, "회수 사유를 입력해 주세요.")
    , USER_400_032(HttpStatus.BAD_REQUEST, "수정 법정 부여량을 0 이상으로 입력해 주세요.")

    // ===== PRAFTA-020 - 결재라인 프리셋 =====
    , USER_400_010(HttpStatus.BAD_REQUEST, "프리셋 이름을 입력해 주세요.")
    , USER_400_011(HttpStatus.BAD_REQUEST, "결재자를 1명 이상 지정해 주세요.")
    , USER_400_012(HttpStatus.BAD_REQUEST, "결재자에 동일한 사용자를 중복 지정할 수 없습니다.")
    , USER_400_013(HttpStatus.BAD_REQUEST, "결재자 후보에 없는 사용자가 포함되어 있습니다.")
    , USER_404_001(HttpStatus.NOT_FOUND, "프리셋을 찾을 수 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    UserErrorCode(HttpStatus httpStatus, String message) {
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