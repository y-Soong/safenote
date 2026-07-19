package com.prafta.common.cmm.dailyentry.application.query;

/**
 * 입장 승인요청 목록 조회 쿼리 (웹 User_08 / 앱 관리자 공용).
 *
 * <p>siteCd 는 필수(사업장 스코프 강제). reqStatus/reqType/reqDate(YYYYMMDD)는 선택 필터.
 * limitCnt 는 전수조회 방지 상한(서비스에서 고정 주입).
 */
public record EntryRequestListQuery(
    String cmpnyCd
    , String siteCd
    , String reqStatus
    , String reqType
    , String reqDate     // YYYYMMDD (REQ_DTIME 일자 필터)
    , int limitCnt
) {
}
