package com.prafta.common.cmm.leave.promotion.autobatch;

import java.util.List;

/**
 * prafta-com-008-A-5: 자동배치 제안(proposal) — 프리뷰 응답 + 커밋 입력.
 *
 * <p>순수 계산 결과만 담는다(등록 없음). 프론트(LeavePromotionAutoBatchPop)가 dailyLoad/peakLoad/
 * shortages 로 분산 품질·미달을 검수한 뒤, 본 객체를 그대로 커밋 EP 로 되돌려 직권 지정에 사용한다.
 *
 * <p>결정성(autobatch §7): 동일 입력(전략/기간/대상자/스냅샷)이면 동일 proposal. 모든 정렬은 명시
 * 키(userCd/ymd asc)로 고정한다.
 *
 * @param strategy    배치 전략 ('YEAR_END' | 'MIN_OVERLAP')
 * @param windowFrom  배정 윈도 시작 (YYYYMMDD)
 * @param windowTo    배정 윈도 종료 (YYYYMMDD)
 * @param assignments 사용자별 배정 날짜
 * @param shortages   가용일 부족으로 일부/전부 미배정된 사용자
 * @param dailyLoad   일자별 휴가 인원(초기부하 포함, 검수/차트용)
 * @param peakLoad    max(load) — MIN_OVERLAP 검수 지표
 */
public record BatchProposal(
        String strategy,
        String windowFrom,
        String windowTo,
        List<Assignment> assignments,
        List<Shortage> shortages,
        List<DailyLoad> dailyLoad,
        int peakLoad
) {

    /** 사용자 1명의 배정 결과(오름차순 날짜 목록). */
    public record Assignment(
            String userCd,
            String siteCd,
            List<String> ymds
    ) {
    }

    /** 가용일 부족으로 미배정 일수가 남은 사용자(prafta-052 패턴 표시용). */
    public record Shortage(
            String userCd,
            int requiredDays,
            int assignedDays,
            int shortageDays,
            String reason
    ) {
    }

    /** 일자별 휴가 인원(초기부하 + 이번 배정 합). */
    public record DailyLoad(
            String ymd,
            int count
    ) {
    }
}
