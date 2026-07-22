package com.prafta.web.subcon.subcon03.result;

import java.util.List;

/**
 * 마감 커버리지 필터 결과(하도급 부분공유 PS-03/04/06).
 *
 * <p>approve(실제 스냅샷 생성)와 approve-info(승인 전 예고)가 <b>같은 계산</b>을 쓰기 위한 공용
 * 결과 구조 — 예고와 실제 기록이 어긋나면 안 된다(D-1/D-2).
 */
public record CoverageResult(
    List<SnapshotSourceRow> includedRows    // 커버리지 통과 행(동의 필터의 입력 — AND 결합)
    , List<CoverageMonthResult> months      // 요청 기간 전 월의 월별 요약(META months 원천)
    , boolean partial                       // 제외 행 존재 여부(월 status 에 PARTIAL/NONE 존재와 동치)
    , int excludedRowCnt                    // 커버리지로 제외된 행 수(로그/관측용 — 저장 안 함)
){
}
