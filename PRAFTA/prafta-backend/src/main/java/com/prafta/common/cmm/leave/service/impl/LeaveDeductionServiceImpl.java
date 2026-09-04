package com.prafta.common.cmm.leave.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.leave.mapper.LeaveDeductionMapper;
import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.service.LeaveDeductionService;
import com.prafta.common.cmm.leave.util.HourlyLeaveChargeUtils;
import com.prafta.common.cmm.leave.util.PartialLeaveWindowUtils;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils;
import com.prafta.common.cmm.leave.vo.DailyScheduleVO;
import com.prafta.common.cmm.leave.vo.HourlyChargeVO;
import com.prafta.common.cmm.leave.vo.HourlyLeaveAggVO;
import com.prafta.common.cmm.leave.vo.LeaveTimeWindowVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.DateTimeUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link LeaveDeductionService} 구현 (prafta-019-A §2.4 → 연차 시간차 환산 개편 LC-03).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.1.1, §8.5.9
 * / 작업지시서_연차-시간차-환산-개편 §2(R1~R4)·F3
 */
@Slf4j
@Service
public class LeaveDeductionServiceImpl implements LeaveDeductionService {

    /** 하루(분). 야간 구간(종료 ≤ 시작) 보정에 사용. */
    private static final int MINUTES_PER_DAY = 1440;

    private final LeaveDeductionMapper leaveDeductionMapper;

    public LeaveDeductionServiceImpl(LeaveDeductionMapper leaveDeductionMapper) {
        this.leaveDeductionMapper = leaveDeductionMapper;
    }

    @Override
    public Integer getDailyStdWorkMinutes(String cmpnyCd, String siteCd, String userCd, String workYmd) {
        if (cmpnyCd == null || siteCd == null || userCd == null || workYmd == null) {
            return null;
        }

        DailyScheduleVO sch = leaveDeductionMapper.selectDailySchedule(cmpnyCd, siteCd, userCd, workYmd);
        if (sch == null) {
            // 근무 계획/스케줄 없음 (연차일 등) → 1일 소정근로분 산출 불가
            return null;
        }

        // PC-03: 구간 합산 산식은 ScheduleWorkMinutesUtils 로 공용 추출(개인 분모 산출과 단일 출처).
        Integer total = ScheduleWorkMinutesUtils.dailyStdWorkMinutes(sch);
        if (total == null) {
            log.warn("1일 소정근로분 계산 실패 - 스케줄 시각 비정상: cmpnyCd={}, siteCd={}, userCd={}, workYmd={}, schCd={}",
                    cmpnyCd, siteCd, userCd, workYmd, sch.getSchCd());
        }
        return total;
    }

    @Override
    public ScheduleWorkMinutesUtils.HalfDayBoundary getHalfDayBoundary(String cmpnyCd, String siteCd,
                                                                      String userCd, String workYmd) {
        if (cmpnyCd == null || siteCd == null || userCd == null || workYmd == null) {
            return null;
        }
        DailyScheduleVO sch = leaveDeductionMapper.selectDailySchedule(cmpnyCd, siteCd, userCd, workYmd);
        if (sch == null) {
            // 근무 계획/스케줄 없음 → 반차 경계 산출 불가(호출부가 ATTD_400_110 으로 거부).
            return null;
        }
        ScheduleWorkMinutesUtils.HalfDayBoundary boundary = ScheduleWorkMinutesUtils.halfDayBoundary(sch);
        if (boundary == null) {
            log.warn("반차 경계 계산 실패 - 스케줄 시각 비정상: cmpnyCd={}, siteCd={}, userCd={}, workYmd={}, schCd={}",
                    cmpnyCd, siteCd, userCd, workYmd, sch.getSchCd());
        }
        return boundary;
    }

    @Override
    public ScheduleWorkMinutesUtils.HalfDayBoundary getHalfDayBoundary(String cmpnyCd, String siteCd,
                                                                      String userCd, String workYmd,
                                                                      ScheduleWorkMinutesUtils.HalfPart part,
                                                                      boolean waive) {
        if (cmpnyCd == null || siteCd == null || userCd == null || workYmd == null) {
            return null;
        }
        DailyScheduleVO sch = leaveDeductionMapper.selectDailySchedule(cmpnyCd, siteCd, userCd, workYmd);
        if (sch == null) {
            // 근무 계획/스케줄 없음 → 반차 경계 산출 불가(호출부가 ATTD_400_110 으로 거부).
            return null;
        }
        ScheduleWorkMinutesUtils.HalfDayBoundary boundary = ScheduleWorkMinutesUtils.halfDayBoundary(sch, part, waive);
        if (boundary == null) {
            log.warn("반차 경계 계산 실패(part={}, waive={}) - 스케줄 시각 비정상: cmpnyCd={}, siteCd={}, userCd={}, workYmd={}, schCd={}",
                    part, waive, cmpnyCd, siteCd, userCd, workYmd, sch.getSchCd());
        } else if (waive) {
            log.debug("[leave-deduct] 반차 경계(휴게 무시 체크): userCd={}, workYmd={}, part={}, boundary={}, recordOnly={}, brkTime={}",
                    userCd, workYmd, boundary.part(), boundary.boundaryMin(), boundary.recordOnly(),
                    boundary.breakTimeRegistered());
        }
        return boundary;
    }

    @Override
    public ScheduleWorkMinutesUtils.BreakMergeResult mergeAdjacentBreaks(String cmpnyCd, String siteCd,
                                                                        String userCd, String workYmd,
                                                                        int startMin, int endMin) {
        if (cmpnyCd == null || siteCd == null || userCd == null || workYmd == null || startMin >= endMin) {
            return null;
        }
        DailyScheduleVO sch = leaveDeductionMapper.selectDailySchedule(cmpnyCd, siteCd, userCd, workYmd);
        if (sch == null) {
            // 스케줄 없음 → 편입 산출 불가(호출부가 ATTD_400_110 으로 거부).
            return null;
        }
        ScheduleWorkMinutesUtils.BreakMergeResult r = ScheduleWorkMinutesUtils.mergeAdjacentBreaks(sch, startMin, endMin);
        if (r != null) {
            log.debug("[leave-deduct] 시간차 휴게 편입: userCd={}, workYmd={}, 신청={}~{}, 저장={}~{}, 차감={}분, 편입={}분, "
                            + "recordOnly={}, clamped={}",
                    userCd, workYmd, startMin, endMin, r.exemptStartMin(), r.exemptEndMin(),
                    r.chargeMinutes(), r.waivedBreakMinutes(), r.recordOnly(), r.clamped());
        }
        return r;
    }

    @Deprecated
    @Override
    public BigDecimal calcDeductionDays(int requestMinutes, int dailyStdWorkMinutes) {
        // LC-03 개편으로 calcHourlyCharge 가 대체(그날 소정근로 분모 → 환산시간 분모 + 하한/캡).
        //   호출처 0건 — 회귀 대비 한시 유지(인터페이스 @Deprecated 참조).
        if (requestMinutes <= 0 || dailyStdWorkMinutes <= 0) {
            return null;
        }
        return BigDecimal.valueOf(requestMinutes)
                .divide(BigDecimal.valueOf(dailyStdWorkMinutes), DEDUCTION_SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public HourlyChargeVO calcHourlyCharge(String cmpnyCd, String siteCd, String userCd, String workYmd,
                                           int requestMinutes) {
        if (cmpnyCd == null || siteCd == null || userCd == null || workYmd == null || requestMinutes <= 0) {
            return null;
        }

        // ⓐ 분모 = 당일 배정 스케줄 소정근로분(E1, 480 캡 E7 — 당일분모 전환 2026-08-03 확정).
        //    이미 조회하는 selectDailySchedule 결과를 D(마일스톤 하한 기준, raw)와 conv(=min(D,480))에
        //    함께 재사용한다(F-E — LeaveConversionPolicyService.resolveDailyConvMinutes 와 동일 산식:
        //    같은 쿼리 + ScheduleWorkMinutesUtils + 480 캡. 중복 쿼리 회피).
        //    산출 불가(근무계획 미배정/시각 비정상)면 시간차 사용 차단(E2 fail-closed, ATTD_400_194).
        //    E1 이후 이 분기는 선행 가드(ATTD_400_110, 스케줄 없는 날 시간차 거부)에 걸러져 실질 도달
        //    불가한 방어층이다. 본 진입부가 차단 판정의 단일 출처 — 웹/앱 submitLeave·preview 는 예외를
        //    그대로 전파한다.
        //    ★불변식(M4): E3 잠금(미결 시간차 신청 시점부터 그날 스케줄 변경 불가)으로 "시간차 존재 기간
        //    중 그날 스케줄 불변"이 구조 보장 → 취소·반려 재정산이 conv 를 재조회해도 원 차감 시점 분모와
        //    동일하다(레거시 차감분 예외는 LeaveHourlyResettleServiceImpl 참조 — E6 무보정 승계).
        DailyScheduleVO daySch = leaveDeductionMapper.selectDailySchedule(cmpnyCd, siteCd, userCd, workYmd);
        Integer daily = (daySch == null) ? null : ScheduleWorkMinutesUtils.dailyStdWorkMinutes(daySch);
        if (daily == null || daily <= 0) {
            log.info("[leave-deduct] 시간차 차단: 당일 분모 산출 불가(근무계획 미배정/시각 비정상). userCd={}, workYmd={}",
                    userCd, workYmd);
            throw new ApiException(AttdErrorCode.ATTD_400_194);
        }
        // conv = min(D, 480) — E7 캡은 분모(conv)에만 적용. 마일스톤 하한 기준 D 는 raw 유지(기존 의미 보존).
        int conv = Math.min(daily, LeaveConversionPolicyService.DEFAULT_CONV_MINUTES);

        // ⓑ 그날 기존 시간차(02/03/04) CONFIRMED 누적 — 전 연차타입 합산(F3 쪼개기 우회 차단).
        //    고정단위(종일/반차/반반차)는 누적 미포함(plan §8-⑤ — 1.0 점유 가드는 호출부 별도 층).
        HourlyLeaveAggVO agg = leaveDeductionMapper.selectHourlyLeaveAggOnDate(cmpnyCd, userCd, workYmd);
        int prevMinutes = (agg == null || agg.cumMinutes() == null) ? 0 : agg.cumMinutes();
        BigDecimal prevCharged = (agg == null || agg.cumChargedDays() == null)
                ? BigDecimal.ZERO : agg.cumChargedDays();

        // ⓒ 코어 산식(LC-05 재정산과 단일 출처 — HourlyLeaveChargeUtils)
        int cumAfter = prevMinutes + requestMinutes;
        BigDecimal raw = HourlyLeaveChargeUtils.rawDays(cumAfter, conv);
        BigDecimal floor = HourlyLeaveChargeUtils.milestoneFloorDays(cumAfter, daily);
        BigDecimal dayTotal = HourlyLeaveChargeUtils.dayTotalDays(cumAfter, conv, daily);
        BigDecimal charge = dayTotal.subtract(prevCharged);
        if (charge.signum() < 0) {
            // 과도기 방어: 보정(T4) 전 구식 차감(그날 소정근로 분모)이 남은 날은 기존 누적 차감 합이
            //   신식 dayTotal 을 넘을 수 있다 — 음수 부과(환급)는 만들지 않고 0 으로 절상.
            log.warn("[leave-deduct] 시간차 차액 음수 감지 — 0 절상(과도기 데이터). userCd={}, workYmd={}, "
                            + "dayTotal={}, prevCharged={}",
                    userCd, workYmd, dayTotal.toPlainString(), prevCharged.toPlainString());
            charge = BigDecimal.ZERO.setScale(DEDUCTION_SCALE);
        }

        boolean floorApplied = floor.compareTo(raw) > 0 && dayTotal.compareTo(floor) == 0;
        boolean capApplied = raw.max(floor).compareTo(BigDecimal.ONE) > 0;
        // 발동 마일스톤 요금(0.25/0.5/1.0) — FE 하한 안내 단위 분기용 부가정보(미발동이면 null).
        BigDecimal floorDays = floorApplied ? floor : null;

        log.debug("[leave-deduct] 시간차 차감 산출: userCd={}, workYmd={}, req={}분, 누적={}분, conv={}, D={}, "
                        + "raw={}, floor={}, dayTotal={}, charge={}",
                userCd, workYmd, requestMinutes, cumAfter, conv, daily,
                raw.toPlainString(), floor.toPlainString(), dayTotal.toPlainString(), charge.toPlainString());

        return new HourlyChargeVO(charge, dayTotal, conv, cumAfter, floorApplied, capApplied, floorDays);
    }

    @Override
    public boolean crossesBreak(String cmpnyCd, String siteCd, String userCd, String workYmd,
                                int startMin, int endMin) {
        if (startMin >= endMin) {
            return false;
        }
        DailyScheduleVO sch = leaveDeductionMapper.selectDailySchedule(cmpnyCd, siteCd, userCd, workYmd);
        if (sch == null) {
            // 스케줄/휴게시각 정보 없음 → 가로지름 검증 skip (보수 처리)
            return false;
        }
        return overlapsBreak(startMin, endMin, sch.getFstBrkStrTime(), sch.getFstBrkEndTime())
                || overlapsBreak(startMin, endMin, sch.getSecBrkStrTime(), sch.getSecBrkEndTime());
    }

    /**
     * 신청 구간 [startMin,endMin) 와 휴게 구간 [brkStr,brkEnd) 가 겹치면 true. 휴게 미설정/0폭이면 false.
     *
     * <p>G-1(2026-09-04): 휴게 종료는 {@link DateTimeUtils#brkEndToMinutes}("2400"=1440 인정)로 파싱하고,
     * 종료 &lt; 시작이면 자정 넘김 휴게(예 '2330'~'0030')로 보아 +1440 wrap 한다 — 야간 휴게가 가로지름 판정에서
     * 사라지던 결함 해소. 종료 == 시작('0000'~'0000')은 0폭 = 휴게 없음(종전과 동일).
     * 신청 분은 호출부 원시 프레임(0..1439)이므로 자정 이전 신청만 야간 휴게와 겹칠 수 있다(현행 범위와 동일).
     */
    private boolean overlapsBreak(int startMin, int endMin, String brkStr, String brkEnd) {
        Integer bs = DateTimeUtils.hhmmToMinutes(brkStr);
        Integer be = DateTimeUtils.brkEndToMinutes(brkEnd);
        if (bs == null || be == null) {
            return false;
        }
        int end = (be < bs) ? be + MINUTES_PER_DAY : be; // 자정 넘김 휴게 wrap
        if (end == bs) {
            return false;
        }
        return startMin < end && bs < endMin;
    }

    @Override
    public boolean withinScheduledWorkHours(String cmpnyCd, String siteCd, String userCd, String workYmd,
                                            int startMin, int endMin) {
        if (startMin >= endMin) {
            return false;
        }
        DailyScheduleVO sch = leaveDeductionMapper.selectDailySchedule(cmpnyCd, siteCd, userCd, workYmd);
        if (sch == null) {
            // 스케줄(근무시간) 자체가 없으면 시간차 신청 불가 → 포함 아님.
            return false;
        }
        return containedInWork(startMin, endMin, sch.getFstSchStrTime(), sch.getFstSchEndTime())
                || containedInWork(startMin, endMin, sch.getSecSchStrTime(), sch.getSecSchEndTime());
    }

    @Override
    public boolean overlapsTimeLeaveOnDate(String cmpnyCd, String siteCd, String userCd, String workYmd,
                                           String startHhmm, String endHhmm) {
        if (cmpnyCd == null || siteCd == null || userCd == null || workYmd == null
                || startHhmm == null || endHhmm == null) {
            return false;
        }

        // ① 그날 원 스케줄 = 연차 시각을 절대 시각으로 바꾸는 날짜 프레임(§15-2 규약).
        DailyScheduleVO sch = leaveDeductionMapper.selectDailySchedule(cmpnyCd, siteCd, userCd, workYmd);
        List<PartialLeaveWindowUtils.ScheduleSegment> frame = (sch == null)
                ? List.of()
                : PartialLeaveWindowUtils.scheduleSegments(
                        sch.getFstSchStrTime(), sch.getFstSchEndTime(),
                        sch.getSecSchStrTime(), sch.getSecSchEndTime());

        int[] target = PartialLeaveWindowUtils.exemptStampRange(startHhmm, endHhmm, frame);
        if (target == null) {
            // 프레임 부재/시각 비정상 → 판정 근거 없음. fail-closed(겹침으로 간주) + WARN.
            log.warn("[leave-deduct] 연차 시간대 겹침 판정 불가(보수 거부) - userCd={}, workYmd={}, {}~{}, schCd={}",
                    userCd, workYmd, startHhmm, endHhmm, (sch == null) ? null : sch.getSchCd());
            return true;
        }

        // ② 그날 확정 "시각 보유" 연차 행(통상 0~3건)을 같은 프레임으로 환산해 겹침 비교.
        List<LeaveTimeWindowVO> rows =
                leaveDeductionMapper.selectTimeLeaveWindowsOnDate(cmpnyCd, siteCd, userCd, workYmd);
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        for (LeaveTimeWindowVO row : rows) {
            if (row == null) {
                continue;
            }
            int[] exist = PartialLeaveWindowUtils.exemptStampRange(row.startTime(), row.endTime(), frame);
            if (exist == null) {
                log.warn("[leave-deduct] 기존 연차 시각 환산 불가(보수 거부) - userCd={}, workYmd={}, {}~{}",
                        userCd, workYmd, row.startTime(), row.endTime());
                return true;
            }
            if (PartialLeaveWindowUtils.stampOverlaps(target, exist)) {
                log.info("[leave-deduct] 연차 시간대 겹침 - userCd={}, workYmd={}, 신규={}~{}, 기존={}~{}",
                        userCd, workYmd, startHhmm, endHhmm, row.startTime(), row.endTime());
                return true;
            }
        }
        return false;
    }

    /** 신청 구간 [startMin,endMin] 이 근무구간 [schStr,schEnd] 에 완전히 포함되면 true. 구간 미설정이면 false. 야간(종료≤시작)은 +1440 보정. */
    private boolean containedInWork(int startMin, int endMin, String schStr, String schEnd) {
        Integer s = DateTimeUtils.hhmmToMinutes(schStr);
        // 종료는 자정 경계 근무(정책 attd/03 §3.3) "2400"=1440 을 인정하는 종료 전용 파서 사용.
        Integer e = DateTimeUtils.schEndToMinutes(schEnd);
        if (s == null || e == null) {
            return false;
        }
        int end = (e <= s) ? e + MINUTES_PER_DAY : e; // 야간 자정 넘김 보정
        return startMin >= s && endMin <= end;
    }
}
