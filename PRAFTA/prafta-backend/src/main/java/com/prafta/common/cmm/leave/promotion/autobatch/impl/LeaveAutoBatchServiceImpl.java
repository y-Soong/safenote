package com.prafta.common.cmm.leave.promotion.autobatch.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.leave.promotion.autobatch.AssignableDateResolver;
import com.prafta.common.cmm.leave.promotion.autobatch.AutoBatchTargetVO;
import com.prafta.common.cmm.leave.promotion.autobatch.BatchProposal;
import com.prafta.common.cmm.leave.promotion.autobatch.LeaveAutoBatchService;
import com.prafta.common.cmm.leave.promotion.autobatch.mapper.LeaveAutoBatchMapper;
import com.prafta.common.cmm.leave.promotion.autobatch.MinOverlapPlanner;
import com.prafta.common.cmm.leave.promotion.autobatch.PlannerInput;
import com.prafta.common.cmm.leave.promotion.autobatch.YearEndPlanner;
import com.prafta.common.error.leavepromo.LeavePromoErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-com-008-A-5: 자동배치 계산 서비스 구현(순수 계산 — 프리뷰).
 *
 * <p>스냅샷 1회 조회 → 가용일 전처리 → 전략 분기. 본 서비스는 어떤 쓰기도 하지 않는다(읽기 전용).
 * 커밋은 웹 서비스가 공용 등록 헬퍼로 별도 수행한다(권한/PUSH/마스터 동반).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveAutoBatchServiceImpl implements LeaveAutoBatchService {

    private final LeaveAutoBatchMapper leaveAutoBatchMapper;
    private final AssignableDateResolver assignableDateResolver;
    private final YearEndPlanner yearEndPlanner;
    private final MinOverlapPlanner minOverlapPlanner;

    @Override
    public BatchProposal preview(String cmpnyCd, String siteCd, String nodeCd, String incSubNodeYn,
                                 String userNm, String tenureFilter,
                                 String strategy, String windowFrom, String windowTo, LocalDate today) {

        if (!STRATEGY_YEAR_END.equals(strategy) && !STRATEGY_MIN_OVERLAP.equals(strategy)) {
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_001);
        }
        if (windowFrom == null || windowFrom.length() != 8
                || windowTo == null || windowTo.length() != 8
                || windowFrom.compareTo(windowTo) > 0) {
            throw new ApiException(LeavePromoErrorCode.LEAVEPROMO_400_001);
        }

        String oneYearAgo = today.minusYears(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 1) 대상자(스코프 내 SECOND 도래자 + 잔여>0) + 본연차 사용가능 구간.
        List<AutoBatchTargetVO> targets = leaveAutoBatchMapper.selectAutoBatchTargets(
                cmpnyCd, siteCd, nodeCd, incSubNodeYn, userNm, tenureFilter, oneYearAgo);
        if (targets == null) {
            targets = new ArrayList<>();
        }

        // 2) 회사 휴일 스냅샷(윈도 기간 일자 + 매년 반복 규칙).
        Set<String> holidayYmds = new HashSet<>(
                nullToEmpty(leaveAutoBatchMapper.selectHolidayYmds(cmpnyCd, windowFrom, windowTo)));
        Set<String> holidayMmdds = new HashSet<>(
                nullToEmpty(leaveAutoBatchMapper.selectHolidayRuleMmdds(cmpnyCd)));

        // 3) 초기부하(스코프 내 윈도 일자별 기존 CONFIRMED 인원).
        Map<String, Integer> initialLoad = new TreeMap<>();
        List<LeaveAutoBatchMapper.DateCountRow> loadRows = leaveAutoBatchMapper.selectScopeDailyLeaveLoad(
                cmpnyCd, siteCd, nodeCd, incSubNodeYn, windowFrom, windowTo);
        if (loadRows != null) {
            for (LeaveAutoBatchMapper.DateCountRow r : loadRows) {
                if (r.getYmd() != null) {
                    initialLoad.put(r.getYmd(), r.getCnt());
                }
            }
        }

        // 4) 사용자별 가용일 + 요구일수(r_i 1일 단위 정수 환산) → PlannerInput.UserPlan.
        List<PlannerInput.UserPlan> userPlans = new ArrayList<>();
        for (AutoBatchTargetVO t : targets) {
            int required = toWholeDays(t.getRemainingDays());
            if (required <= 0) {
                continue;
            }
            Set<String> existing = new HashSet<>(nullToEmpty(
                    leaveAutoBatchMapper.selectUserConfirmedLeaveYmds(cmpnyCd, t.getUserCd(), windowFrom, windowTo)));
            List<String> assignable = assignableDateResolver.resolve(
                    cmpnyCd, t.getSiteCd(), t.getUserCd(),
                    t.getAvailFromDate(), t.getAvailToDate(),
                    windowFrom, windowTo,
                    holidayYmds, holidayMmdds, existing);
            userPlans.add(new PlannerInput.UserPlan(
                    t.getUserCd(), t.getSiteCd(), required, assignable, t.getAvailToDate()));
        }

        PlannerInput input = new PlannerInput(userPlans, windowFrom, windowTo, initialLoad);

        BatchProposal proposal = STRATEGY_YEAR_END.equals(strategy)
                ? yearEndPlanner.plan(input)
                : minOverlapPlanner.plan(input);

        log.info("[autobatch] 프리뷰 — siteCd={}, nodeCd={}, strategy={}, 대상 {}명, 미달 {}건, peak={}",
                siteCd, nodeCd, strategy, userPlans.size(),
                proposal.shortages() == null ? 0 : proposal.shortages().size(), proposal.peakLoad());
        return proposal;
    }

    /** 잔여(decimal)를 1일 단위 정수로 환산(소수점 절사 — 0.5 등 시간차 잔여는 자동배치 대상 아님). */
    private static int toWholeDays(BigDecimal remaining) {
        if (remaining == null) {
            return 0;
        }
        return remaining.setScale(0, java.math.RoundingMode.DOWN).intValue();
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return (list == null) ? List.of() : list;
    }
}
