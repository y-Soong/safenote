package com.prafta.app.home.home01.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prafta.app.home.home01.application.param.HomeSummaryParam;
import com.prafta.app.home.home01.application.query.HomeSummaryQuery;
import com.prafta.app.home.home01.dto.response.HomeSummaryResponse;
import com.prafta.app.home.home01.mapper.AppHome01Mapper;
import com.prafta.app.home.home01.result.AttdMgmtResult;
import com.prafta.app.home.home01.result.LeaveSummaryResult;
import com.prafta.app.home.home01.result.OvernightScheduleResult;
import com.prafta.app.home.home01.result.ScheduleResult;
import com.prafta.app.home.home01.result.TbmStatusResult;
import com.prafta.app.home.home01.service.AppHome01Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-001: 앱 메인화면 요약 서비스 구현.
 * <p>읽기 전용. 4개 영역을 개별 조회 후 계약서 §2 규칙대로 가공하여 단일 응답으로 묶는다.
 * 모든 식별값은 토큰 기반(param)이며 클라 입력을 사용하지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppHome01ServiceImpl implements AppHome01Service {

    // 근태 상태
    private static final String STATUS_BEFORE_WORK = "BEFORE_WORK";
    private static final String STATUS_WORKING = "WORKING";
    private static final String STATUS_OFF_WORK = "OFF_WORK";

    // TBM 세션 상태 / 참석 상태
    private static final String TBM_STATUS_NONE = "NONE";
    private static final String TBM_ATT_NOT_ENTERED = "NOT_ENTERED";
    private static final String TBM_ATT_ENTERED = "ENTERED";
    private static final String TBM_ATT_COMPLETED = "COMPLETED";
    private static final String TBM_COMPLETION_COMPLETED = "COMPLETED";

    private final AppHome01Mapper appHome01Mapper;

    @Override
    public HomeSummaryResponse selectHomeSummary(HomeSummaryParam param) {

        // leave/approval/tbm 영역이 동일 기준일을 공유하도록 DB 기준 오늘(YYYYMMDD)을 1회 조회
        String todayYmd = appHome01Mapper.selectTodayYmd();
        HomeSummaryQuery query = HomeSummaryQuery.from(param, todayYmd);

        // prafta-app-013-1: attendance 영역 전용 기준일(baseYmd).
        //   전일 야간(오버나이트) 근무가 자정을 넘겨 진행 중이면 전일 기준으로 본다.
        //   leave/approval/tbm 빌더는 회귀 방지를 위해 기존 todayYmd 그대로 사용한다.
        String baseYmd = resolveAttendanceBaseYmd(query, todayYmd);
        HomeSummaryQuery attdQuery = HomeSummaryQuery.from(param, baseYmd);

        log.info("[home01] 메인 요약 조회 시작 userCd={}, today={}, attdBase={}",
                param.userCd(), todayYmd, baseYmd);

        HomeSummaryResponse response = HomeSummaryResponse.builder()
                .attendance(buildAttendance(attdQuery))
                .leave(buildLeave(query))
                .approval(buildApproval(query))
                .tbm(buildTbm(query))
                .build();

        log.info("[home01] 메인 요약 조회 완료 userCd={}", param.userCd());

        return response;
    }

    /**
     * attendance 영역 산출.
     * <p>오늘 근태 레코드(구간별)와 스케줄로 status·표시시각·canCheckIn·canCheckOut 을 서버 산출한다.
     * <p>2구간 스케줄 대응(MainView 카드의 단일 구간 표시 한계 내):
     *   현재 진행/예정 중인 구간 하나를 골라 그 구간 기준으로 카드를 구성한다.
     *   특히 1구간 퇴근 완료 + 2구간 미출근 상태에서는 1구간의 출퇴근 기록을 보여주지 않고
     *   2구간 스케줄을 "예정"(BEFORE_WORK)으로 노출한다(attd01 의 구간 전환 규칙과 정합).
     * <p>isOffsite(prafta-app-003 B-1): 오늘 가장 최근 출근 레코드에 출근 GPS 행이 존재(=지오펜스 밖)하면 외근.
     */
    private HomeSummaryResponse.Attendance buildAttendance(HomeSummaryQuery query) {

        List<AttdMgmtResult> attds = appHome01Mapper.selectTodayAttdList(query);
        // prafta-app-013-1: sch 는 attendance 전용 기준일(baseYmd) 로 조회된다(query 가 attdQuery).
        ScheduleResult sch = appHome01Mapper.selectTodaySchedule(query);
        // 외근 여부: 오늘 최근 출근 레코드의 출근 GPS 행(01) 존재 여부(존재=외근). 오늘 출근 없으면 0.
        boolean isOffsite = appHome01Mapper.selectTodayCheckInOffsite(query) > 0;

        // prafta-app-013-2: 기준일 스케줄(work_plan→sch_mgmt) 존재 여부. 휴무/미배정/미존재면 false.
        boolean scheduleExists = (sch != null);
        // 2구간 스케줄 여부: SEC 시작시각이 채워져 있으면 2구간(attd01 isTwoSlot 판정과 동일).
        boolean isTwoSlot = sch != null && StringUtils.hasText(sch.secScheduleStart());

        // 구간별 근태 레코드 매핑(WORK_SEQ 1=1구간, 2=2구간). §5.5 Case C 는 1구간 없이 2구간만 존재 가능.
        AttdMgmtResult s1 = null;
        AttdMgmtResult s2 = null;
        if (attds != null) {
            for (AttdMgmtResult a : attds) {
                Integer seq = a.workSeq();
                if (seq != null && seq == 2) {
                    if (s2 == null) s2 = a;
                } else if (s1 == null) {
                    s1 = a; // WORK_SEQ=1 또는 null 은 1구간으로 간주.
                }
            }
        }

        boolean s1In = s1 != null;
        boolean s1Out = s1 != null && StringUtils.hasText(s1.checkOutTime());
        boolean s2In = s2 != null;
        boolean s2Out = s2 != null && StringUtils.hasText(s2.checkOutTime());

        // 진행 중(미퇴근) 레코드 존재 여부 → 퇴근 가능 판정 근거.
        boolean hasOpen = (s1In && !s1Out) || (s2In && !s2Out);
        // 그날 채울 수 있는 최대 구간 수(1구간=1, 2구간=2). 출근 가능 여부 산출에 사용.
        int maxSlots = isTwoSlot ? 2 : 1;
        int existing = (s1In ? 1 : 0) + (s2In ? 1 : 0);

        String status;
        String checkInTime = null;
        String checkOutTime = null;
        // 표시할 스케줄 시각(BEFORE_WORK 카드의 "예정 ~"). 기본은 1구간.
        String scheduleStart = (sch != null) ? sch.scheduleStart() : null;
        String scheduleEnd = (sch != null) ? sch.scheduleEnd() : null;

        if (isTwoSlot) {
            if (!s1In && !s2In) {
                // 출근 전 → 1구간 예정 표시.
                status = STATUS_BEFORE_WORK;
            } else if (s1In && !s1Out) {
                // 1구간 진행 중 → 근무중(1구간 출근시각).
                status = STATUS_WORKING;
                checkInTime = s1.checkInTime();
            } else if (s1Out && !s2In) {
                // 1구간 완료 + 2구간 미출근 → 2구간을 "예정"으로 노출(핵심 수정).
                status = STATUS_BEFORE_WORK;
                scheduleStart = sch.secScheduleStart();
                scheduleEnd = sch.secScheduleEnd();
            } else if (s2In && !s2Out) {
                // 2구간 진행 중 → 근무중(2구간 출근시각).
                status = STATUS_WORKING;
                checkInTime = s2.checkInTime();
            } else {
                // 전 구간 완료(2구간 퇴근까지) → 퇴근 완료. 하루 출근=첫 구간, 퇴근=마지막 구간으로 표시.
                status = STATUS_OFF_WORK;
                checkInTime = s1In ? s1.checkInTime() : s2.checkInTime();
                checkOutTime = s2.checkOutTime();
            }
        } else {
            // 1구간 스케줄(기존 동작 유지) — 다만 canCheckIn 은 아래 공통 규칙으로 산출.
            if (!s1In) {
                status = STATUS_BEFORE_WORK;
            } else {
                checkInTime = s1.checkInTime();
                checkOutTime = s1.checkOutTime();
                status = s1Out ? STATUS_OFF_WORK : STATUS_WORKING;
            }
        }

        // 공통 액션 규칙(attd01 정합): 진행중 레코드 있으면 퇴근만, 없고 잔여 구간 있으면 출근만.
        boolean canCheckOut = hasOpen;
        boolean canCheckIn = !hasOpen && existing < maxSlots;

        return HomeSummaryResponse.Attendance.builder()
                .status(status)
                .scheduleExists(scheduleExists) // prafta-app-013-2: 기준일 스케줄 존재 여부.
                .scheduleStart(scheduleStart)
                .scheduleEnd(scheduleEnd)
                .checkInTime(checkInTime)
                .checkOutTime(checkOutTime)
                .isOffsite(isOffsite) // 오늘 최근 출근 GPS 행(01) 존재=외근 (prafta-app-003 B-1).
                .canCheckIn(canCheckIn)
                .canCheckOut(canCheckOut)
                .build();
    }

    /**
     * leave 영역 산출(법정+약정 합산). SUM 이 null 이면 0.0 폴백, 소수 1자리 반올림.
     */
    private HomeSummaryResponse.Leave buildLeave(HomeSummaryQuery query) {

        LeaveSummaryResult leave = appHome01Mapper.selectLeaveSummary(query);

        double granted = toScaledDouble(leave != null ? leave.grantedDays() : null);
        double remaining = toScaledDouble(leave != null ? leave.remainingDays() : null);

        return HomeSummaryResponse.Leave.builder()
                .grantedDays(granted)
                .remainingDays(remaining)
                .build();
    }

    /**
     * approval 영역 산출(REQ_STATUS='01' 본인 건수).
     */
    private HomeSummaryResponse.Approval buildApproval(HomeSummaryQuery query) {

        int pending = appHome01Mapper.selectPendingApprovalCount(query);

        return HomeSummaryResponse.Approval.builder()
                .pendingCount(pending)
                .build();
    }

    /**
     * tbm 영역 산출. 오늘 세션 없으면 hasToday=false / NONE.
     */
    private HomeSummaryResponse.Tbm buildTbm(HomeSummaryQuery query) {

        TbmStatusResult tbm = appHome01Mapper.selectTodayTbm(query);

        if (tbm == null) {
            return HomeSummaryResponse.Tbm.builder()
                    .hasToday(false)
                    .sessionCd(null)
                    .sessionStatus(TBM_STATUS_NONE)
                    .openedTime(null)
                    .title(null)
                    .presenterName(null)
                    .myAttendanceStatus(TBM_ATT_NOT_ENTERED)
                    .myEntryTime(null)
                    .build();
        }

        String myAttendanceStatus = resolveMyAttendanceStatus(tbm);
        String myEntryTime = StringUtils.hasText(tbm.attendanceCd()) ? tbm.myEntryTime() : null;

        return HomeSummaryResponse.Tbm.builder()
                .hasToday(true)
                .sessionCd(tbm.sessionCd())
                .sessionStatus(tbm.sessionStatus())
                .openedTime(tbm.openedTime())
                .title(tbm.title())
                .presenterName(tbm.presenterName())
                .myAttendanceStatus(myAttendanceStatus)
                .myEntryTime(myEntryTime)
                .build();
    }

    /**
     * 본인 참석 상태 산출.
     * <ul>
     *   <li>참석 레코드 없음 → NOT_ENTERED</li>
     *   <li>COMPLETION_STATUS_CD='COMPLETED' 또는 EXIT_AT 존재 → COMPLETED</li>
     *   <li>그 외(입실만) → ENTERED</li>
     * </ul>
     */
    private String resolveMyAttendanceStatus(TbmStatusResult tbm) {

        if (!StringUtils.hasText(tbm.attendanceCd())) {
            return TBM_ATT_NOT_ENTERED;
        }

        boolean exitExists = "Y".equals(tbm.exitExists());
        boolean completed = TBM_COMPLETION_COMPLETED.equals(tbm.completionStatusCd());

        if (exitExists || completed) {
            return TBM_ATT_COMPLETED;
        }
        return TBM_ATT_ENTERED;
    }

    /**
     * prafta-app-013-1: attendance 영역 전용 기준일(baseYmd) 산출.
     * <p>전일 스케줄의 마지막 구간이 자정을 넘기는(오버나이트) 근무이고, 현재 시각이
     * 아직 그 종료시각 이전(새벽)이면 전일을 기준으로 본다. 그 외에는 당일.
     * <p>경계는 strict(nowMin &lt; endMin): 종료시각 정각이면 당일로 전환.
     * attd01 의 toMinutes/nowMinutes 시각 해석과 동일하게 구현한다(자정 넘김 = 종료&lt;시작).
     *
     * @param query    본인 스코프(cmpny/site/user)
     * @param todayYmd DB 기준 오늘(YYYYMMDD)
     * @return baseYmd (당일 또는 전일, YYYYMMDD)
     */
    private String resolveAttendanceBaseYmd(HomeSummaryQuery query, String todayYmd) {

        // 전일 일자(YYYYMMDD) — BASIC_ISO_DATE 로 파싱 후 minusDays(1).
        String yesterdayYmd;
        try {
            LocalDate today = LocalDate.parse(todayYmd, DateTimeFormatter.BASIC_ISO_DATE);
            yesterdayYmd = today.minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);
        } catch (Exception e) {
            // 일자 파싱 실패는 비정상 — 안전하게 당일 기준 유지.
            log.warn("[home01] baseYmd 산출 중 today 파싱 실패 today={} → 당일 기준 유지", todayYmd);
            return todayYmd;
        }

        OvernightScheduleResult prevSch =
                appHome01Mapper.selectScheduleEndForOvernight(query, yesterdayYmd);
        if (prevSch == null) {
            return todayYmd; // 전일 스케줄(휴무/미배정 포함) 없음 → 당일.
        }

        // 마지막 구간: 2구간 스케줄이면 SEC, 아니면 FST.
        boolean twoSlot = StringUtils.hasText(prevSch.secSchStrTime());
        String lastStart = twoSlot ? prevSch.secSchStrTime() : prevSch.fstSchStrTime();
        String lastEnd = twoSlot ? prevSch.secSchEndTime() : prevSch.fstSchEndTime();

        Integer startMin = toMinutes(lastStart);
        Integer endMin = toMinutes(lastEnd);
        if (startMin == null || endMin == null) {
            return todayYmd; // 시각 해석 불가 → 당일.
        }

        boolean overnight = endMin < startMin; // 종료<시작 = 자정 넘김.
        if (overnight && nowMinutes() < endMin) {
            return yesterdayYmd; // 새벽, 아직 전일 근무 진행 중 → 전일 기준.
        }
        return todayYmd;
    }

    /** HHMM -> 자정 기준 분(HH*60+MM). 잘못된 값이면 null. (attd01 toMinutes 와 동일 해석) */
    private Integer toMinutes(String hhmm) {
        if (hhmm == null || !hhmm.matches("\\d{4}")) {
            return null;
        }
        int h = Integer.parseInt(hhmm.substring(0, 2));
        int m = Integer.parseInt(hhmm.substring(2, 4));
        return h * 60 + m;
    }

    /** 현재 시각(분, 0~1439). 서버 LocalTime.now() 기준. (attd01 nowMinutes 와 동일) */
    private int nowMinutes() {
        LocalTime t = LocalTime.now();
        return t.getHour() * 60 + t.getMinute();
    }

    /**
     * BigDecimal → double (null=0.0, 소수 1자리 반올림).
     */
    private double toScaledDouble(BigDecimal value) {
        if (value == null) {
            return 0.0;
        }
        return value.setScale(1, java.math.RoundingMode.HALF_UP).doubleValue();
    }
}
