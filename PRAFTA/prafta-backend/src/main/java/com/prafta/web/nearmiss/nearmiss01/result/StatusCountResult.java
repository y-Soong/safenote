package com.prafta.web.nearmiss.nearmiss01.result;

/**
 * 상태별 카운트 결과 VO (E3 탭 배지용).
 * 단일 행 집계 결과: 접수/검토중/조치중/완료 건수.
 */
public record StatusCountResult(
    int receivedCnt    // 100 접수
    , int reviewingCnt // 200 검토중
    , int actingCnt    // 300 조치중
    , int completedCnt // 400 완료
){
}
