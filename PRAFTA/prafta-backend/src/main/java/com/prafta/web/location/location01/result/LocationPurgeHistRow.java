package com.prafta.web.location.location01.result;

/**
 * 위치정보 파기 이력 1행 — Location_01 상세.
 *
 * <p>★★좌표 값은 담지 않는다(테이블 자체에 없다). "누가·언제·무엇을·몇 건·왜" 뿐이다.
 * <p>★record 위치 매핑 — SELECT 순서 유지.
 */
public record LocationPurgeHistRow(
      long purgeId
    , String purgeReasonCd
    , String termsVersion
    , int attdGpsRows
    , int tbmAttendanceRows
    , int tbmSessionRows
    , String oldestCollected
    , String latestCollected
    , String actorUserCd
    , String actionDtime
) {
}
