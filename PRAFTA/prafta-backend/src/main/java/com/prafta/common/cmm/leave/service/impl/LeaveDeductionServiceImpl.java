package com.prafta.common.cmm.leave.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.leave.mapper.LeaveDeductionMapper;
import com.prafta.common.cmm.leave.service.LeaveDeductionService;
import com.prafta.common.cmm.leave.vo.DailyScheduleVO;
import com.prafta.common.util.DateTimeUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link LeaveDeductionService} 구현 (prafta-019-A §2.4).
 *
 * <p>정책서: {@code .claude/context/policies/attd/08-leave.md} §8.1.1, §8.5.9
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

        Integer fst = segmentWorkMinutes(sch.getFstSchStrTime(), sch.getFstSchEndTime(), sch.getFstSchBrkMin());
        if (fst == null) {
            // 1구간이 비정상이면 전체 산출 불가 (1구간은 필수)
            log.warn("1일 소정근로분 계산 실패 - 1구간 시각 비정상: cmpnyCd={}, siteCd={}, userCd={}, workYmd={}, schCd={}",
                    cmpnyCd, siteCd, userCd, workYmd, sch.getSchCd());
            return null;
        }

        int total = fst;

        // 2구간은 선택적 — 시작/종료가 모두 있을 때만 합산
        if (sch.getSecSchStrTime() != null && !sch.getSecSchStrTime().isBlank()
                && sch.getSecSchEndTime() != null && !sch.getSecSchEndTime().isBlank()) {
            Integer sec = segmentWorkMinutes(sch.getSecSchStrTime(), sch.getSecSchEndTime(), sch.getSecSchBrkMin());
            if (sec != null) {
                total += sec;
            }
        }

        return total > 0 ? Integer.valueOf(total) : null;
    }

    /**
     * 한 구간의 근로분 = {@code (종료 - 시작) - 휴게(분)}. 종료 ≤ 시작이면 야간 구간으로 보아 +1440 보정.
     * 시각 파싱 실패나 음수 결과 시 {@code null}.
     */
    private Integer segmentWorkMinutes(String strTime, String endTime, String brkMin) {
        Integer start = DateTimeUtils.hhmmToMinutes(strTime);
        Integer end = DateTimeUtils.hhmmToMinutes(endTime);
        if (start == null || end == null) {
            return null;
        }

        int span = end - start;
        if (span <= 0) {
            // 야간 구간 (예: 22:00 ~ 06:00)
            span += MINUTES_PER_DAY;
        }

        int brk = parseBrkMin(brkMin);
        int work = span - brk;
        return work > 0 ? Integer.valueOf(work) : null;
    }

    /** 휴게(분) 문자열 파싱. null/비정상/음수면 0. */
    private int parseBrkMin(String brkMin) {
        if (brkMin == null || brkMin.isBlank()) {
            return 0;
        }
        try {
            int v = Integer.parseInt(brkMin.trim());
            return v > 0 ? v : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public BigDecimal calcDeductionDays(int requestMinutes, int dailyStdWorkMinutes) {
        if (requestMinutes <= 0 || dailyStdWorkMinutes <= 0) {
            return null;
        }
        return BigDecimal.valueOf(requestMinutes)
                .divide(BigDecimal.valueOf(dailyStdWorkMinutes), DEDUCTION_SCALE, RoundingMode.HALF_UP);
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
        Integer e = DateTimeUtils.hhmmToMinutes(schEnd);
        if (s == null || e == null) {
            return false;
        }
        int end = (e <= s) ? e + MINUTES_PER_DAY : e; // 야간 자정 넘김 보정
        return startMin >= s && endMin <= end;
    }
}
