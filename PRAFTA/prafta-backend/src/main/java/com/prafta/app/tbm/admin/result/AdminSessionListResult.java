package com.prafta.app.tbm.admin.result;

/** T-A1 교육관리 리스트 행. 출결/이수/미이수 집계 포함. */
public record AdminSessionListResult(
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
