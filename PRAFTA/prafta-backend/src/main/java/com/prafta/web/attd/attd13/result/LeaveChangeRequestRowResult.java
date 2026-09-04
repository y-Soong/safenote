package com.prafta.web.attd.attd13.result;

/**
 * 연차 변경/삭제 요청 1건(목록/상세) (PRAFTA-COM-008-C).
 *
 * <p>SELECT 컬럼 순서 = 생성자 인자 순서(MyBatis 위치 기반 매핑). 컬럼 추가 시 SELECT 도 동일 위치 유지.
 *
 * <p>G1(운영 실증 - 2026-08-04): 대상 연차의 <b>사용단위/시각/차감량</b>이 응답에 없어 웹 "변경 요청 확인"
 * 팝업·앱 "연차 변경 확인"·앱 근로자 동의 팝업이 "무슨 연차인지" 를 표시하지 못했다. 표시용 컬럼 8개를
 * <b>마지막에</b> 추가한다(기존 19 컴포넌트 순서 불변 - 위치매핑).
 * 본 record 를 쓰는 3개 조회(selectChangeRequests / selectChangeRequest / selectPendingConsents)의
 * SELECT 절 끝에 동일 순서로 컬럼을 추가해야 한다. 한 쿼리라도 빠지면 생성자 불일치로 조회가 전멸한다.
 */
public record LeaveChangeRequestRowResult(
      String changeReqId
    , String cmpnyCd
    , String siteCd
    , String targetUserCd
    , String targetUserNm
    , String targetLeaveId
    , String targetStartDate
    , String initiatorType
    , String reqType
    , String moveTargetDate
    , String reqReason
    , String workerResponse
    , String responseReason
    , String rejectReason
    , String reqStatus
    , String initiatorUserCd
    , String confirmUserCd
    , String confirmDate
    , String insertDate

    /* ── G1 표시용 확장(대상 연차 속성) — 여기부터 SELECT 절 끝과 1:1 대응 ── */

    /** 연차 종류 코드 (TB_USER_LEAVE_USE.LEAVE_CD) */
    , String leaveCd

    /** 연차 종류명 (TB_LEAVE_TYPE_MGMT.LEAVE_NM) */
    , String leaveNm

    /** 사용단위 코드 [SYS025] (00종일/01반차/02 2시간/03 1시간/04 30분/05 반반차) */
    , String useUnitType

    /** 사용단위 한글 라벨 (SYS025 FNC 산출) */
    , String unitNm

    /** 시작 시각 (HHmm. 시간차 02/03/04 + 반차 01 이 값 보유 — 반차 시간대 도입(2026-08-08)으로 확장. 종일 00 은 미기록) */
    , String startTime

    /** 종료 시각 (HHmm. 시간차 02/03/04 만 값 보유) */
    , String endTime

    /** 차감 일수 (분할차감 건은 REQ 단위 합산. decimal 문자열 그대로 — FE 가 표기 정규화) */
    , String leaveDays

    /** 차감 분 (시간차 원본. 대표행이 총량을 보유 — 불변식 1) */
    , Integer leaveMinutes

    /* ── 위치선택 확장(2026-08-18) — SELECT 절 끝에 동일 순서로 3쿼리 동시 추가(위치매핑) ── */

    /** 이동 대상 반차 파트 (START:시작기준-늦게출근 / END:종료기준-일찍퇴근 / NULL:원 파트 유지) */
    , String moveTargetHalfPart

    /** 이동 대상 시간차 시작 시각 (HHMM / NULL:원 시각 유지. 종료는 원 분량으로 서버 파생) */
    , String moveTargetStartTime

    /* ── BW-12 잔여 B-1(2026-09-04): 휴게 미이용 요청 승계 표시 — 3쿼리 동시 추가(위치매핑) ── */

    /**
     * 원 연차 사용 행의 휴게 미이용 요청 여부 (TB_USER_LEAVE_USE.BRK_WAIVE_YN, 'Y'/'N').
     *
     * <p>이동은 같은 요청의 위치 변경이라 요청이 소멸하지 않고 새 행에 승계된다(정책서 attd/08-leave.md
     * §8.5.10(b)). 웹 이동 확정 화면이 "휴게 미이용 요청 승계" 배지 1줄을 띄우는 근거값이다.
     * 구 행(컬럼 DEFAULT 'N')·종일 연차는 'N'.
     */
    , String brkWaiveYn
) {
}
