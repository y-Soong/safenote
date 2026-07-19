package com.prafta.web.subcon.subcon03.result;

import java.util.List;

/**
 * 근태 마감 게이팅 판정 결과(PRAFTA-SUBCON-T3 §5-4).
 *
 * <p>closedAll = 요청 기간의 모든 월이 (모든 부서노드 + 전체 센티넬 '*' 기준으로) 마감 커버됨.
 * unclosedYms = 미마감 월 목록(YYYY-MM 표기 — 화면 안내용).
 */
public record CloseGateResult(
    boolean closedAll
    , List<String> unclosedYms
){
}
