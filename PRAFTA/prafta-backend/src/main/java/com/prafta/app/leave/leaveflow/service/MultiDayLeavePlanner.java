package com.prafta.app.leave.leaveflow.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.prafta.app.leave.leaveflow.mapper.AppLeaveFlowMapper;
import com.prafta.app.leave.leaveflow.result.MultiDayLeaveDayRow;
import com.prafta.app.leave.leaveflow.result.RangeGrantRow;
import com.prafta.app.leave.leaveflow.vo.MultiDayLeaveDayPlan;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.web.attd.attd07.service.AttdCloseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-leavemulti: 연차 기간(From-To) 신청의 <b>날짜별 판정 단일 출처</b>.
 *
 * <p>미리보기(GET)와 제출 Phase 1(POST) 이 <b>같은 코드</b>로 판정하게 해서
 * "미리보기는 통과했는데 제출은 실패" 를 구조적으로 막는다.
 *
 * <p>판정 술어는 전부 기존 단일일 신청 가드({@code AppLeaveFlowServiceImpl.submitLeave})를 미러한다:
 * <ul>
 *   <li>출근 기록 존재 → {@code ATTD_400_108}</li>
 *   <li>같은 날 하루 초과 중복 → {@code ATTD_400_111} (종일 1.0 추가 시 점유>0 이면 초과)</li>
 *   <li><b>과거일</b> + 그 월 마감 → {@code ATTD_400_050}
 *       (★ 미래일은 마감 검사를 하지 않는다 — 단일일 가드가 {@code workYmd < today} 일 때만 보기 때문.
 *        여기서 미래일까지 막으면 미리보기가 실제보다 더 엄격해져 정상 신청을 차단한다)</li>
 * </ul>
 *
 * <p>본 컴포넌트는 조회·계산만 하며 상태를 바꾸지 않는다(행 잠금도 잡지 않는다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MultiDayLeavePlanner {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 기간 신청 1회의 최대 구간 길이(일). 재귀 CTE 폭주·과도한 REQ 생성 방지. */
    public static final int MAX_RANGE_DAYS = 62;

    /** 종일 연차 1일 차감량. */
    private static final BigDecimal ONE_DAY = BigDecimal.ONE;

    private final AppLeaveFlowMapper appLeaveFlowMapper;
    private final AttdCloseService attdCloseService;

    // ============================================================
    // 1) 구간 날짜별 판정 (미리보기)
    // ============================================================

    /**
     * 구간의 날짜별 선택 가능 여부·기본 체크 상태를 계산한다.
     *
     * <p><b>기본 체크 규칙 (3단)</b>
     * <ol>
     *   <li>그날 근무계획(SCH)이 있으면 기본 체크 → 주말 근무자도 토·일이 자동 포함된다</li>
     *   <li>근무계획이 없으면 기본 해제 (단 <b>선택은 가능</b>)</li>
     *   <li>구간 전체에 근무계획이 하나도 없으면 달력 기준(주말·공휴일만 해제)으로 폴백
     *       — 근무계획 미보유 사용자가 전부 기본 해제가 되어 기능이 무용지물이 되는 것을 막는다</li>
     * </ol>
     */
    public List<MultiDayLeaveDayPlan> planDays(String cmpnyCd, String siteCd, String userCd,
                                               String fromYmd, String toYmd) {

        List<MultiDayLeaveDayRow> rows =
                appLeaveFlowMapper.selectMultiDayLeaveDayRows(cmpnyCd, siteCd, userCd, fromYmd, toYmd);
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }

        // 3단 규칙의 폴백 판정 — 구간 전체에 근무계획이 하나도 없는가.
        boolean anySchedule = rows.stream().anyMatch(r -> "Y".equals(r.hasScheduleYn()));

        String today = LocalDate.now().format(YMD);
        // 마감 조회는 월 단위이므로 같은 월을 반복 조회하지 않도록 캐시한다(구간이 걸쳐도 보통 2~3개월).
        Map<String, Boolean> closedCache = new HashMap<>();

        List<MultiDayLeaveDayPlan> plans = new ArrayList<>(rows.size());
        for (MultiDayLeaveDayRow r : rows) {
            String ymd = r.ymd();
            boolean weekend = "Y".equals(r.weekendYn());
            boolean holiday = "Y".equals(r.holidayYn());
            boolean hasSchedule = "Y".equals(r.hasScheduleYn());

            String blockedCode = null;
            // ① 출근 기록 존재 (단일일 가드 ATTD_400_108 미러)
            if (r.attdCnt() > 0) {
                blockedCode = AttdErrorCode.ATTD_400_108.name();
            }
            // ② 같은 날 하루 초과 중복 — 종일(1.0) 추가이므로 점유가 조금이라도 있으면 초과다
            //    (단일일 가드: 점유 + 신규 > 1.0 → 거부)
            else if (r.occupiedDays() != null && r.occupiedDays().signum() > 0) {
                blockedCode = AttdErrorCode.ATTD_400_111.name();
            }
            // ③ 과거일 + 그 월 마감 (★미래일은 검사하지 않는다 — 단일일 가드와 동일)
            else if (ymd.compareTo(today) < 0 && isClosed(closedCache, cmpnyCd, siteCd, userCd, ymd)) {
                blockedCode = AttdErrorCode.ATTD_400_050.name();
            }

            boolean selectable = (blockedCode == null);
            // 기본 체크: 선택 가능한 날 중에서만. 근무계획 기준, 없으면(구간 전체 무계획) 달력 폴백.
            boolean defaultChecked = selectable
                    && (anySchedule ? hasSchedule : !(weekend || holiday));

            plans.add(new MultiDayLeaveDayPlan(
                    ymd, r.dow(), weekend, holiday, hasSchedule,
                    defaultChecked, selectable,
                    blockedCode, blockedCode == null ? null : messageOf(blockedCode)));
        }
        return plans;
    }

    // ============================================================
    // 2) 선택 날짜 재검증 (제출 Phase 1)
    // ============================================================

    /**
     * 제출된 날짜 목록을 전부 검증해 <b>막힌 날짜를 모두</b> 반환한다(빈 리스트 = 전부 통과).
     *
     * <p>★첫 실패에서 중단하지 않는다. 하나씩 알려주면 사용자가 "고침 → 재제출"을 반복하게 되므로,
     * 한 번에 전부 보여주기 위해 끝까지 수집한다(정책 ② 전체 실패 + 사유 일괄 반환).
     */
    public List<MultiDayLeaveDayPlan> findBlocked(String cmpnyCd, String siteCd, String userCd,
                                                  List<String> dates) {
        if (dates == null || dates.isEmpty()) {
            return List.of();
        }
        List<String> sorted = new ArrayList<>(new LinkedHashSet<>(dates));
        sorted.sort(String::compareTo);
        // 선택 날짜가 구간 안에서 띄엄띄엄일 수 있으므로 min~max 로 조회한 뒤 선택분만 본다.
        List<MultiDayLeaveDayPlan> all =
                planDays(cmpnyCd, siteCd, userCd, sorted.get(0), sorted.get(sorted.size() - 1));

        Map<String, MultiDayLeaveDayPlan> byYmd = new HashMap<>();
        all.forEach(p -> byYmd.put(p.ymd(), p));

        List<MultiDayLeaveDayPlan> blocked = new ArrayList<>();
        for (String ymd : sorted) {
            MultiDayLeaveDayPlan p = byYmd.get(ymd);
            if (p != null && !p.selectable()) {
                blocked.add(p);
            }
        }
        return blocked;
    }

    // ============================================================
    // 3) 잔여 배정 시뮬레이션
    // ============================================================

    /** 잔여 시뮬레이션 결과. {@code shortageDays > 0} 이면 부족(전체 거부 대상). */
    public record BalanceSim(BigDecimal neededDays, BigDecimal assignedDays, BigDecimal shortageDays) {
        public boolean insufficient() {
            return shortageDays != null && shortageDays.signum() > 0;
        }
    }

    /**
     * 선택 날짜들에 대해 <b>날짜 오름차순 그리디 배정</b>을 시뮬레이션한다.
     *
     * <p>잔여는 날짜마다 다르다(부여의 {@code AVAIL_FROM_DATE ~ AVAIL_TO_DATE}). 따라서
     * "총 N일 ≤ 잔여" 단순 비교는 양방향으로 틀린다 — 앞 날짜에만 쓸 수 있는 만료 임박 부여를
     * 과대(뒤 날짜엔 못 씀) 또는 과소(앞 날짜엔 쓸 수 있음) 계상한다.
     *
     * <p>배정 규칙은 실제 차감({@code resolveGeneralCharges})과 동일하다:
     * 만료 임박순으로 앞에서부터 {@code min(부여 잔여, 필요분)} 을 가져간다.
     * — 미리보기 판정과 실제 차감이 갈리지 않게 하는 것이 핵심이다.
     */
    public BalanceSim simulateBalance(String cmpnyCd, String userCd, String leaveCd, List<String> dates) {
        if (dates == null || dates.isEmpty()) {
            return new BalanceSim(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        List<String> sorted = new ArrayList<>(new LinkedHashSet<>(dates));
        sorted.sort(String::compareTo);

        List<RangeGrantRow> grants = appLeaveFlowMapper.selectDeductibleGrantsForRange(
                cmpnyCd, userCd, leaveCd, sorted.get(0), sorted.get(sorted.size() - 1));
        if (grants == null) {
            grants = List.of();
        }
        // 부여별 잔여를 시뮬레이션 동안 차감해 간다(DB 미변경 — 계산 전용).
        Map<String, BigDecimal> left = new HashMap<>();
        for (RangeGrantRow g : grants) {
            left.put(g.grantId(), g.remaining());
        }

        BigDecimal needed = BigDecimal.ZERO;
        BigDecimal assigned = BigDecimal.ZERO;
        for (String ymd : sorted) {
            needed = needed.add(ONE_DAY);
            BigDecimal need = ONE_DAY;
            for (RangeGrantRow g : grants) {          // 이미 만료 임박순 정렬
                if (need.signum() <= 0) {
                    break;
                }
                if (!g.usableOn(ymd)) {
                    continue;                          // 그 날짜엔 못 쓰는 부여
                }
                BigDecimal avail = left.getOrDefault(g.grantId(), BigDecimal.ZERO);
                if (avail.signum() <= 0) {
                    continue;
                }
                BigDecimal take = avail.min(need);
                left.put(g.grantId(), avail.subtract(take));
                need = need.subtract(take);
                assigned = assigned.add(take);
            }
        }
        BigDecimal shortage = needed.subtract(assigned);
        if (shortage.signum() > 0) {
            log.info("[leavemulti] 잔여 배정 부족 — userCd={}, leaveCd={}, 필요={}, 배정={}, 부족={}",
                    userCd, leaveCd, needed.toPlainString(), assigned.toPlainString(), shortage.toPlainString());
        }
        return new BalanceSim(needed, assigned, shortage);
    }

    // ============================================================
    // helpers
    // ============================================================

    private boolean isClosed(Map<String, Boolean> cache, String cmpnyCd, String siteCd,
                             String userCd, String ymd) {
        String closeYm = ymd.substring(0, 6);
        return cache.computeIfAbsent(closeYm,
                ym -> attdCloseService.isClosedForUser(cmpnyCd, siteCd, userCd, ym));
    }

    /** 사유 코드 → 사용자 문구. 기존 에러코드 메시지를 그대로 쓴다(문구 이원화 방지). */
    private String messageOf(String code) {
        try {
            return AttdErrorCode.valueOf(code).message();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
