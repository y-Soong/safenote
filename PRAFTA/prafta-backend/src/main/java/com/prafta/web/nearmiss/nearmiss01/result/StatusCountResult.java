package com.prafta.web.nearmiss.nearmiss01.result;

/**
 * 상태별 카운트 결과 VO (E3 탭 배지용).
 * 단일 행 집계 결과 (SYS063 재번호 D4): 접수/조치중/완료/미처리대상 건수.
 */
public record StatusCountResult(
    int receivedCnt      // 100 접수
    , int actingCnt      // 200 조치중
    , int completedCnt   // 300 완료
    , int unaddressedCnt // 400 미처리대상
){
}
