package com.prafta.common.cmm.leave.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.leave.mapper.LeaveDashboardMapper;
import com.prafta.common.cmm.leave.mapper.LeaveRemnantCoverMapper;
import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.service.LeaveGrantEngineService;
import com.prafta.common.cmm.leave.service.LeaveRemnantCoverService;
import com.prafta.common.cmm.leave.vo.BorrowProjectionVO;
import com.prafta.common.cmm.leave.util.HourlyLeaveChargeUtils;
import com.prafta.common.cmm.leave.vo.RemnantCoverInsertVO;
import com.prafta.common.cmm.leave.vo.RemnantCoverListRowVO;
import com.prafta.common.cmm.leave.vo.RemnantCoverRowVO;
import com.prafta.common.cmm.leave.vo.RemnantCoverSummaryVO;
import com.prafta.common.cmm.leave.vo.RemnantDeductibleGrantVO;
import com.prafta.common.cmm.leave.vo.RemnantLeaveUseVO;
import com.prafta.common.cmm.leave.vo.RemnantPolicyVO;
import com.prafta.common.cmm.leave.vo.RemnantReportRowVO;
import com.prafta.common.cmm.leave.vo.RemnantReportVO;
import com.prafta.common.cmm.leave.vo.RemnantTriggerPlanVO;
import com.prafta.common.cmm.leave.vo.RemnantTriggerPlanVO.RemnantGrantChargeVO;
import com.prafta.common.cmm.leave.vo.RemnantUserRemainVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AdvisoryLockTxUtils;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link LeaveRemnantCoverService} 구현 (PC-05·06·07).
 *
 * <p>최소 사용단위 판정: 회사 USAGE_UNIT(단일 설정)의 SYS025 코드가 곧 허용 최소 단위다
 * (계층 SSOT 는 앱 모듈 {@code LeaveUnitGranularity} — common 이 앱 모듈을 참조하지 않도록
 * USAGE_UNIT→SYS025 매핑만 본 클래스에 미러한다. 값 변경 시 양쪽 동기화 주의).
 * 교대자(개인 분모 산출 불가)는 시간차(02/03/04)가 차단되므로 시간차 제외 후 최소 = 반차('01').
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveRemnantCoverServiceImpl implements LeaveRemnantCoverService {

    /** 발동 대상 법정 5종 (D4 — SYS_BIRTHDAY·비법정 제외, attd/08-leave §8.5.5). */
    private static final List<String> TARGET_LEAVE_CDS = List.of(
            "SYS_ANNUAL", "SYS_MONTHLY", "SYS_TENURE_BONUS", "SYS_PREGRANT", "SYS_PROMOTION");

    /** 절사 끝수 임계(§5-④ 확정): 잔여 &lt; 0.001 = "절사 끝수", 이상 = "실질 짜투리". */
    private static final BigDecimal ROUNDING_DUST_THRESHOLD = new BigDecimal("0.001");

    // 사용 단위 [SYS025]
    private static final String UNIT_FULL = "00";
    private static final String UNIT_HALF = "01";
    private static final String UNIT_HOUR2 = "02";
    private static final String UNIT_HOUR1 = "03";
    private static final String UNIT_MIN30 = "04";
    private static final String UNIT_QUARTER = "05";

    private static final String USE_CONFIRMED = "CONFIRMED";
    private static final String YN_Y = "Y";

    /** D7 회수 advisory lock 타임아웃(초) — leave01/leaveDay 관례 미러. */
    private static final int LOCK_TIMEOUT_SEC = 5;

    /** 회수 use 행 사유(감사 추적용 고정 문구). */
    private static final String RECLAIM_USE_REASON = "짜투리 보전 회수(정상 차감 전환)";

    /** T10(ⓕ): 1년 미만 월차 최대 발생 수(§8.5.4) — 가불 엔진 MONTHLY_MAX 와 동일 상수. */
    private static final int MONTHLY_MAX = 11;

    private final LeaveRemnantCoverMapper leaveRemnantCoverMapper;
    /** 리포트 행별 본인 분모(오늘 기준) — PC-03 단일 출처 재사용. */
    private final LeaveConversionPolicyService leaveConversionPolicyService;
    /** T10(ⓕ): 차기 본연차/근속가산 도래일 projection 재사용(산식 미러 복제 금지 — plan §1-T10). */
    private final LeaveGrantEngineService leaveGrantEngineService;
    /** T10(ⓕ): HIRE_DATE·경력인정 개월 단건 조회 재사용(기존 select — 신규 쿼리 없음). */
    private final LeaveDashboardMapper leaveDashboardMapper;

    // ============================================================
    // PC-05 — 발동 판정(D5) / 발동 처리(D6)
    // ============================================================

    @Override
    public RemnantTriggerPlanVO evaluateTrigger(String cmpnyCd, String userCd, String workYmd, String leaveCd,
                                                String useUnitType, Integer leaveMinutes, BigDecimal chargeDays,
                                                Integer personalConvMinutes) {
        if (cmpnyCd == null || userCd == null || workYmd == null || leaveCd == null || useUnitType == null
                || chargeDays == null || chargeDays.signum() <= 0) {
            return null;
        }
        // ⓑ-1: 대상 5종만(D4 — SYS_BIRTHDAY·비법정 제외).
        if (!TARGET_LEAVE_CDS.contains(leaveCd)) {
            return null;
        }
        // ⓐ: 옵션 ON(활성 정책). 정책 미존재는 발동 비대상(fail-closed).
        RemnantPolicyVO policy = leaveRemnantCoverMapper.selectRemnantPolicy(cmpnyCd);
        if (policy == null || !YN_Y.equals(policy.allowRemnantRoundUp())) {
            return null;
        }
        // ⓑ-2: 신청 단위 = 그 사용자의 최소 사용단위 "1건"(메인 세션 확정 — 더 큰 단위 신청 미발동).
        String minUnit = resolveMinUnit(policy.usageUnit(), personalConvMinutes == null);
        if (!minUnit.equals(useUnitType)) {
            return null;
        }
        if (isHourlyUnit(useUnitType)
                && (leaveMinutes == null || leaveMinutes != hourlyUnitMinutes(useUnitType))) {
            // 시간차 최소단위는 "정확히 1단위 분"만 발동(예: MIN_30 이면 30분 신청만).
            return null;
        }

        // ⓒ·ⓓ: 대상 5종 합산 잔여 (0 < 잔여 < 신청 요금). FOR UPDATE — submit 은 remnant lock 하에서
        //   호출되고(N9), preview 는 autocommit 이라 행 잠금이 즉시 해제된다(기존 preview 관례).
        List<RemnantDeductibleGrantVO> grants =
                leaveRemnantCoverMapper.selectRemnantDeductibleGrants(cmpnyCd, userCd, workYmd, TARGET_LEAVE_CDS);
        BigDecimal remaining = BigDecimal.ZERO;
        for (RemnantDeductibleGrantVO g : grants) {
            BigDecimal avail = nz(g.grantDays()).subtract(nz(g.usedDays()));
            if (avail.signum() > 0) {
                remaining = remaining.add(avail);
            }
        }
        if (remaining.compareTo(ROUNDING_DUST_THRESHOLD) < 0) {
            // ⓓ 잔여가 절사 끝수 수준(순수 반올림 잔재, §5-④ 리포트 기준과 동일 임계) — 보전 대상 아님.
            //   R2(DOWN 절사)로 누적된 미세 잔여가 여기서 걸러지지 않으면, 실질 잔여가 없는 사용자도
            //   "잔여>0"로 오인돼 회사부담 발동으로 새어나간다(리포트 경로는 이미 이 임계로 걸러냄).
            return null;
        }
        if (remaining.compareTo(chargeDays) >= 0) {
            return null; // ⓒ 잔여 충분 — 정상 차감 경로
        }
        // ⓔ: 미래 예정 연차 0건(실사용일 도래 기준 — D5 사용자 명시 요구).
        if (leaveRemnantCoverMapper.countUpcomingLeaveUse(cmpnyCd, userCd, todayYmd(), TARGET_LEAVE_CDS) > 0) {
            return null;
        }
        // ⓕ (T10 정책 개정, 2026-08-03 사용자 확정): "잔여가 소멸하기 전에 다음 대상 부여가 도래할
        //   예정이면 발동하지 않는다"(일반형 — SYS_MONTHLY 하드코딩 아님). 월차 11개는 만료일이
        //   전부 동일(입사+1년−1일 일괄소멸)해 마지막 월차 도래 전 구간의 잔여는 결합 사용이
        //   가능하므로 자투리가 아니다. 보수안: 잔여를 구성하는 "모든" 조각의 AVAIL_TO_DATE 가
        //   다음 도래일(nextGrantYmd) 이후에도 유효할 때만 스킵 — 하나라도 먼저 소멸하면 발동 유지.
        //   산출 불가/오류 시 발동 유지 폴백(스킵은 근로자 신청 거부로 이어지므로 불확실하면 기존 동작).
        String nextGrantYmd = resolveNextGrantYmd(cmpnyCd, userCd);
        if (nextGrantYmd != null) {
            int fragments = 0;
            String minAvailTo = null;
            boolean allSurvive = true;
            for (RemnantDeductibleGrantVO g : grants) {
                BigDecimal avail = nz(g.grantDays()).subtract(nz(g.usedDays()));
                if (avail.signum() <= 0) {
                    continue;
                }
                fragments++;
                String availTo = g.availToDate();
                if (minAvailTo == null || (availTo != null && availTo.compareTo(minAvailTo) < 0)) {
                    minAvailTo = availTo;
                }
                if (availTo == null || availTo.compareTo(nextGrantYmd) < 0) {
                    allSurvive = false; // 다음 도래 전에 소멸하는 조각 존재(진짜 자투리) — 발동 유지
                }
            }
            if (fragments > 0 && allSurvive) {
                log.info("[leave-remnant] 발동 스킵(ⓕ 결합 가능 잔여): userCd={}, workYmd={}, nextGrantYmd={}, "
                                + "잔여 조각={}개, 최소 만료일={}",
                        userCd, workYmd, nextGrantYmd, fragments, minAvailTo);
                return null;
            }
        }

        // 발동 계획: 잔여 전액을 만료 임박순 분할(원장 음수 금지 D6). 회사 부담분 = 정상 요금 − 잔여.
        int conv = (personalConvMinutes != null)
                ? personalConvMinutes : LeaveConversionPolicyService.DEFAULT_CONV_MINUTES;
        BigDecimal coverDays = chargeDays.subtract(remaining);
        int coverMinutes = toMinutes(coverDays, conv);
        List<RemnantGrantChargeVO> charges = allocate(grants, remaining);

        log.info("[leave-remnant] 짜투리 발동 판정 충족: userCd={}, workYmd={}, leaveCd={}, unit={}, "
                        + "요금={}, 잔여={}, 회사부담={}일({}분), conv={}",
                userCd, workYmd, leaveCd, useUnitType, chargeDays.toPlainString(),
                remaining.toPlainString(), coverDays.toPlainString(), coverMinutes, conv);

        return new RemnantTriggerPlanVO(chargeDays, remaining, coverDays, coverMinutes, conv, charges);
    }

    @Override
    public String applyTrigger(String cmpnyCd, String siteCd, String userCd, String workYmd, String useUnitType,
                               String startTime, String endTime, Integer leaveMinutes, String reason, String reqId,
                               RemnantTriggerPlanVO plan, String actorUserCd) {
        if (plan == null || plan.charges() == null || plan.charges().isEmpty()) {
            // 판정 계획 없이 호출된 비정상 — 잔여 부족 거부와 동일 처리(호출부 방어).
            throw new ApiException(AttdErrorCode.ATTD_400_051);
        }
        String leaveId = null;
        boolean first = true;
        for (RemnantGrantChargeVO charge : plan.charges()) {
            leaveId = leaveRemnantCoverMapper.selectNextLeaveId(cmpnyCd);
            leaveRemnantCoverMapper.insertLeaveUse(RemnantLeaveUseVO.builder()
                    .leaveId(leaveId).cmpnyCd(cmpnyCd).siteCd(siteCd).userCd(userCd)
                    .leaveCd(charge.leaveCd()) // 차감 GRANT 귀속 코드(자동 차감 선례 미러)
                    .reqId(reqId).grantId(charge.grantId())
                    .startDate(workYmd).startTime(startTime).endDate(workYmd).endTime(endTime)
                    .useUnitType(useUnitType).leaveDays(charge.days())
                    .leaveMinutes(first ? leaveMinutes : null) // 분할 규칙: 첫 행만(PC-01 REQ 합산 정합)
                    .leaveReason(reason).leaveStatus(USE_CONFIRMED).insertNo(actorUserCd)
                    .build());
            leaveRemnantCoverMapper.recomputeGrantUsedDays(cmpnyCd, charge.grantId(), actorUserCd);
            first = false;
        }

        String coverId = leaveRemnantCoverMapper.selectNextCoverId(cmpnyCd);
        leaveRemnantCoverMapper.insertCover(RemnantCoverInsertVO.builder()
                .coverId(coverId).cmpnyCd(cmpnyCd).siteCd(siteCd).userCd(userCd).reqId(reqId)
                .workYmd(workYmd).useUnitType(useUnitType)
                .chargeDays(plan.chargeDays()).remnantDays(plan.remnantDays())
                .coverDays(plan.coverDays()).coverMinutes(plan.coverMinutes())
                .convMinutes(plan.convMinutes()).insertNo(actorUserCd)
                .build());

        log.info("[leave-remnant] 짜투리 발동 처리 완료: coverId={}, reqId={}, userCd={}, workYmd={}, "
                        + "잔여 전액 {}일 차감({}행), 회사부담 {}일({}분)",
                coverId, reqId, userCd, workYmd, plan.remnantDays().toPlainString(),
                plan.charges().size(), plan.coverDays().toPlainString(), plan.coverMinutes());

        return leaveId;
    }

    /**
     * T10(ⓕ): 다음 도래 예정 대상 부여일(YYYYMMDD) 산출 — 월차/본연차(근속가산 포함) 두 계열의 최솟값.
     *
     * <ul>
     *   <li>월차: 실근속(경력인정 제외 — §8.5.4, 가불 슬롯 산식 createMonthlyBorrowGrant 동일 기준) 기준
     *       다음 슬롯 m = actualMonths+1 이 11 이하이면 hire+m개월. m &gt; 11 이면 도래 없음.
     *       경력인정 더블딥(월차 비대상): 실근속&lt;12 인데 산정근속(실근속+경력인정)&ge;12 이면 월차 계열을
     *       무시한다. ★2026-08-20 엔진 {@code isCreditDoubleDip} 이 같은 2조건으로 정정되어 <b>근사가 아니라
     *       정확히 일치</b>한다(종전에는 엔진이 full 본연차 발생까지 봐서 이쪽이 보수 근사였다).</li>
     *   <li>본연차/근속가산: {@code projectNextAnnualGrant} 재사용 — days &gt; 0 일 때만 availFromYmd 인정.</li>
     * </ul>
     *
     * <p>산출 불가(입사일 미상/형식 오류)·조회 실패 시 {@code null}(호출부 = 발동 유지 폴백 — 스킵은
     * 근로자 신청 거부로 이어지므로 불확실하면 기존 동작 유지, plan §1-T10 확정).
     */
    private String resolveNextGrantYmd(String cmpnyCd, String userCd) {
        try {
            String hireDate = leaveDashboardMapper.selectUserHireDate(cmpnyCd, userCd);
            if (hireDate == null || !hireDate.matches("\\d{8}")) {
                return null; // 입사일 미상 — 산출 불가(발동 유지 폴백)
            }
            LocalDate hire = LocalDate.parse(hireDate, DateTimeFormatter.BASIC_ISO_DATE);
            LocalDate today = LocalDate.now();
            if (hire.isAfter(today)) {
                return null;
            }

            // 월차 계열: 실근속 기준 다음 슬롯(가불 엔진 createMonthlyBorrowGrant 671행과 동일 기준).
            String monthlyNext = null;
            int actualMonths = (int) Math.max(0, ChronoUnit.MONTHS.between(hire, today));
            int nextSlot = actualMonths + 1;
            if (nextSlot <= MONTHLY_MAX) {
                // 더블딥 판정: 엔진 isCreditDoubleDip 과 동일한 2조건(실근속<12 AND 산정근속>=12).
                //   ★2026-08-20 엔진이 같은 기준으로 정정되어 근사가 아닌 정합 판정이 됐다(기준 일원화).
                int creditMonths = Math.max(0, leaveDashboardMapper.selectCreditMonths(cmpnyCd, userCd));
                if (actualMonths + creditMonths < 12) {
                    monthlyNext = hire.plusMonths(nextSlot).format(DateTimeFormatter.BASIC_ISO_DATE);
                }
            }

            // 본연차/근속가산 계열: days > 0 일 때만 도래 예정으로 인정.
            String annualNext = null;
            BorrowProjectionVO proj = leaveGrantEngineService.projectNextAnnualGrant(cmpnyCd, userCd, hireDate);
            if (proj != null && proj.getDays() != null && proj.getDays().signum() > 0
                    && proj.getAvailFromYmd() != null) {
                annualNext = proj.getAvailFromYmd();
            }

            if (monthlyNext == null) {
                return annualNext; // 두 계열 모두 없으면 null → ⓕ 통과(발동 유지)
            }
            if (annualNext == null) {
                return monthlyNext;
            }
            return (monthlyNext.compareTo(annualNext) <= 0) ? monthlyNext : annualNext;
        } catch (Exception e) {
            log.warn("[leave-remnant] ⓕ 다음 부여 도래일 산출 실패 — 발동 유지 폴백. cmpnyCd={}, userCd={}",
                    cmpnyCd, userCd, e);
            return null;
        }
    }

    // ============================================================
    // PC-06 — D7 회수
    // ============================================================

    @Override
    public void reclaimIfPossible(String cmpnyCd, String userCd, String actorUserCd) {
        if (cmpnyCd == null || cmpnyCd.isBlank() || userCd == null || userCd.isBlank()) {
            return;
        }
        // N9: 발동(신청)과 같은 사용자 단위 lock 으로 직렬화. 획득 순서는 leaveDay → leaveRemnant
        //   (재정산 훅 뒤에서 호출되므로 leaveDay lock 이 이미 잡힌 상태 — 순서 일관, 데드락 없음).
        String lockKey = LeaveRemnantCoverService.remnantLockKey(cmpnyCd, userCd);
        acquireLock(lockKey);
        boolean lockDeferred = AdvisoryLockTxUtils.deferReleaseToAfterCompletion(lockKey, this::releaseLock);
        try {
            List<RemnantCoverRowVO> covers = leaveRemnantCoverMapper.selectActiveCovers(cmpnyCd, userCd);
            if (covers.isEmpty()) {
                return;
            }
            String today = todayYmd();
            for (RemnantCoverRowVO c : covers) {
                // 도래(당일 포함) = 유지(D7 — 이미 쉼/쉬는 중인 휴가는 뒤집지 않는다, 메인 세션 확정).
                if (c.workYmd() == null || c.workYmd().compareTo(today) <= 0) {
                    continue;
                }
                if (c.coverDays() == null || c.coverDays().signum() <= 0) {
                    continue;
                }
                // 복원 잔여(대상 5종, 그 COVER 근무일 기준 유효 부여) — 부여별 소비창이 달라 건별 재조회.
                List<RemnantDeductibleGrantVO> grants = leaveRemnantCoverMapper
                        .selectRemnantDeductibleGrants(cmpnyCd, userCd, c.workYmd(), TARGET_LEAVE_CDS);
                BigDecimal restored = BigDecimal.ZERO;
                for (RemnantDeductibleGrantVO g : grants) {
                    BigDecimal avail = nz(g.grantDays()).subtract(nz(g.usedDays()));
                    if (avail.signum() > 0) {
                        restored = restored.add(avail);
                    }
                }
                if (restored.signum() <= 0) {
                    continue;
                }
                // 부분 회수 허용(메인 세션 확정): 복원 잔여 한도 내에서 정상 차감으로 전환.
                BigDecimal take = c.coverDays().min(restored);
                List<RemnantGrantChargeVO> charges = allocate(grants, take);
                for (RemnantGrantChargeVO charge : charges) {
                    String leaveId = leaveRemnantCoverMapper.selectNextLeaveId(cmpnyCd);
                    leaveRemnantCoverMapper.insertLeaveUse(RemnantLeaveUseVO.builder()
                            .leaveId(leaveId).cmpnyCd(cmpnyCd).siteCd(c.siteCd()).userCd(userCd)
                            .leaveCd(charge.leaveCd())
                            .reqId(c.reqId()).grantId(charge.grantId())
                            .startDate(c.workYmd()).startTime(null).endDate(c.workYmd()).endTime(null)
                            .useUnitType(c.useUnitType()).leaveDays(charge.days())
                            .leaveMinutes(null) // 회수 행은 항상 분 없음 — REQ 합산(PC-01) 분 중복 방지
                            .leaveReason(RECLAIM_USE_REASON).leaveStatus(USE_CONFIRMED).insertNo(actorUserCd)
                            .build());
                    leaveRemnantCoverMapper.recomputeGrantUsedDays(cmpnyCd, charge.grantId(), actorUserCd);
                }
                BigDecimal newCoverDays = c.coverDays().subtract(take);
                int conv = (c.convMinutes() != null)
                        ? c.convMinutes() : LeaveConversionPolicyService.DEFAULT_CONV_MINUTES;
                boolean reclaimed = newCoverDays.signum() == 0;
                leaveRemnantCoverMapper.updateCoverReclaim(cmpnyCd, c.coverId(), newCoverDays,
                        toMinutes(newCoverDays, conv), reclaimed, actorUserCd);

                log.info("[leave-remnant] 짜투리 회수: coverId={}, userCd={}, workYmd={}, 회수 {}일 "
                                + "(부담 {} → {}), 상태={}",
                        c.coverId(), userCd, c.workYmd(), take.toPlainString(),
                        c.coverDays().toPlainString(), newCoverDays.toPlainString(),
                        reclaimed ? "RECLAIMED" : "ACTIVE(부분 회수)");
            }
        } finally {
            // afterCompletion 등록 성공분은 여기서 해제하지 않는다(이중 해제 방지). 등록 실패 시에만 폴백.
            if (!lockDeferred) {
                releaseLock(lockKey);
            }
        }
    }

    // ============================================================
    // T1·T2 — 자기 cover 무효화(CANCELLED)
    // ============================================================

    @Override
    public int cancelCoversByReq(String cmpnyCd, String reqId, String actorUserCd) {
        if (cmpnyCd == null || cmpnyCd.isBlank() || reqId == null || reqId.isBlank()) {
            return 0;
        }
        // 회수 use INSERT 없이 상태만 CANCELLED — 원 use 행이 이미 취소(또는 곧 취소)되는 흐름 전용.
        //   반드시 reclaimIfPossible 호출 "전"에 수행해야 자기 cover 부활(plan §0-1-2)이 없다.
        int cancelled = leaveRemnantCoverMapper.cancelCoversByReqId(cmpnyCd, reqId, actorUserCd);
        if (cancelled > 0) {
            log.info("[leave-remnant] 자기 cover 무효화(CANCELLED): cmpnyCd={}, reqId={}, {}건, by={}",
                    cmpnyCd, reqId, cancelled, actorUserCd);
        }
        return cancelled;
    }

    // ============================================================
    // PC-07 — D9-② 집계 / D9-③ 리포트
    // ============================================================

    @Override
    public RemnantCoverSummaryVO getCoverSummary(String cmpnyCd, String authCd, String year) {
        ensureManager(cmpnyCd, authCd, "회사 부담 보전 집계 조회");
        String targetYear = (year != null && year.matches("\\d{4}"))
                ? year : String.valueOf(LocalDate.now().getYear());

        boolean policyOn = isPolicyOn(cmpnyCd);
        List<RemnantCoverListRowVO> rows = leaveRemnantCoverMapper.selectCoverSummaryRows(cmpnyCd, targetYear);
        BigDecimal total = BigDecimal.ZERO;
        for (RemnantCoverListRowVO r : rows) {
            total = total.add(nz(r.coverDays()));
        }

        log.info("[leave-remnant] 회사 부담 보전 집계 조회. cmpnyCd={}, year={}, 건수={}, 합계={}",
                cmpnyCd, targetYear, rows.size(), total.toPlainString());

        return new RemnantCoverSummaryVO(policyOn, targetYear, total, rows.size(), rows);
    }

    @Override
    public RemnantReportVO getRemnantReport(String cmpnyCd, String authCd) {
        ensureManager(cmpnyCd, authCd, "소멸 임박 짜투리 리포트 조회");

        RemnantPolicyVO policy = leaveRemnantCoverMapper.selectRemnantPolicy(cmpnyCd);
        boolean policyOn = policy != null && YN_Y.equals(policy.allowRemnantRoundUp());
        String usageUnit = (policy == null) ? null : policy.usageUnit();
        String today = todayYmd();

        List<RemnantUserRemainVO> remains =
                leaveRemnantCoverMapper.selectRemnantRemainByUser(cmpnyCd, today, TARGET_LEAVE_CDS);
        List<RemnantReportRowVO> rows = new ArrayList<>();
        for (RemnantUserRemainVO r : remains) {
            BigDecimal remnant = nz(r.remnantDays());
            if (remnant.signum() <= 0) {
                continue;
            }
            // M6·E4 참고치 규약(당일분모 전환 후 유지): 특정일 없는 배치성 판정(소멸 임박 리포트)은
            //   참고 분모(기본 근무타입 근사치, 미지정 480) 기준 — 실차감 분모(당일 배정 스케줄, E1)와
            //   편차 허용(사용자 확정 2026-08-03). 오늘 기준 conv, 미산출자는 시간차 제외 최소(반차).
            //   2026-08-09 표기 규약 변경: rows 의 convMinutes 는 FE 표기에 더 이상 사용되지 않음
            //   (additive 잔존 — 판정 로직 자체는 유지·불변).
            Integer conv = leaveConversionPolicyService.resolvePersonalConvMinutes(cmpnyCd, r.userCd(), today);
            String minUnit = resolveMinUnit(usageUnit, conv == null);
            int convOrDefault = (conv != null) ? conv : LeaveConversionPolicyService.DEFAULT_CONV_MINUTES;
            BigDecimal fee = minUnitChargeDays(minUnit, convOrDefault);
            if (fee.signum() <= 0 || remnant.compareTo(fee) >= 0) {
                continue; // 최소단위 요금 이상 = 신청으로 소진 가능 — 리포트 비대상
            }
            boolean dust = remnant.compareTo(ROUNDING_DUST_THRESHOLD) < 0;
            rows.add(new RemnantReportRowVO(r.userCd(), r.userNm(), remnant, minUnit, fee, dust,
                    r.nearestExpireYmd(), convOrDefault));
        }

        log.info("[leave-remnant] 소멸 임박 짜투리 리포트 조회. cmpnyCd={}, 후보={}, 대상={}",
                cmpnyCd, remains.size(), rows.size());

        return new RemnantReportVO(policyOn, rows);
    }

    // ============================================================
    // 내부 유틸
    // ============================================================

    /** 잔여/회수분을 만료 임박순 부여에 분할 배분(부여 잔여 상한, 합 = amount 보장 — 부족하면 가능한 만큼). */
    private List<RemnantGrantChargeVO> allocate(List<RemnantDeductibleGrantVO> grants, BigDecimal amount) {
        List<RemnantGrantChargeVO> charges = new ArrayList<>();
        BigDecimal remaining = amount;
        for (RemnantDeductibleGrantVO g : grants) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal avail = nz(g.grantDays()).subtract(nz(g.usedDays()));
            if (avail.signum() <= 0) {
                continue;
            }
            BigDecimal take = avail.min(remaining);
            charges.add(new RemnantGrantChargeVO(g.grantId(), g.leaveCd(), take));
            remaining = remaining.subtract(take);
        }
        return charges;
    }

    /**
     * 회사 USAGE_UNIT(단일 설정) → 그 사용자의 최소 사용단위(SYS025).
     * 설정 코드 자체가 허용 최소 단위(계층 SSOT = 앱 LeaveUnitGranularity — 매핑만 미러, 동기화 주의).
     * {@code hourlyBlocked}(분모 산출 불가 — E1 이후 발동 판정은 "신청 대상일 당일 분모" null(미배정일),
     * 배치성 리포트는 참고 분모 null)면 시간차 제외 후 최소 = 반차('01').
     */
    private String resolveMinUnit(String usageUnit, boolean hourlyBlocked) {
        String code;
        if (usageUnit == null) {
            code = UNIT_FULL;
        } else {
            code = switch (usageUnit) {
                case "FULL_DAY" -> UNIT_FULL;
                case "HALF_DAY" -> UNIT_HALF;
                // HB-04(2026-08-07): 반반차 폐지 — 구 설정은 반차로 축소 해석
                //   (LeaveUnitGranularity.USAGE_UNIT_TO_CODE / LeavePolicyServiceImpl 정규화와 동일 규약).
                case "QUARTER_DAY" -> UNIT_HALF;
                case "HOUR_2" -> UNIT_HOUR2;
                case "HOUR_1" -> UNIT_HOUR1;
                case "MIN_30" -> UNIT_MIN30;
                default -> UNIT_FULL; // 알 수 없는 값 안전 폴백(종일)
            };
        }
        if (hourlyBlocked && isHourlyUnit(code)) {
            // 시간차 설정 계층 [00,01,02..]에서 시간차 제외 후 최소 = 반차.
            return UNIT_HALF;
        }
        return code;
    }

    /** 최소단위 정상 요금(일). 고정단위=고정요금, 시간차=단위분 ÷ 본인 분모(DOWN 절사 — R2). */
    private BigDecimal minUnitChargeDays(String unit, int convMinutes) {
        return switch (unit) {
            case UNIT_FULL -> new BigDecimal("1.00000");
            case UNIT_HALF -> new BigDecimal("0.50000");
            case UNIT_QUARTER -> new BigDecimal("0.25000");
            default -> HourlyLeaveChargeUtils.rawDays(hourlyUnitMinutes(unit), convMinutes);
        };
    }

    private boolean isHourlyUnit(String unit) {
        return UNIT_HOUR2.equals(unit) || UNIT_HOUR1.equals(unit) || UNIT_MIN30.equals(unit);
    }

    private int hourlyUnitMinutes(String unit) {
        if (UNIT_HOUR2.equals(unit)) {
            return 120;
        }
        if (UNIT_HOUR1.equals(unit)) {
            return 60;
        }
        return 30; // UNIT_MIN30
    }

    /** 일수 × 분모(분) → 분(DOWN 절사, 표기·preview 용). */
    private int toMinutes(BigDecimal days, int convMinutes) {
        if (days == null || days.signum() <= 0) {
            return 0;
        }
        return days.multiply(BigDecimal.valueOf(convMinutes)).setScale(0, RoundingMode.DOWN).intValue();
    }

    private boolean isPolicyOn(String cmpnyCd) {
        RemnantPolicyVO policy = leaveRemnantCoverMapper.selectRemnantPolicy(cmpnyCd);
        return policy != null && YN_Y.equals(policy.allowRemnantRoundUp());
    }

    /** 관리자(MASTER/HR) 권한 가드(정책서 §8.5.7) — attd09 대시보드와 동일 기준·에러코드. */
    private void ensureManager(String cmpnyCd, String authCd, String action) {
        if (!AuthRoleUtils.isManager(authCd)) {
            log.warn("[leave-remnant] {} 권한 없음. cmpnyCd={}, authCd={}", action, cmpnyCd, authCd);
            throw new ApiException(AttdErrorCode.ATTD_403_020);
        }
    }

    /** D7 회수 advisory lock 획득 — 타임아웃/오류면 동시 처리로 보고 재시도 안내(재정산 관례 미러). */
    private void acquireLock(String lockKey) {
        Integer got = leaveRemnantCoverMapper.getAdvisoryLock(lockKey, LOCK_TIMEOUT_SEC);
        if (got == null || got != 1) {
            log.info("[leave-remnant] 회수 advisory lock 미획득 — lockKey={}, got={}", lockKey, got);
            throw new ApiException(AttdErrorCode.ATTD_409_071);
        }
    }

    /** advisory lock 해제(예외 무시 — 세션 종료 시 자동 해제됨). */
    private void releaseLock(String lockKey) {
        try {
            leaveRemnantCoverMapper.releaseAdvisoryLock(lockKey);
        } catch (Exception e) {
            log.warn("[leave-remnant] advisory lock 해제 실패(무시) — lockKey={}", lockKey, e);
        }
    }

    private String todayYmd() {
        return LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    /** null-safe BigDecimal(0 폴백). */
    private BigDecimal nz(BigDecimal v) {
        return (v == null) ? BigDecimal.ZERO : v;
    }
}
