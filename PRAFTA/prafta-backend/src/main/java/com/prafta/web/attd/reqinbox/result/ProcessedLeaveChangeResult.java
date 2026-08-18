package com.prafta.web.attd.reqinbox.result;

/**
 * 요청 승인 관리 — 내가 확인(CONFIRMED)/반려(REJECTED)한 연차 변경(이동/삭제) 요청 이력 1건.
 *
 * <p>TB_LEAVE_CHANGE_REQUEST.CONFIRM_USER_CD = 본인 기준. 연차 탭 "내 처리 이력" 팝업의
 * 보조 섹션에 표시한다.
 *
 * <p>★ record 위치 기반 매핑 — SELECT 컬럼 순서와 컴포넌트 순서 1:1 유지.
 */
public record ProcessedLeaveChangeResult(
      String changeReqId
    , String reqType          // MOVE:이동 / DELETE:삭제 [SYS071]
    , String targetUserCd
    , String targetUserNm
    , String targetStartDate  // 대상 연차일 (YYYYMMDD)
    , String moveTargetDate   // 이동 대상일 (MOVE 시)
    , String initiatorType    // ADMIN / WORKER [SYS070]
    , String reqStatus        // CONFIRMED / REJECTED [SYS072]
    , String reqReason
    , String rejectReason     // 관리자 반려 사유(REJECTED 시)
    , String confirmDate      // 내가 확인/반려한 일시
    // 위치선택 확장(2026-08-18 재작업 B): 이동 대상 위치 병기용 — 미지정(NULL)이면 종전 표시 그대로.
    , String moveTargetHalfPart  // 이동 대상 반차 파트 (START/END, NULL:원 파트 유지)
    , String moveTargetStartTime // 이동 대상 시간차 시작 시각 (HHMM, NULL:원 시각 유지)
    , Integer leaveMinutes       // 대상 use 대표행 분량(분) — 시간차 종료 파생용(시작+분량)
) {
}
