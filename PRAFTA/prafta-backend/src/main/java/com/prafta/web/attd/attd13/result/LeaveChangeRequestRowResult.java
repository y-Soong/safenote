package com.prafta.web.attd.attd13.result;

/**
 * 연차 변경/삭제 요청 1건(목록/상세) (PRAFTA-COM-008-C).
 *
 * <p>SELECT 컬럼 순서 = 생성자 인자 순서(MyBatis 위치 기반 매핑). 컬럼 추가 시 SELECT 도 동일 위치 유지.
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
) {
}
