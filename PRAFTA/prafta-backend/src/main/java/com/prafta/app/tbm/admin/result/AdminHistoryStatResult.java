package com.prafta.app.tbm.admin.result;

/**
 * R6 이력 상단 통계 집계(스코프 적용된 동일 조건, 페이징 무관 전체).
 *
 * <p>web Tbm04 HistoryStatResult 포팅. avgCompletionRate 는 서비스단에서 산출(이수/참여*100).
 */
public record AdminHistoryStatResult(
    int sessionCount
    , int attendanceCount
    , int completedCount
    , int notCompletedCount
){
}
