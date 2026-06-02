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

    // ===== PRAFTA-036 - 신규 계정 생성 (단건/엑셀 업로드 공통, 사용자 친화 메시지) =====
    // 표준 한글 사유: D6 (BatchResultPop 표시용) 일관성 유지.
    , USER_400_040(HttpStatus.BAD_REQUEST, "필수값누락")
    , USER_400_041(HttpStatus.BAD_REQUEST, "사용자ID중복")
    , USER_400_042(HttpStatus.BAD_REQUEST, "휴대폰번호중복")
    , USER_400_043(HttpStatus.BAD_REQUEST, "사업장번호없음")
    , USER_400_044(HttpStatus.BAD_REQUEST, "부서코드없음")
    , USER_400_045(HttpStatus.BAD_REQUEST, "권한코드없음")
    , USER_400_046(HttpStatus.BAD_REQUEST, "권한레벨초과")
    , USER_400_047(HttpStatus.BAD_REQUEST, "고용형태오류")
    , USER_400_048(HttpStatus.BAD_REQUEST, "사유유형오류")
    , USER_400_049(HttpStatus.BAD_REQUEST, "휴대폰번호형식오류")

    // ===== PRAFTA-036 - 엑셀 업로드 검증 =====
    , USER_400_050(HttpStatus.BAD_REQUEST, "엑셀(.xlsx) 형식의 파일만 업로드할 수 있습니다.")
    , USER_400_051(HttpStatus.BAD_REQUEST, "엑셀 파일 크기는 5MB 이하여야 합니다.")
    , USER_400_052(HttpStatus.BAD_REQUEST, "엑셀 데이터 행 수는 1000행 이하여야 합니다.")
    , USER_400_053(HttpStatus.BAD_REQUEST, "엑셀 파일을 읽을 수 없습니다. 양식을 확인해 주세요.")

    // ===== PRAFTA-037-F1 - 첫 로그인 강제 비밀번호 변경 =====
    // plan §1 D-2: 동일 PW 거부 (현재 PW = 신규 PW). 자발/강제 변경 공통 적용.
    // USER_400_010 가 prafta-020 결재라인 프리셋에서 이미 점유되어 본 코드는 054 로 시프트.
    , USER_400_054(HttpStatus.BAD_REQUEST, "새 비밀번호는 현재 비밀번호와 동일할 수 없습니다.")

    // ===== PRAFTA-037-F7 - 단건 생성 시 추가 사이트 권한 =====
    // 추가 사이트 코드가 회사 내 존재하지 않을 때.
    , USER_400_055(HttpStatus.BAD_REQUEST, "사이트코드없음")

    // ===== PRAFTA-037-F6 - 비동기 업로드 잡 =====
    // 잡 없음 또는 본인 잡 아님 (둘을 같은 메시지로 통합 — 회사/존재 노출 차단).
    , USER_404_002(HttpStatus.NOT_FOUND, "업로드 작업을 찾을 수 없습니다.")
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