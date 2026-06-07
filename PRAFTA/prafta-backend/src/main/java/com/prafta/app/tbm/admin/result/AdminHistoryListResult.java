package com.prafta.app.tbm.admin.result;

/** R6 이력 리스트 행(STATUS_CD IN COMPLETED/CANCELLED). web Tbm04 HistorySessionListResult 포팅. */
public record AdminHistoryListResult(
    String sessionCd
    , String siteCd
    , String siteNm
    , String title
    , String statusCd
    , String statusNm
    , String managerUserCd
    , String managerUserNm
    , String openedAt
    , String startedAt
    , String endedAt
    , String insertDate
    , int riskCount
    , int attendanceCount
    , int completedCount
    , int notCompletedCount
){
}
