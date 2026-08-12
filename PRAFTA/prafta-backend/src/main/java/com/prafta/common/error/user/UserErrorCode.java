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

    // ===== PRAFTA-046 - 노드-관리자 정합성 가드 (BatchResultPop 표시용, D6 한글 사유 일관) =====
    // 관리자(정/부)가 지정되지 않은 노드에 사용자 생성/이동 차단.
    , USER_400_056(HttpStatus.BAD_REQUEST, "관리자미지정부서")

    // ===== PRAFTA-COM-001-T4 (8.4) - 기본 결재라인 프리셋 삭제 차단 (서버 강제, FE 우회 방지) =====
    , USER_400_057(HttpStatus.BAD_REQUEST, "기본 프리셋은 삭제할 수 없습니다.")

    // ===== prafta-daily-blacklist - 일일계정 블랙리스트 관리(User_06) =====
    // 이미 활성 등록된 휴대폰번호 재등록 차단(사전 count + UNIQUE 양쪽 방어).
    , USER_400_058(HttpStatus.BAD_REQUEST, "이미 등록된 휴대폰번호입니다.")
    // 해제 대상 블랙리스트 항목 없음(존재하지 않거나 타 회사 — 회사/존재 노출 차단 메시지).
    , USER_404_003(HttpStatus.NOT_FOUND, "해제할 블랙리스트 항목을 찾을 수 없습니다.")
    // 등록 사유 길이 초과(DDL varchar(200) — 서버측 truncation/500 방지).
    , USER_400_059(HttpStatus.BAD_REQUEST, "등록 사유는 200자 이하여야 합니다.")

    // ===== PRAFTA-WEB_001-1 - 사용자 소속이동 예약(필수값/불가케이스) =====
    // 필수 입력 누락(사용자 친화 메시지).
    , USER_400_060(HttpStatus.BAD_REQUEST, "소속이동 사유를 입력해 주세요.")
    , USER_400_061(HttpStatus.BAD_REQUEST, "소속이동일은 내일 이후로 지정해 주세요.")
    , USER_400_062(HttpStatus.BAD_REQUEST, "이동할 사업장을 선택해 주세요.")
    , USER_400_063(HttpStatus.BAD_REQUEST, "이동할 소속부서를 선택해 주세요.")
    , USER_400_064(HttpStatus.BAD_REQUEST, "정규직은 기본 근무타입을 지정해야 합니다.")
    // 5종 불가케이스(정규직 한정). 관리자에게 사유를 안내.
    , USER_400_065(HttpStatus.BAD_REQUEST, "사업장 관리자는 소속이동할 수 없습니다.\n사업장 관리자 변경 후 다시 시도해 주세요.")
    , USER_400_066(HttpStatus.BAD_REQUEST, "부서의 마지막 담당자는 소속이동할 수 없습니다.\n다른 담당자 지정 후 다시 시도해 주세요.")
    , USER_400_067(HttpStatus.BAD_REQUEST, "순회점검 담당자는 소속이동할 수 없습니다.\n점검 담당자 변경 후 다시 시도해 주세요.")
    , USER_400_068(HttpStatus.BAD_REQUEST, "교대근무 조에 속한 사용자는 소속이동할 수 없습니다.\n교대조 해제 후 다시 시도해 주세요.")
    // (데드 보존) 구 불가⑤ — 시간차 연차 커버리지 판정(개인분모 체제). 당일분모 전환(E1·W8, 2026-08-04 확정)으로
    //   "미래 시간차 존재 = 무조건 차단"(USER_400_073)이 대체 — 커버리지 여부와 무관해져 발생 경로 소멸.
    //   호출부 0건. enum 상수는 보존(외부 참조·로그 추적 안정성). 재사용 금지.
    , USER_400_069(HttpStatus.BAD_REQUEST, "등록된 시간차 연차를 기본 근무타입 시간이 포함하지 못합니다.\n근무타입을 조정해 주세요.")
    // 동일 사용자 활성 예약 중복.
    , USER_400_070(HttpStatus.BAD_REQUEST, "이미 진행 중인 소속이동 예약이 있습니다.")
    // 소속이동 사유 길이 초과(DDL varchar(500) 정합 — 미입력(060)과 구분).
    , USER_400_071(HttpStatus.BAD_REQUEST, "소속이동 사유는 500자 이하여야 합니다.")
    // 소속이동 권한 부족(master/hr 외) — 보안 민감, 일반 메시지.
    , USER_403_002(HttpStatus.FORBIDDEN, "권한이 없습니다.")
    // 대상 사용자 없음(타 회사/미존재 — 존재 비노출 통합 메시지).
    , USER_404_004(HttpStatus.NOT_FOUND, "대상 사용자를 찾을 수 없습니다.")

    // ===== PRAFTA-WEB_002-T1-03 (1.4-2) - 권한 등급 escalation 서버 가드 =====
    // 요청자(viewer) 본인보다 높거나 같은 등급의 권한을 타 계정에 부여 시도 시 차단(권한 상승 방지).
    , USER_403_003(HttpStatus.FORBIDDEN, "본인보다 낮은 등급의 권한만 부여할 수 있습니다.")

    // ===== User_03 사업장 권한 관리 - master/hr 회수 차단 =====
    // master/hr 은 전 사업장 권한 보유가 불변식(SiteAccessService 전사 통과와 정합) — 회수 시도 차단.
    , USER_400_072(HttpStatus.BAD_REQUEST, "마스터/HR 관리자의 사업장 권한은 회수할 수 없습니다.")

    // ===== 연차 시간차 당일분모 전환(E1·W8) — 미래 시간차 보유자 소속이동 차단 =====
    // 시간차 분모(E1) = 당일 배정 스케줄인데, 소속이동 발효는 발효일 이후 구 사업장 근무계획을 전량
    //   삭제(deleteFutureWorkPlansOnSite, WORK_YMD >= 발효일)하므로 미래 시간차의 분모 소스가 소실된다.
    //   발효일 이후 확정 시간차 사용 또는 미결 시간차 신청이 있으면 이동 차단(구 불가⑤ 커버리지 판정 대체 —
    //   UserTransferValidator ⑤). 종일/반차/반반차는 차감량이 스케줄 무관(고정 1.0/0.5/0.25)이라 이동 허용.
    //   탈출구 = 해당 연차 취소·처리 → 이동 → (필요 시) 재신청. 사용자 확정 2026-08-04.
    //   ★ Q2(2026-08-07, 반차 시간대 도입): 반차에 경계 시각이 생기면서 selectFuturePartialLeaves
    //     술어(USE_UNIT_TYPE != '00' + START_TIME 보유)에 반차가 자동 편입된다(차단 로직 변경 0).
    //     문구가 "시간 단위 연차"만 지목하면 반차로 막힌 관리자가 없는 시간차를 찾게 되므로
    //     "반차·시간 단위 연차"로 확장한다(F-7 동일 계열 — 동작은 맞는데 안내가 틀려 오진 유발).
    , USER_400_073(HttpStatus.BAD_REQUEST, "미래 일자에 반차 또는 시간 단위 연차가 신청·사용된 직원은 사업장을 이동할 수 없습니다.\n해당 연차를 먼저 취소·처리한 뒤 이동해 주세요.")

    // ===== 소정-09 - 셀프가입 승인/거부 (User_09) =====
    // 대상이 '06 가입승인대기' 가 아님(이미 처리됨/대상 아님 — 동시 처리 낙관적 차단 포함).
    , USER_400_074(HttpStatus.BAD_REQUEST, "이미 처리되었거나 승인 대기 상태가 아닌 가입 신청입니다.")
    , USER_400_075(HttpStatus.BAD_REQUEST, "거부 사유를 입력해 주세요.")
    , USER_400_076(HttpStatus.BAD_REQUEST, "거부 사유는 200자 이하여야 합니다.")
    , USER_400_077(HttpStatus.BAD_REQUEST, "입사일을 올바르게 입력해 주세요. (YYYYMMDD)")
    , USER_400_078(HttpStatus.BAD_REQUEST, "직급 코드가 올바르지 않습니다.")
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