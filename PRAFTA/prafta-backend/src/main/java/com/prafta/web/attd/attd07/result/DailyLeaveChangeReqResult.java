package com.prafta.web.attd.attd07.result;

/**
 * Result row of {@code Attd07Mapper.selectDailyLeaveChangeReq}.
 *
 * <p>일자 상세 팝업(AttdDayDetailPop)의 "근로자 요청" 영역에 노출되는 연차 변경(이동/삭제) 요청 1건.
 * 출처는 {@code TB_LEAVE_CHANGE_REQUEST}(PRAFTA-COM-008-C) 로, 근태 요청({@code TB_USER_ATTD_REQ})과
 * 별개 테이블이다. 활성 요청(REQ_STATUS IN ('REQUESTED','AGREED')) 중 <b>출발일(연차 사용일)</b> 또는
 * <b>이동 대상일</b> 이 조회 일자와 일치하는 건을 내린다(양쪽 셀에서 동일 요청이 보이도록 — 근태 요청 UX 정합).
 *
 * <p>처리(확인/반려)는 신규 EP 없이 attd13 의 기존 엔드포인트를 재사용한다
 * ({@code POST /webApi/attd13/change-requests/{id}/confirm|reject}). 권한·마감·만료·충돌 재검증이
 * 그쪽 서비스에 이미 있으므로 중복 구현하지 않는다.
 *
 * ⚠️ MyBatis record 는 컬럼을 "순서(위치)"로 생성자 인자에 바인딩한다.
 *    아래 필드 순서는 selectDailyLeaveChangeReq 의 SELECT 절 컬럼 순서와 정확히 일치해야 한다
 *    (record 끝 = SELECT 끝). 중간 삽입/순서 변경 금지(전 필드 밀림 → 런타임 변환 폭발).
 */
public record DailyLeaveChangeReqResult(
    /** 변경요청 ID (TB_LEAVE_CHANGE_REQUEST.CHANGE_REQ_ID) — 확인/반려 EP 의 path 변수 */
      String changeReqId

    /** 대상 연차 사용 ID (TB_USER_LEAVE_USE.LEAVE_ID) */
    , String targetLeaveId

    /** 발의 주체 [SYS070] ADMIN:관리자 / WORKER:근로자 */
    , String initiatorType

    /** 요청 유형 [SYS071] MOVE:이동 / DELETE:삭제(관리자 발의만) */
    , String reqType

    /** 요청 상태 [SYS072] REQUESTED:근로자 응답대기 / AGREED:관리자 확인대기 */
    , String reqStatus

    /** 출발일 = 현재 연차 사용일 (TB_USER_LEAVE_USE.START_DATE, YYYYMMDD) */
    , String targetStartDate

    /** 이동 대상일 (YYYYMMDD. MOVE 만 값 존재, DELETE 는 NULL) */
    , String moveTargetDate

    /** 요청 사유 (발의자 작성, 필수) */
    , String reqReason

    /** 요청 등록 일시 (yyyyMMddHHmmss) */
    , String insertDate

    /** 연차 종류 코드 (TB_USER_LEAVE_USE.LEAVE_CD) */
    , String leaveCd

    /** 연차 종류명 (TB_LEAVE_TYPE_MGMT.LEAVE_NM) */
    , String leaveNm

    /** 사용단위 코드 [SYS025] (00종일/01반차/02 2시간/03 1시간/04 30분/05 반반차) */
    , String useUnitType

    /** 사용단위 한글 라벨 (SYS025 FNC 산출) */
    , String unitNm

    /** 시작 시각 (HHmm, 시간차일 때만 의미) */
    , String startTime

    /** 종료 시각 (HHmm, 시간차일 때만 의미) */
    , String endTime

    /** 차감 일수 (decimal 문자열 그대로 — FE 가 정규화) */
    , String leaveDays

    /* ── 위치선택 확장(2026-08-18) — SELECT 절 끝과 1:1(위치매핑). 표시 전용 ── */

    /** 이동 대상 반차 파트 (START:시작기준-늦게출근 / END:종료기준-일찍퇴근 / NULL:원 파트 유지) */
    , String moveTargetHalfPart

    /** 이동 대상 시간차 시작 시각 (HHMM / NULL:원 시각 유지. 종료는 원 분량으로 서버 파생) */
    , String moveTargetStartTime
) {
}
