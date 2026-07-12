package com.prafta.app.tbm.admin.result;

/** T-A1 교육관리 리스트 행. 출결/이수/미이수 집계 포함. */
public record AdminSessionListResult(
    String sessionCd
    , String siteCd
    , String siteNm
    , String title
    , String statusCd
    , String statusNm
    , Integer eduMinutes        // 교육 인정시간(분, 1~60). 미설정 시 null
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
