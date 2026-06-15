package com.prafta.common.cmm.leave.promotion.autobatch;

import java.util.List;
import java.util.Map;

/**
 * prafta-com-008-A-5: 전략 플래너(YEAR_END/MIN_OVERLAP) 공통 입력(순수).
 *
 * <p>가용일 전처리({@link AssignableDateResolver})와 초기부하(스코프 기존 CONFIRMED) 산출이 끝난 뒤의
 * 결정적 계산 입력이다. 플래너는 본 입력만으로 동작하며 DB/난수/시계에 의존하지 않는다(재현성).
 *
 * @param users         사용자별 가용일/요구일수(userCd asc 정렬되어 들어옴)
 * @param windowFrom    배치 윈도 시작(proposal 메타 보존용, YYYYMMDD)
 * @param windowTo      배치 윈도 종료(YEAR_END anchor 상한, YYYYMMDD)
 * @param initialLoad   일자별 초기부하(기존 CONFIRMED 인원) — MIN_OVERLAP 시드. 가변 복사본을 받는다.
 */
public record PlannerInput(
        List<UserPlan> users,
        String windowFrom,
        String windowTo,
        Map<String, Integer> initialLoad
) {

    /**
     * 사용자 1명의 플래너 입력.
     *
     * @param userCd          사용자 코드
     * @param siteCd          사업장 코드
     * @param requiredDays    배정해야 할 일수(r_i, 1일 단위 정수)
     * @param assignableYmds  가용일 오름차순(A_i)
     * @param availTo         본연차 만료일(YEAR_END anchor 캡, YYYYMMDD, null 허용)
     */
    public record UserPlan(
            String userCd,
            String siteCd,
            int requiredDays,
            List<String> assignableYmds,
            String availTo
    ) {
    }
}
