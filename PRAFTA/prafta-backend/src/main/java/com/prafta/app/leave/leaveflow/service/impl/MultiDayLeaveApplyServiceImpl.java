package com.prafta.app.leave.leaveflow.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.app.leave.leaveflow.application.param.LeaveApplyParam;
import com.prafta.app.leave.leaveflow.dto.response.LeaveApplyMultiPreviewResponse;
import com.prafta.app.leave.leaveflow.mapper.AppLeaveFlowMapper;
import com.prafta.app.leave.leaveflow.service.AppLeaveFlowService;
import com.prafta.app.leave.leaveflow.service.MultiDayLeaveApplyService;
import com.prafta.app.leave.leaveflow.service.MultiDayLeavePlanner;
import com.prafta.app.leave.leaveflow.vo.MultiDayLeaveDayPlan;
import com.prafta.common.dto.TokenInfo;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-leavemulti: 연차 기간(From-To) 신청 오케스트레이터 구현 (종일 전용).
 *
 * <p>★ {@link AppLeaveFlowService} 와 <b>별도 빈</b>이다. 같은 빈에서 호출하면 self-invocation 으로
 * {@code submitLeave} 의 {@code @Transactional} 이 무시되어 각 INSERT 가 autocommit 되고,
 * 정책 ②(전체 실패 = 전체 롤백)가 깨진다. 여기서 프록시를 거쳐 호출하므로 REQUIRED 로 <b>본 트랜잭션에 합류</b>한다.
 *
 * <p>기존 연차 로직은 수정하지 않는다. 종일 고정({@code useUnitType='00'})·가불 미사용({@code isBorrow=false})
 * 으로 호출하므로 시간차 advisory lock 과 가불 분기에 애초에 진입하지 않고,
 * Phase 1 이 잔여를 보장하므로 짜투리(remnant) 분기에도 진입하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiDayLeaveApplyServiceImpl implements MultiDayLeaveApplyService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 기간신청은 종일만 대상이다(반차·반반차·시간차는 기존 단건 신청 사용). */
    private static final String UNIT_FULL_DAY = "00";

    private final MultiDayLeavePlanner planner;
    private final AppLeaveFlowService appLeaveFlowService;
    private final AppLeaveFlowMapper appLeaveFlowMapper;

    // ============================================================
    // 미리보기
    // ============================================================
    @Override
    @Transactional(readOnly = true)
    public LeaveApplyMultiPreviewResponse preview(TokenInfo tokenInfo, String leaveCd,
                                                  String fromYmd, String toYmd) {

        assertToken(tokenInfo);
        if (!StringUtils.hasText(leaveCd) || !isYmd(fromYmd) || !isYmd(toYmd)) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        if (fromYmd.compareTo(toYmd) > 0) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
        assertRangeWithinLimit(fromYmd, toYmd);

        String cmpny = tokenInfo.gv_cmpnyCd();
        String site = tokenInfo.gv_siteCd();
        String user = tokenInfo.gv_userCd();

        List<MultiDayLeaveDayPlan> plans = planner.planDays(cmpny, site, user, fromYmd, toYmd);

        // 잔여 시뮬레이션은 "기본 체크된 날짜" 기준으로 미리 보여준다.
        //   사용자가 체크를 바꾸면 화면이 로컬로 재계산하거나(단순 가감), 제출 시 서버가 최종 판정한다.
        List<String> defaultDates = plans.stream()
                .filter(MultiDayLeaveDayPlan::defaultChecked)
                .map(MultiDayLeaveDayPlan::ymd)
                .toList();
        MultiDayLeavePlanner.BalanceSim sim = planner.simulateBalance(cmpny, user, leaveCd, defaultDates);

        List<LeaveApplyMultiPreviewResponse.Day> days = plans.stream()
                .map(p -> LeaveApplyMultiPreviewResponse.Day.builder()
                        .ymd(p.ymd())
                        .dow(p.dow())
                        .weekend(p.weekend())
                        .holiday(p.holiday())
                        .hasSchedule(p.hasSchedule())
                        .defaultChecked(p.defaultChecked())
                        .selectable(p.selectable())
                        .blockedReasonCode(p.blockedReasonCode())
                        .blockedReason(p.blockedReason())
                        .build())
                .toList();

        log.info("[leavemulti] 기간신청 미리보기 — userCd={}, leaveCd={}, {}~{}, 기본체크={}일, 부족={}",
                user, leaveCd, fromYmd, toYmd, defaultDates.size(), sim.shortageDays().toPlainString());

        return LeaveApplyMultiPreviewResponse.builder()
                .days(days)
                .defaultCheckedCount(defaultDates.size())
                .neededDays(sim.neededDays())
                .assignedDays(sim.assignedDays())
                .shortageDays(sim.shortageDays())
                .build();
    }

    // ============================================================
    // 제출 (2-Phase)
    // ============================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String applyMulti(TokenInfo tokenInfo, String leaveCd, String leaveType, List<String> dates,
                             String reason, List<String> approverUserCds, String presetId,
                             String evidenceFileId) {

        assertToken(tokenInfo);
        if (!StringUtils.hasText(leaveCd) || dates == null || dates.isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        String cmpny = tokenInfo.gv_cmpnyCd();
        String site = tokenInfo.gv_siteCd();
        String user = tokenInfo.gv_userCd();

        // 중복 제거 + 오름차순. 날짜 배정 시뮬레이션이 오름차순 전제이며, 생성 순서도 날짜순이 자연스럽다.
        List<String> targets = new ArrayList<>(new LinkedHashSet<>(dates));
        for (String d : targets) {
            if (!isYmd(d)) {
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
        }
        targets.sort(String::compareTo);
        assertRangeWithinLimit(targets.get(0), targets.get(targets.size() - 1));

        // ---------- Phase 1 : 전 날짜 검증 (아무것도 만들지 않는다) ----------
        // ★첫 실패에서 끊지 않는다 — 막힌 날짜를 전부 모아 한 번에 돌려줘야 사용자가 한 번에 고친다.
        List<MultiDayLeaveDayPlan> blocked = planner.findBlocked(cmpny, site, user, targets);
        if (!blocked.isEmpty()) {
            List<Map<String, String>> payload = new ArrayList<>(blocked.size());
            for (MultiDayLeaveDayPlan b : blocked) {
                Map<String, String> m = new LinkedHashMap<>();
                m.put("date", b.ymd());
                m.put("reasonCode", b.blockedReasonCode());
                m.put("reason", b.blockedReason());
                payload.add(m);
            }
            log.info("[leavemulti] 기간신청 거부(신청 불가일 포함) — userCd={}, 요청={}일, 차단={}일",
                    user, targets.size(), blocked.size());
            throw ApiException.withExtra(AttdErrorCode.ATTD_400_136, Map.of("blockedDates", payload));
        }

        // 잔여 부족은 전체 거부(정책 ③). 날짜별 배정 시뮬레이션 결과로 판정한다
        //   — 부여 유효기간 때문에 "총 N일 ≤ 잔여" 단순 비교로는 틀린다.
        MultiDayLeavePlanner.BalanceSim sim = planner.simulateBalance(cmpny, user, leaveCd, targets);
        if (sim.insufficient()) {
            log.info("[leavemulti] 기간신청 거부(잔여 부족) — userCd={}, leaveCd={}, 필요={}, 배정={}, 부족={}",
                    user, leaveCd, sim.neededDays().toPlainString(),
                    sim.assignedDays().toPlainString(), sim.shortageDays().toPlainString());
            throw ApiException.withExtra(AttdErrorCode.ATTD_400_051, Map.of(
                    "neededDays", sim.neededDays().toPlainString(),
                    "assignedDays", sim.assignedDays().toPlainString(),
                    "shortageDays", sim.shortageDays().toPlainString()));
        }

        // ---------- Phase 2 : 날짜별 생성 (단일 트랜잭션 — 하나라도 실패하면 전체 롤백) ----------
        String groupId = appLeaveFlowMapper.selectNextLeaveGroupId(cmpny);

        // 기준 param 을 만들고 날짜만 갈아끼운다. 종일 고정·가불 미사용은 deriveForDate 가 보장한다.
        LeaveApplyParam base = new LeaveApplyParam(
                leaveCd, leaveType, targets.get(0), UNIT_FULL_DAY,
                null, null, null,          // halfPart/startTime/endTime — 종일이라 미사용
                reason, approverUserCds, presetId,
                false,                     // isBorrow — 1차 범위 제외
                cmpny, site, user, groupId,
                evidenceFileId);           // 연차 신청 증빙 필수화(2026-08-29) — 날짜별 파생에 그대로 승계

        for (String ymd : targets) {
            // ★별도 빈(AppLeaveFlowService 프록시) 호출이라 REQUIRED 로 본 트랜잭션에 합류한다.
            //   실패 시 예외가 그대로 올라가 전체 롤백된다(정책 ② 전체 실패).
            appLeaveFlowService.submitLeave(base.deriveForDate(ymd, groupId));
        }

        log.info("[leavemulti] 기간신청 완료 — userCd={}, leaveCd={}, groupId={}, {}일 ({} ~ {})",
                user, leaveCd, groupId, targets.size(), targets.get(0), targets.get(targets.size() - 1));
        return groupId;
    }

    // ============================================================
    // helpers
    // ============================================================

    private void assertToken(TokenInfo t) {
        if (t == null
                || !StringUtils.hasText(t.gv_cmpnyCd())
                || !StringUtils.hasText(t.gv_siteCd())
                || !StringUtils.hasText(t.gv_userCd())) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
    }

    private boolean isYmd(String s) {
        if (s == null || s.length() != 8) {
            return false;
        }
        try {
            LocalDate.parse(s, YMD);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** 구간 상한 강제 — 재귀 CTE 폭주와 과도한 REQ 생성을 막는다. */
    private void assertRangeWithinLimit(String fromYmd, String toYmd) {
        long span = ChronoUnit.DAYS.between(LocalDate.parse(fromYmd, YMD), LocalDate.parse(toYmd, YMD)) + 1;
        if (span > MultiDayLeavePlanner.MAX_RANGE_DAYS) {
            throw new ApiException(AttdErrorCode.ATTD_400_137);
        }
    }

    /** BigDecimal null-safe 0. */
    @SuppressWarnings("unused")
    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
