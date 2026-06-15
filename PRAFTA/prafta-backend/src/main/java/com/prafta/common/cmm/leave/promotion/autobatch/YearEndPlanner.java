package com.prafta.common.cmm.leave.promotion.autobatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

import com.prafta.common.cmm.leave.promotion.autobatch.BatchProposal.Assignment;
import com.prafta.common.cmm.leave.promotion.autobatch.BatchProposal.DailyLoad;
import com.prafta.common.cmm.leave.promotion.autobatch.BatchProposal.Shortage;

/**
 * prafta-com-008-A-5: 전략 ① 말일 역방향 채우기(YEAR_END, autobatch §3).
 *
 * <p>사용자별 독립 처리. anchor = min(windowTo, availTo) 부터 역방향으로 가용일을 r_i 개 채운다.
 * 만료 임박일부터 빠르게 소진(후반 집중은 의도된 특성). 균형 고려 없음.
 *
 * <p>결정성: 사용자 처리 순서 userCd asc(입력이 이미 정렬), 각자 anchor 역순 결정적. 난수 없음.
 */
@Component
public class YearEndPlanner {

    /** 전략 ①로 제안을 산출한다(등록 없음). */
    public BatchProposal plan(PlannerInput input) {
        List<Assignment> assignments = new ArrayList<>();
        List<Shortage> shortages = new ArrayList<>();
        // 일자별 부하 = 초기부하 + 이번 배정(검수용). 정렬 위해 TreeMap.
        Map<String, Integer> load = new TreeMap<>();
        if (input.initialLoad() != null) {
            load.putAll(input.initialLoad());
        }

        for (PlannerInput.UserPlan u : input.users()) {
            int required = u.requiredDays();
            // anchor 캡 = min(windowTo, availTo). 입력 가용일은 이미 만료/주말/휴일 제외 + 오름차순.
            String anchor = capAnchor(input.windowTo(), u.availTo());

            // anchor 이하 가용일을 역순(내림차순)으로 take(required).
            List<String> picked = new ArrayList<>();
            List<String> asc = u.assignableYmds();
            for (int i = asc.size() - 1; i >= 0 && picked.size() < required; i--) {
                String d = asc.get(i);
                if (anchor != null && d.compareTo(anchor) > 0) {
                    continue;
                }
                picked.add(d);
            }
            // 반환은 오름차순.
            Collections.reverse(picked);

            assignments.add(new Assignment(u.userCd(), u.siteCd(), picked));
            for (String d : picked) {
                load.merge(d, 1, Integer::sum);
            }
            if (picked.size() < required) {
                int shortageDays = required - picked.size();
                shortages.add(new Shortage(u.userCd(), required, picked.size(), shortageDays,
                        "가용일 부족(윈도/만료/휴일/기존연차 제외 후 부족)"));
            }
        }

        return new BatchProposal(
                "YEAR_END",
                input.windowFrom(),
                input.windowTo(),
                assignments,
                shortages,
                toDailyLoad(load),
                peak(load));
    }

    private static String capAnchor(String windowTo, String availTo) {
        if (availTo == null || availTo.isBlank()) {
            return windowTo;
        }
        if (windowTo == null) {
            return availTo;
        }
        return (availTo.compareTo(windowTo) < 0) ? availTo : windowTo;
    }

    private static List<DailyLoad> toDailyLoad(Map<String, Integer> load) {
        // TreeMap 순회 = ymd 오름차순(결정성).
        List<DailyLoad> list = new ArrayList<>();
        Map<String, Integer> ordered = new LinkedHashMap<>(load);
        for (Map.Entry<String, Integer> e : ordered.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) {
                list.add(new DailyLoad(e.getKey(), e.getValue()));
            }
        }
        return list;
    }

    private static int peak(Map<String, Integer> load) {
        int max = 0;
        for (Integer v : load.values()) {
            if (v != null && v > max) {
                max = v;
            }
        }
        return max;
    }
}
