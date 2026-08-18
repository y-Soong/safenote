package com.prafta.web.attd.attd14.result;

/**
 * 관리자 발신 연차 변경 요청 이력 1건(목록/상세) (prafta-com-016-H).
 *
 * <p>출처 테이블 = {@code TB_LEAVE_CHANGE_REQUEST}(INITIATOR_TYPE='ADMIN' 고정). 읽기 전용.
 * attd13 의 {@code LeaveChangeRequestRowResult} 를 복제하되, 표시용 발의자/확인자 이름
 * (initiatorUserNm/confirmUserNm)을 레코드 말미에 추가한다(MyBatis 이름 기반 매핑 — SELECT 별칭 = 레코드 컴포넌트명 일치).
 */
public record AdminRequestHistoryRowResult(
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
    , String initiatorUserNm
    , String confirmUserCd
    , String confirmUserNm
    , String confirmDate
    , String insertDate
    // 위치선택 확장(2026-08-18 재작업 B): 이동 대상 위치 병기용 — 미지정(NULL)이면 종전 표시 그대로.
    //   ★ 목록(selectAdminRequestHistory)·상세(selectAdminRequestHistoryDetail) 두 쿼리가 본 record 를
    //   공유하므로 SELECT 끝 3컬럼을 두 쿼리에 동시 유지할 것.
    , String moveTargetHalfPart  // 이동 대상 반차 파트 (START/END, NULL:원 파트 유지)
    , String moveTargetStartTime // 이동 대상 시간차 시작 시각 (HHMM, NULL:원 시각 유지)
    , Integer leaveMinutes       // 대상 use 대표행 분량(분) — 시간차 종료 파생용(시작+분량)
) {
}
