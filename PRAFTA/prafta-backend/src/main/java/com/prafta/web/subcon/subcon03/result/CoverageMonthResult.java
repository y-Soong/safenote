package com.prafta.web.subcon.subcon03.result;

import java.util.List;

/**
 * 월별 마감 커버리지 요약 1건(하도급 부분공유 PS-03/04/06 — COVERAGE_META JSON 계약과 동일 구조).
 *
 * <p>ym 은 내부 표준 YYYYMM(META 기록 계약값). 승인 사전정보 응답에서는 서비스가 YYYY-MM 으로
 * 변환해 내려준다. excludedDeptNms 는 <b>실제 제외된 행</b>의 부서명 집합(최대 20개 + 초과 시
 * "외 N개 부서" 항목 1개)이며 성명/USER_CD 등 개인 식별 정보는 절대 담지 않는다(공통 §11).
 */
public record CoverageMonthResult(
    String ym                       // YYYYMM(META 계약) — 응답 변환은 서비스에서
    , String status                 // FULL:제외 0건 / PARTIAL:일부 제외 / NONE:포함 0건
    , List<String> excludedDeptNms  // 제외 행의 부서명(캡 20 + "외 N개 부서"). FULL 이면 빈 목록
    , String orphanUnclosedYn       // 무부서/고아 행 제외 존재 시 'Y', 아니면 null
){
}
