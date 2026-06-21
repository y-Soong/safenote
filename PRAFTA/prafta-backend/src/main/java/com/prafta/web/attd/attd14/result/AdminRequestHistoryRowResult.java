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
) {
}
