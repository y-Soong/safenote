package com.prafta.app.leave.leave01.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prafta.app.leave.leave01.application.param.MyLeaveSummaryParam;
import com.prafta.app.leave.leave01.application.param.MyLeaveUseListParam;
import com.prafta.app.leave.leave01.application.query.MyLeaveSummaryQuery;
import com.prafta.app.leave.leave01.dto.response.MyLeaveSummaryResponse;
import com.prafta.app.leave.leave01.dto.response.MyLeaveUseListResponse;
import com.prafta.app.leave.leave01.mapper.AppLeave01Mapper;
import com.prafta.app.leave.leave01.result.AppliedLeaveTypeRow;
import com.prafta.app.leave.leave01.result.HourlyUsedSplitRow;
import com.prafta.app.leave.leave01.result.LeaveExpiringResult;
import com.prafta.app.leave.leave01.result.LeaveGroupAggResult;
import com.prafta.app.leave.leave01.result.LeaveUserResult;
import com.prafta.app.leave.leave01.service.AppLeave01Service;
import com.prafta.common.cmm.leave.mapper.LeavePolicyMapper;
import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.util.FiscalYearUtils;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-005: 앱 "연차 현황" 서비스 구현.
 * <p>읽기 전용. 그룹 3종(TOTAL/STATUTORY/NON_STATUTORY)을 동일 SQL 에 prefix 만 바꿔 호출하고,
 * 소멸임박/사용자 메타를 합쳐 단일 응답으로 묶는다. 식별값은 토큰 기반(param)이며 클라 입력을 사용하지 않는다.
 * <p>수치 정의(decisions/plan §1-3, D-Q5):
 * <pre>
 *   used      = usedTotal - planned
 *   remaining = granted - usedTotal
 *   usageRate = (granted==0)?0:round(usedTotal / granted * 100)   // 분자 usedTotal, 그룹별
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppLeave01ServiceImpl implements AppLeave01Service {

    // 그룹 LIKE prefix (언더스코어 '\' 이스케이프 — 웹 LeaveDashboardMapper 동일 규칙)
    private static final String PREFIX_STATUTORY = "STATUTORY\\_%";
    private static final String PREFIX_MANUAL = "MANUAL\\_%";

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 연차 원장 일수 컬럼 최대 스케일(TB_USER_LEAVE_USE.LEAVE_DAYS / TB_USER_LEAVE_GRANT.USED_DAYS = decimal(8,5)).
    // 응답 일수 수치를 이 스케일로 전달해 시간차 사용분(예 0.075일)을 손실 없이 앱에 넘긴다(반올림 오표기 제거).
    private static final int LEDGER_DECIMAL_SCALE = 5;

    private final AppLeave01Mapper appLeave01Mapper;
    /** 연차 개편(표시): 활성정책 AXIS2_FISCAL_START_MM/_DD 조회(회계연도 경계 산출 입력). 락 없는 조회 메서드 재사용. */
    private final LeavePolicyMapper leavePolicyMapper;
    /** LC-07(표기): 현재 기준 1일 환산시간(분) 조회 — FE "N일 H시간 M분" 조립 분모 단일 출처. */
    private final LeaveConversionPolicyService leaveConversionPolicyService;

    @Override
    public MyLeaveSummaryResponse selectMyLeaveSummary(MyLeaveSummaryParam param) {

        // 그룹/예정/소멸 쿼리가 동일 기준일을 공유하도록 DB 기준 오늘(YYYYMMDD)을 1회 조회
        String todayYmd = appLeave01Mapper.selectTodayYmd();
        MyLeaveSummaryQuery baseQuery = MyLeaveSummaryQuery.from(param, todayYmd);

        log.info("[leave01] 연차 현황 조회 시작 userCd={}, today={}", param.userCd(), todayYmd);

        // LC-07(표기): 오늘 기준 환산시간 + 시간차 사용 분 합계(전 기간) — 기존 필드 불변, additive.
        // HB-13(F-3): 같은 합계를 "사용 / 사용예정"으로 분리해 함께 내린다. FE 가 일수→시간을 단일
        //   분모로 역환산하던 것을 실분 표기로 대체하기 위함(당일분모 전환 E1 이 만든 표시 결함).
        //   전 기간 합계(hourlyUsedMinutes, 구 앱 호환)는 분리 결과의 합으로 산출해 쿼리 1회로 줄인다.
        HourlyUsedSplitRow hourlySplit =
                appLeave01Mapper.selectHourlyUsedMinutesSplit(param.cmpnyCd(), param.userCd());
        int hourlyPastMinutes = (hourlySplit == null) ? 0 : hourlySplit.pastMinutes();
        int hourlyPlannedMinutes = (hourlySplit == null) ? 0 : hourlySplit.plannedMinutes();
        int hourlyUsedMinutes = hourlyPastMinutes + hourlyPlannedMinutes;

        // E4 참고치 규약(당일분모 전환 후 유지): convMinutes = 오늘 기준 본인 참고 분모(기본 근무타입
        //   근사치, 480 캡). 특정일 없는 잔여 카드 표기 전용 — 실차감 분모(당일 배정 스케줄, E1)와
        //   편차 허용(사용자 확정 2026-08-03). 산출 불가면 480 폴백(FE formatLeaveDays 폴백과 정합).
        Integer personalConv = leaveConversionPolicyService.resolvePersonalConvMinutes(
                param.cmpnyCd(), param.userCd(), todayYmd);

        MyLeaveSummaryResponse response = MyLeaveSummaryResponse.builder()
                .user(buildUser(baseQuery))
                .groups(buildGroups(baseQuery))
                .expiringSoon(buildExpiringSoon(baseQuery))
                .appliedLeaveTypes(buildAppliedLeaveTypes(param))
                .borrowedDays(toScaledDouble(nz(
                        appLeave01Mapper.selectBorrowedDaysTotal(param.cmpnyCd(), param.userCd(), todayYmd))))
                .convMinutes(personalConv != null ? personalConv : LeaveConversionPolicyService.DEFAULT_CONV_MINUTES)
                .hourlyUsedMinutes(hourlyUsedMinutes)
                .hourlyUsedMinutesPast(hourlyPastMinutes)
                .hourlyUsedMinutesPlanned(hourlyPlannedMinutes)
                .build();

        log.info("[leave01] 연차 현황 조회 완료 userCd={}", param.userCd());

        return response;
    }

    @Override
    public MyLeaveUseListResponse selectMyLeaveUses(MyLeaveUseListParam param) {

        // year 미지정이면 DB 기준 올해로 보정(클라이언트 시계 불신 — 기준일 단일 출처 유지).
        String year = param.year();
        if (!StringUtils.hasText(year)) {
            year = appLeave01Mapper.selectTodayYmd().substring(0, 4);
        }

        List<com.prafta.app.leave.leave01.result.LeaveUseHistoryRow> list =
                appLeave01Mapper.selectLeaveUsesByRange(
                        param.cmpnyCd(), param.userCd(), year + "0101", year + "1231");

        log.info("[leave01] 연차 사용 내역 조회 userCd={}, year={}, count={}",
                param.userCd(), year, list == null ? 0 : list.size());

        return MyLeaveUseListResponse.builder()
                .year(year)
                .list(list == null ? List.of() : list)
                .build();
    }

    /**
     * 그룹 3종 산출. TOTAL 은 prefix 무관(null), STATUTORY/NON_STATUTORY 는 각 prefix.
     */
    private MyLeaveSummaryResponse.Groups buildGroups(MyLeaveSummaryQuery baseQuery) {

        return MyLeaveSummaryResponse.Groups.builder()
                .TOTAL(buildGroup(baseQuery.withGrantTypePrefix(null)))
                .STATUTORY(buildGroup(baseQuery.withGrantTypePrefix(PREFIX_STATUTORY)))
                .NON_STATUTORY(buildGroup(baseQuery.withGrantTypePrefix(PREFIX_MANUAL)))
                .build();
    }

    /**
     * 단일 그룹 수치 산출(BigDecimal 기준으로 파생값 계산 후 소수1자리 반올림).
     */
    private MyLeaveSummaryResponse.Group buildGroup(MyLeaveSummaryQuery query) {

        LeaveGroupAggResult agg = appLeave01Mapper.selectGroupAgg(query);

        BigDecimal granted = nz(agg != null ? agg.granted() : null);
        BigDecimal usedTotal = nz(agg != null ? agg.usedTotal() : null);
        BigDecimal planned = nz(agg != null ? agg.planned() : null);

        BigDecimal used = usedTotal.subtract(planned);
        BigDecimal remaining = granted.subtract(usedTotal);

        int usageRate = (granted.signum() == 0)
                ? 0
                : usedTotal.multiply(BigDecimal.valueOf(100))
                        .divide(granted, 0, RoundingMode.HALF_UP)
                        .intValue();

        return MyLeaveSummaryResponse.Group.builder()
                .granted(toScaledDouble(granted))
                .used(toScaledDouble(used))
                .planned(toScaledDouble(planned))
                .remaining(toScaledDouble(remaining))
                .usageRate(usageRate)
                .build();
    }

    /**
     * 소멸 임박(D-30) 산출. 대상 0건이면 exists=false / 0 폴백.
     */
    private MyLeaveSummaryResponse.ExpiringSoon buildExpiringSoon(MyLeaveSummaryQuery baseQuery) {

        LeaveExpiringResult exp = appLeave01Mapper.selectExpiringSoon(baseQuery);

        boolean exists = exp != null && exp.targetCount() > 0;

        if (!exists) {
            return MyLeaveSummaryResponse.ExpiringSoon.builder()
                    .exists(false)
                    .daysUntilExpiry(0)
                    .totalRemainingDays(0.0)
                    .expiryDate(null)
                    .build();
        }

        int daysUntilExpiry = (exp.daysUntilExpiry() != null) ? exp.daysUntilExpiry() : 0;

        return MyLeaveSummaryResponse.ExpiringSoon.builder()
                .exists(true)
                .daysUntilExpiry(daysUntilExpiry)
                .totalRemainingDays(toScaledDouble(nz(exp.totalRemainingDays())))
                .expiryDate(exp.nearestExpiryYmd())
                .build();
    }

    /**
     * 사용자 메타 산출. userNm/hireDate/serviceCreditMonths + 서버 계산 serviceMonths(실근속).
     */
    private MyLeaveSummaryResponse.User buildUser(MyLeaveSummaryQuery baseQuery) {

        LeaveUserResult user = appLeave01Mapper.selectUser(baseQuery);

        String userNm = (user != null) ? user.userNm() : null;
        String hireDate = (user != null) ? user.hireDate() : null;
        int serviceCreditMonths = (user != null) ? user.serviceCreditMonths() : 0;
        int serviceMonths = calcServiceMonths(hireDate, baseQuery.todayYmd());

        return MyLeaveSummaryResponse.User.builder()
                .userNm(userNm)
                .hireDate(hireDate)
                .serviceMonths(serviceMonths)
                .serviceCreditMonths(serviceCreditMonths)
                .build();
    }

    /**
     * 연차 개편(표시): 신청형 휴가('01') 타입별 항목 산출.
     * <p>법정/관리자부여(groups)와 분리된 별도 섹션. 회계연도 경계는 단일출처 {@link FiscalYearUtils} 로 산출하여
     *   사용분 술어(CONFIRMED·DEL_YN='N'·당해 회계연도)와 함께 주입한다(leaveflow.selectFiscalUsedDays 동일 술어).
     *   각 타입: 한도(MAX_APLY_DAYS, NULL→0 fail-closed) - 사용분 = 잔여. '01' 타입 0개면 빈 리스트.
     */
    private List<MyLeaveSummaryResponse.AppliedLeaveType> buildAppliedLeaveTypes(MyLeaveSummaryParam param) {

        FiscalYearUtils.FiscalWindow fiscal = resolveFiscalWindow(param.cmpnyCd());

        List<AppliedLeaveTypeRow> rows = appLeave01Mapper.selectAppliedLeaveTypes(
                param.cmpnyCd(), param.userCd(),
                fiscal.fiscalStartYmd(), fiscal.fiscalEndYmdExclusive());

        List<MyLeaveSummaryResponse.AppliedLeaveType> items = new ArrayList<>(rows.size());
        for (AppliedLeaveTypeRow row : rows) {
            // 한도 NULL → 0(신청불가 = 잔여 0, fail-closed). 사용분 NULL → 0(IFNULL 로 SQL 에서 0 이지만 방어).
            BigDecimal max = (row.maxAplyDays() == null) ? BigDecimal.ZERO : BigDecimal.valueOf(row.maxAplyDays());
            BigDecimal used = nz(row.usedDays());
            BigDecimal remain = max.subtract(used);

            items.add(MyLeaveSummaryResponse.AppliedLeaveType.builder()
                    .leaveCd(row.leaveCd())
                    .leaveNm(row.leaveNm())
                    .maxAplyDays(toScaledDouble(max))
                    .usedDays(toScaledDouble(used))
                    .remainDays(toScaledDouble(remain))
                    .build());
        }

        log.info("[leave01] 신청형 휴가('01') 항목 산출 완료 userCd={}, 타입수={}", param.userCd(), items.size());
        return items;
    }

    /**
     * 연차 개편(표시): 당해 회계연도 윈도우 산출(단일출처 {@link FiscalYearUtils}).
     * 활성정책 AXIS2_FISCAL_START_MM/_DD 로 산출하며, 정책 미존재/NULL 이면 1월 1일 폴백(유틸이 처리).
     * (leaveflow.AppLeaveFlowServiceImpl.resolveFiscalWindow 와 동일 — 신청 화면 잔여와 동일 경계 보장.)
     */
    private FiscalYearUtils.FiscalWindow resolveFiscalWindow(String cmpnyCd) {
        LeavePolicyVO policy = leavePolicyMapper.selectActivePolicy(cmpnyCd);
        String mm = (policy == null) ? null : policy.getAxis2FiscalStartMm();
        String dd = (policy == null) ? null : policy.getAxis2FiscalStartDd();
        return FiscalYearUtils.fiscalWindow(LocalDate.now(), mm, dd);
    }

    /**
     * 실근속 개월수 = 입사일~오늘(YYYYMMDD) 사이 완전 경과 개월수(경력 미포함).
     * hireDate 가 없거나 파싱 불가/미래면 0.
     */
    private int calcServiceMonths(String hireYmd, String todayYmd) {

        if (!StringUtils.hasText(hireYmd) || !StringUtils.hasText(todayYmd)) {
            return 0;
        }

        try {
            LocalDate hire = LocalDate.parse(hireYmd, YMD);
            LocalDate today = LocalDate.parse(todayYmd, YMD);
            if (hire.isAfter(today)) {
                return 0;
            }
            long months = ChronoUnit.MONTHS.between(hire, today);
            return (int) months;
        } catch (DateTimeParseException e) {
            // 비정상 입사일 포맷은 0 으로 폴백(원본값 로그 금지 — PII/비정형)
            log.warn("[leave01] 입사일 파싱 실패로 근속 0 처리");
            return 0;
        }
    }

    /**
     * null → BigDecimal.ZERO.
     */
    private BigDecimal nz(BigDecimal value) {
        return (value == null) ? BigDecimal.ZERO : value;
    }

    /**
     * BigDecimal → double (null=0.0).
     *
     * <p>연차 원장 정밀도(TB_USER_LEAVE_USE.LEAVE_DAYS / TB_USER_LEAVE_GRANT.USED_DAYS
     * = decimal(8,5), GRANT_DAYS = decimal(5,2))를 그대로 전달한다. 여기서 처리하는 값은
     * granted/used/planned/remaining/borrowedDays/totalRemainingDays/appliedLeaveTypes 로
     * 전부 원장 컬럼의 가감산 결과라 소수 최대 5자리이다.</p>
     *
     * <p>과거 소수 1자리 반올림(setScale(1))이 시간차 사용분을 왜곡해(예 1.075일 → 1.1일)
     * 앱 분(分) 환산 시 "1일 40분"(실제 "1일 30분")으로 오표기되던 결함을 제거한다.
     * 원장 스케일(5)로 맞추면 손실 없이 전달되고 앱 leaveFormat 이 정확히 "N일 H시간 M분"으로 표기한다.</p>
     */
    private double toScaledDouble(BigDecimal value) {
        if (value == null) {
            return 0.0;
        }
        // 원장 최대 스케일(decimal(8,5)) 로 맞춤 — 반올림 없이 원값 그대로 전달.
        return value.setScale(LEDGER_DECIMAL_SCALE, RoundingMode.HALF_UP).doubleValue();
    }
}
