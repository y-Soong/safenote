package com.prafta.common.cmm.leave.promotion.autobatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

import com.prafta.common.cmm.leave.promotion.autobatch.BatchProposal.Assignment;
import com.prafta.common.cmm.leave.promotion.autobatch.BatchProposal.DailyLoad;
import com.prafta.common.cmm.leave.promotion.autobatch.BatchProposal.Shortage;

/**
 * prafta-com-008-A-5: 전략 ② 하루 동시휴가 인원 최소화(MIN_OVERLAP, autobatch §4).
 *
 * <p>피크(max_d load[d]) 최소화 + 평탄화. Phase 1 결정적 그리디(tightest-first + least-loaded-date)
 * 후 Phase 2 단조 감소 국소개선(D-AB5)을 1패스 적용한다. 초기부하(기존 CONFIRMED)를 반영해 진짜
 * 동시 휴가자를 최소화한다(빈 캘린더 가정 금지).
 *
 * <p>결정성(autobatch §7): 모든 정렬 tie-break 는 명시 키(load asc → ymd asc, slack asc → r desc →
 * userCd asc)로 고정. 난수 없음 → 동일 입력 동일 결과.
 */
@Component
public class MinOverlapPlanner {

    /** 전략 ②로 제안을 산출한다(등록 없음). */
    public BatchProposal plan(PlannerInput input) {
        // 일자별 부하(초기부하 시드 → 배정마다 +1). TreeMap = ymd asc(결정성).
        Map<String, Integer> load = new TreeMap<>();
        if (input.initialLoad() != null) {
            load.putAll(input.initialLoad());
        }

        // 사용자별 배정 결과(삽입순 보존 + 빠른 포함검사). userCd → 선택 날짜 집합.
        Map<String, LinkedHashSet<String>> picks = new TreeMap<>();
        List<Shortage> shortages = new ArrayList<>();

        // 1) 처리 순서: slack 적은(제약 큰) 사용자 먼저 → r 많은 → userCd asc.
        List<PlannerInput.UserPlan> order = new ArrayList<>(input.users());
        order.sort(Comparator
                .comparingInt((PlannerInput.UserPlan u) -> u.assignableYmds().size() - u.requiredDays())
                .thenComparing(Comparator.comparingInt(PlannerInput.UserPlan::requiredDays).reversed())
                .thenComparing(PlannerInput.UserPlan::userCd));

        // 2) 각 사용자에게 "현재 부하 최소" 날짜 r_i 개 배정(tie-break: 이른 날 우선).
        for (PlannerInput.UserPlan u : order) {
            LinkedHashSet<String> mine = new LinkedHashSet<>();
            picks.put(u.userCd(), mine);
            int required = u.requiredDays();
            if (required <= 0) {
                continue;
            }

            List<String> candidates = new ArrayList<>(u.assignableYmds());
            // load asc → ymd asc.
            candidates.sort(Comparator
                    .comparingInt((String d) -> load.getOrDefault(d, 0))
                    .thenComparing(Comparator.naturalOrder()));

            int taken = 0;
            for (String d : candidates) {
                if (taken >= required) {
                    break;
                }
                mine.add(d);
                load.merge(d, 1, Integer::sum);
                taken++;
            }
            if (taken < required) {
                shortages.add(new Shortage(u.userCd(), required, taken, required - taken,
                        "가용일 부족(윈도/만료/휴일/기존연차 제외 후 부족)"));
            }
        }

        // 3) Phase 2: 피크 감소 국소개선(단조 감소, 종료 보장).
        localImprove(input, picks, load);

        // 4) 출력 — 사용자는 userCd asc(picks TreeMap), 날짜는 오름차순.
        List<Assignment> assignments = new ArrayList<>();
        for (PlannerInput.UserPlan u : input.users()) {
            LinkedHashSet<String> mine = picks.get(u.userCd());
            List<String> ymds = (mine == null) ? new ArrayList<>() : new ArrayList<>(mine);
            ymds.sort(Comparator.naturalOrder());
            assignments.add(new Assignment(u.userCd(), u.siteCd(), ymds));
        }
        // assignments 도 userCd asc(input.users 가 이미 정렬).

        return new BatchProposal(
                "MIN_OVERLAP",
                input.windowFrom(),
                input.windowTo(),
                assignments,
                shortages,
                toDailyLoad(load),
                peak(load));
    }

    /**
     * Phase 2 국소개선(autobatch §4-4). 피크 날짜의 한 배정을 더 한산한 날로 이동(load[v] ≤ load[p]-2).
     * 단조 감소(피크 기여 감소, v 새 피크 안 됨) → 종료 보장. K=윈도일수 라운드 상한.
     */
    private void localImprove(PlannerInput input,
                              Map<String, LinkedHashSet<String>> picks,
                              Map<String, Integer> load) {
        // 사용자별 가용일 빠른 조회(이동 후보 v 검증용).
        Map<String, List<String>> assignableByUser = new TreeMap<>();
        for (PlannerInput.UserPlan u : input.users()) {
            assignableByUser.put(u.userCd(), u.assignableYmds());
        }

        int maxRounds = Math.max(1, load.size());
        for (int round = 0; round < maxRounds; round++) {
            int peak = peak(load);
            if (peak <= 1) {
                break;
            }
            boolean improved = false;

            // 피크 날짜를 ymd asc 로 순회.
            List<String> peakDates = new ArrayList<>();
            for (Map.Entry<String, Integer> e : load.entrySet()) {
                if (e.getValue() != null && e.getValue() == peak) {
                    peakDates.add(e.getKey());
                }
            }
            peakDates.sort(Comparator.naturalOrder());

            for (String p : peakDates) {
                // p 에 배정된 사용자(userCd asc).
                List<String> usersOnP = new ArrayList<>();
                for (Map.Entry<String, LinkedHashSet<String>> e : picks.entrySet()) {
                    if (e.getValue().contains(p)) {
                        usersOnP.add(e.getKey());
                    }
                }
                usersOnP.sort(Comparator.naturalOrder());

                for (String userCd : usersOnP) {
                    LinkedHashSet<String> mine = picks.get(userCd);
                    List<String> assignable = assignableByUser.getOrDefault(userCd, List.of());
                    // 이동 후보 v: load asc → ymd asc, u 에게 미배정 + load[v] ≤ load[p]-2.
                    List<String> vCands = new ArrayList<>(assignable);
                    vCands.sort(Comparator
                            .comparingInt((String d) -> load.getOrDefault(d, 0))
                            .thenComparing(Comparator.naturalOrder()));
                    for (String v : vCands) {
                        if (mine.contains(v)) {
                            continue;
                        }
                        int lv = load.getOrDefault(v, 0);
                        if (lv <= peak - 2) {
                            // u 를 p → v 이동.
                            mine.remove(p);
                            mine.add(v);
                            load.merge(p, -1, Integer::sum);
                            load.merge(v, 1, Integer::sum);
                            improved = true;
                            break;
                        }
                    }
                    if (improved) {
                        break;
                    }
                }
                if (improved) {
                    break;
                }
            }
            if (!improved) {
                break;
            }
        }
    }

    private static List<DailyLoad> toDailyLoad(Map<String, Integer> load) {
        List<DailyLoad> list = new ArrayList<>();
        for (Map.Entry<String, Integer> e : load.entrySet()) {
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
