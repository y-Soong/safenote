package com.prafta.common.error.subcon;

import org.springframework.http.HttpStatus;

import com.prafta.common.error.ApiErrorCode;

/**
 * 회사 간 연동(SUBCON) 도메인 에러코드.
 *
 * <p>존재/비존재를 응답으로 구분하지 않는다(열거·IDOR 방지 — PRAFTA-SUBCON-T1 §6/§7):
 * 미존재/타사 관계/이미 처리 상태는 전부 SUBCON_404_001 로 통합한다.
 */
public enum SubconErrorCode implements ApiErrorCode {

    // ===== PRAFTA-SUBCON-T1 - 회사 간 연동 관계 수립 =====
    // 필수값 누락/형식 오류(관계ID 누락 등 — 사용자 친화 메시지).
    SUBCON_400_001(HttpStatus.BAD_REQUEST, "요청 정보가 올바르지 않습니다.")
    // 자기 회사 대상 연동 요청 차단(프론트 선안내 + 서버 이중 가드).
    , SUBCON_400_002(HttpStatus.BAD_REQUEST, "자기 회사에는 연동을 요청할 수 없습니다.")
    // 거부 사유 필수(plan §7-1 #3).
    , SUBCON_400_003(HttpStatus.BAD_REQUEST, "거부 사유를 입력해 주세요.")
    // 거부 사유 길이 제한(DDL varchar(500) — truncation/500 방지).
    , SUBCON_400_004(HttpStatus.BAD_REQUEST, "사유는 500자 이하여야 합니다.")
    // 메뉴 버튼 권한 게이트 차단(보안 민감 — 일반 메시지).
    , SUBCON_403_001(HttpStatus.FORBIDDEN, "권한이 없습니다.")
    // 관계 미존재/타사 관계/이미 처리 상태 통합(존재 비노출 — 사유 무구분).
    , SUBCON_404_001(HttpStatus.NOT_FOUND, "처리할 연동 관계를 찾을 수 없습니다.")
    // 요청 생성 시 대상 회사 유효성 재검증 실패(미존재/비활성 무구분 — 열거 방지).
    , SUBCON_404_002(HttpStatus.NOT_FOUND, "연동 요청 대상 회사를 찾을 수 없습니다.")
    // 활성 관계 중복(방향 불문 쌍당 1건 — 서비스 가드 + DB UNIQUE 백스톱 공통 메시지).
    , SUBCON_409_001(HttpStatus.CONFLICT, "이미 진행 중이거나 연동된 회사입니다.")

    // ===== PRAFTA-SUBCON-T2 - 사업장/근무타입 미러 연동 =====
    // 미러 사업장 기본정보 수정 잠금(SITE_ADMIN_CD 만 수신사 수정 허용 — plan §5-5).
    , SUBCON_403_002(HttpStatus.FORBIDDEN, "연동(읽기전용) 사업장입니다. 원본 제공 회사만 수정할 수 있습니다.")
    // 미러 사업장 근무타입 수정 잠금(신규 생성 포함 전면 거부 — plan §5-5).
    , SUBCON_403_003(HttpStatus.FORBIDDEN, "연동 사업장의 근무타입은 제공 회사에서 관리합니다.")
    // 링크 미존재/비당사자/기처리 통합(조건부 UPDATE 0행 — 존재 비노출).
    , SUBCON_404_003(HttpStatus.NOT_FOUND, "처리할 사업장 연동을 찾을 수 없습니다.")
    // 제안 대상/연동 원본 사업장 미존재(소유 검증 실패 무구분 — 열거 방지).
    , SUBCON_404_004(HttpStatus.NOT_FOUND, "연동 대상 사업장을 찾을 수 없습니다.")
    // 활성 링크 중복(같은 제공 사업장 → 같은 수신 회사 — 서비스 가드 + UX_SITE_LINK_ACTIVE 백스톱).
    , SUBCON_409_002(HttpStatus.CONFLICT, "이미 제안 중이거나 연동된 사업장입니다.")
    // 루프 차단(제안 대상이 연동 출처 체인의 조상 회사 — 마스터 §1-3 ①).
    , SUBCON_409_003(HttpStatus.CONFLICT, "연동 출처 회사에는 재연동할 수 없습니다.")
    // 관계 미수립(tb_cmpny_relation ACCEPTED 부재).
    , SUBCON_409_004(HttpStatus.CONFLICT, "연동 관계가 수립된 회사가 아닙니다.")
    // 전파/조상 순회 깊이 초과(안전핀 20단 — 데이터 오염 감지, 로그 필수).
    , SUBCON_500_001(HttpStatus.INTERNAL_SERVER_ERROR, "연동 체인 처리 중 오류가 발생했습니다. 관리자에게 문의해 주세요.")

    // ===== PRAFTA-SUBCON-T3 - 데이터 공유(스냅샷/릴레이) =====
    // 지원하지 않는 데이터 유형(화이트리스트 = ATTD 만 — RISK/NEARMISS 는 T7).
    , SUBCON_400_005(HttpStatus.BAD_REQUEST, "지원하지 않는 데이터 유형입니다.")
    // 요청 기간 오류(형식/순서/미래/12개월 초과 무구분).
    , SUBCON_400_006(HttpStatus.BAD_REQUEST, "요청 기간이 올바르지 않습니다.")
    // 제공 목적 필수/길이 초과(≤500자 — 개보법 근거 문서화).
    , SUBCON_400_007(HttpStatus.BAD_REQUEST, "제공 목적을 입력해 주세요.(500자 이하)")
    // 공유요청 미존재/비당사자/기처리 통합(조건부 UPDATE 0행 — 존재 비노출, IDOR 열거 방지).
    , SUBCON_404_005(HttpStatus.NOT_FOUND, "처리할 공유 요청을 찾을 수 없습니다.")
    // 동일 조건 REQUESTED 중복(연타/중복 요청 — 처리 대기분은 1건만).
    , SUBCON_409_005(HttpStatus.CONFLICT, "이미 처리 대기 중인 요청입니다.")
    // 사업장 체인 부재(요청자 사업장과 제공사 사이에 ACTIVE 사업장 연동이 없음).
    , SUBCON_409_006(HttpStatus.CONFLICT, "연동된 사업장이 아닙니다.")
    // [미사용 — 부분 공유 전환(2026-07-22)] 구 마감 미완료 차단(CLOSED_ONLY_YN='Y' 요청 시 승인 거부).
    //   "마감분만" 옵션이 차단 게이트에서 부분 포함 필터로 재정의(D-1/D-2)되어 더 이상 던지지 않는다.
    //   enum 은 프론트 에러 메시지 매핑 회귀 방지를 위해 유지한다.
    , SUBCON_409_007(HttpStatus.CONFLICT, "근태 마감이 완료되지 않은 기간입니다.")
    // 릴레이 후보 부적격(서버 재검증 실패 — 소유/체인/기간/미마감 4조건 미충족).
    //   [D-3 재정의] ④ 는 "미마감 포함(UNCLOSED_INCLUDED_YN='Y')" 여부만 검사한다 — 마감분만 필터로
    //   만든 부분 포함 스냅샷(CLOSED_PARTIAL_YN='Y')은 미마감 포함이 아니므로 적격이며, 부분 포함
    //   여부는 표식 병합으로 상위에 전파된다.
    , SUBCON_409_008(HttpStatus.CONFLICT, "함께 제공할 수 없는 자료가 포함되어 있습니다.")
    // 제3자 제공 동의 약관(006)이 배포되어 있으나 비활성(USE_YN='N') — 동의 필터를 적용할 수 없으므로
    // 승인을 차단한다(fail-closed). 전원 포함으로 진행하면 명시적 미동의자의 PII 가 반출된다(security M-1).
    , SUBCON_409_009(HttpStatus.CONFLICT, "제3자 제공 동의 약관이 비활성 상태입니다. 관리자에게 문의해 주세요.")

    // ===== PRAFTA-SUBCON-T6 - 순회점검 구성 연동 + 점검 결과 통합 =====
    // 미러 점검대상 수정 잠금(잠금 예외 = 점검 담당자 MGMT_USER_CD 지정만 — plan §5-2).
    , SUBCON_403_004(HttpStatus.FORBIDDEN, "연동(읽기전용) 점검대상입니다. 담당자 지정만 변경할 수 있습니다.")
    // 미러 점검문항 수정 잠금(전면 — 운영 예외 없음).
    , SUBCON_403_005(HttpStatus.FORBIDDEN, "연동 사업장의 점검문항은 제공 회사에서 관리합니다.")
    // 점검 연동 링크 미존재/비당사자/기처리 통합(조건부 UPDATE 0행 — 존재 비노출, IDOR 열거 방지).
    , SUBCON_404_006(HttpStatus.NOT_FOUND, "처리할 점검 연동을 찾을 수 없습니다.")
    // 불량조치 선처리 충돌(최초 조치자만 수정 가능 — 서비스가 조치 회사명을 동적 메시지로 덮어쓴다).
    , SUBCON_409_010(HttpStatus.CONFLICT, "이미 조치되었습니다.")

    // ===== PRAFTA-SUBCON-T7 - 스냅샷 유형 확장(위험성평가/아차사고) =====
    // 수신 스냅샷 첨부 미존재/비소유(소유·참조 검증 실패 통합 — IDOR 열거 방지).
    , SUBCON_404_007(HttpStatus.NOT_FOUND, "요청한 첨부를 찾을 수 없습니다.")

    // ===== SHIFT-LINK - 교대근무 타입 사업장 연동(복제·전파) =====
    // 미러 사업장 교대근무 타입 정의 잠금(신규 생성 전면 거부 — 정의 4테이블은 원본 소유사의 전파로만 갱신.
    // 팀 구성·인원 배속(Attd06)은 수신사 자율이라 잠그지 않는다 — 지시서 §2.1-1).
    , SUBCON_403_006(HttpStatus.FORBIDDEN, "연동 사업장의 교대근무 타입은 제공 회사에서 관리합니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;

    SubconErrorCode(HttpStatus httpStatus, String message) {
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
