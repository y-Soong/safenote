package com.prafta.common.cmm.leave.promotion.autobatch;

import java.time.LocalDate;

/**
 * prafta-com-008-A-5: 자동배치 계산 서비스(2전략, 순수 계산 — 프리뷰 산출).
 *
 * <p>대상자/기존연차/초기부하/휴일 스냅샷을 1회 조회한 뒤, 가용일 전처리({@link AssignableDateResolver})
 * + 전략(YEAR_END/MIN_OVERLAP)으로 제안({@link BatchProposal})만 만든다. <b>등록은 하지 않는다</b>(커밋은
 * 웹 서비스가 공용 등록 헬퍼로 별도 수행). 결정성: 기준일 today 주입, 난수 없음(autobatch §7).
 */
public interface LeaveAutoBatchService {

    /** 전략 코드 — 말일 역방향 채우기. */
    String STRATEGY_YEAR_END = "YEAR_END";
    /** 전략 코드 — 하루 동시휴가 인원 최소화. */
    String STRATEGY_MIN_OVERLAP = "MIN_OVERLAP";

    /**
     * 자동배치 제안(프리뷰)을 산출한다. 등록 없음.
     *
     * @param cmpnyCd      회사 코드(JWT 강제값)
     * @param siteCd       세션 고정 사업장
     * @param nodeCd       조회 노드(null=루트)
     * @param incSubNodeYn 하위노드 포함('Y'/'N')
     * @param userNm       사용자명 LIKE 필터(빈값 무시)
     * @param tenureFilter 1년차 필터('ALL'/'OVER1'/'UNDER1')
     * @param strategy     'YEAR_END' | 'MIN_OVERLAP'
     * @param windowFrom   배치 윈도 시작(YYYYMMDD)
     * @param windowTo     배치 윈도 종료(YYYYMMDD)
     * @param today        기준일(컨트롤러가 1회 산출 — 결정성)
     * @return 배치 제안(assignments/shortages/dailyLoad/peakLoad)
     */
    BatchProposal preview(String cmpnyCd, String siteCd, String nodeCd, String incSubNodeYn,
                          String userNm, String tenureFilter,
                          String strategy, String windowFrom, String windowTo, LocalDate today);
}
