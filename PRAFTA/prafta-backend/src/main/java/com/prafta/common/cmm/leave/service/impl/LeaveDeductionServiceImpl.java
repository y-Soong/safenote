package com.prafta.common.cmm.leave.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.leave.mapper.LeaveDeductionMapper;
import com.prafta.common.cmm.leave.service.LeaveConversionPolicyService;
import com.prafta.common.cmm.leave.service.LeaveDeductionService;
import com.prafta.common.cmm.leave.util.HourlyLeaveChargeUtils;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils;
import com.prafta.common.cmm.leave.vo.DailyScheduleVO;
import com.prafta.common.cmm.leave.vo.HourlyChargeVO;
import com.prafta.common.cmm.leave.vo.HourlyLeaveAggVO;
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
    /** LC-03: 시간차 차감 분모(1일 환산시간, 신청 대상일 기준 F4) 단일 출처. */
    private final LeaveConversionPolicyService leaveConversionPolicyService;

    public LeaveDeductionServiceImpl(LeaveDeductionMapper leaveDeductionMapper,
                                     LeaveConversionPolicyService leaveConversionPolicyService) {
        this.leaveDeductionMapper = leaveDeductionMapper;
        this.leaveConversionPolicyService = leaveConversionPolicyService;
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

        // ⓐ 분모 = 개인 기본 근무타입 소정근로분(대상일 기준 유효 버전, 480 캡 — PC-03 D1).
        //    산출 불가(DEFAULT_SCH_CD 미지정(교대 등)/스케줄 이상)면 시간차 사용 차단(D2·N5 fail-closed).
        //    본 진입부가 차단 판정의 단일 출처 — 웹/앱 submitLeave·preview 는 예외를 그대로 전파한다.
        Integer convResolved = leaveConversionPolicyService.resolvePersonalConvMinutes(cmpnyCd, userCd, workYmd);
        if (convResolved == null) {
            log.info("[leave-deduct] 시간차 차단: 개인 분모 산출 불가(기본 근무타입 미지정 등). userCd={}, workYmd={}",
                    userCd, workYmd);
            throw new ApiException(AttdErrorCode.ATTD_400_193);
        }
        int conv = convResolved;

        // 마일스톤(하한) 기준 D = 그날 소정근로분. 스케줄 없는 날은 시간차 신청 자체가 호출부에서
        //   거부되지만(ATTD_400_110), 방어적으로 null 이면 하한 미적용(floor=0)으로 계산한다.
        Integer daily = getDailyStdWorkMinutes(cmpnyCd, siteCd, userCd, workYmd);

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

    /** 신청 구간 [startMin,endMin) 와 휴게 구간 [brkStr,brkEnd) 가 겹치면 true. 휴게 미설정/0폭이면 false. */
    private boolean overlapsBreak(int startMin, int endMin, String brkStr, String brkEnd) {
        Integer bs = DateTimeUtils.hhmmToMinutes(brkStr);
        Integer be = DateTimeUtils.hhmmToMinutes(brkEnd);
        if (bs == null || be == null || be <= bs) {
            return false;
        }
        return startMin < be && bs < endMin;
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
