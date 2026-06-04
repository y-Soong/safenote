package com.prafta.app.attd.attd01.service.impl;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.app.attd.attd01.application.command.CheckInCommand;
import com.prafta.app.attd.attd01.application.command.CheckInGpsCommand;
import com.prafta.app.attd.attd01.application.command.CheckOutCommand;
import com.prafta.app.attd.attd01.application.command.CheckOutGpsCommand;
import com.prafta.app.attd.attd01.application.param.CheckInParam;
import com.prafta.app.attd.attd01.application.param.CheckOutParam;
import com.prafta.app.attd.attd01.application.param.DayDetailParam;
import com.prafta.app.attd.attd01.application.param.MonthParam;
import com.prafta.app.attd.attd01.application.param.TodayParam;
import com.prafta.app.attd.attd01.application.param.WeekParam;
import com.prafta.app.attd.attd01.application.query.AttdRangeQuery;
import com.prafta.app.attd.attd01.dto.response.AttendanceResponse;
import com.prafta.app.attd.attd01.dto.response.DayActionsResponse;
import com.prafta.app.attd.attd01.dto.response.MonthDayResponse;
import com.prafta.app.attd.attd01.dto.response.MonthSummaryResponse;
import com.prafta.app.attd.attd01.dto.response.MyAttendanceDayResponse;
import com.prafta.app.attd.attd01.dto.response.MyMonthResponse;
import com.prafta.app.attd.attd01.dto.response.MyWeekResponse;
import com.prafta.app.attd.attd01.dto.response.ScheduleResponse;
import com.prafta.app.attd.attd01.dto.response.SlotResponse;
import com.prafta.app.attd.attd01.dto.response.StandardizedResponse;
import com.prafta.app.attd.attd01.dto.response.WeekDayActionsResponse;
import com.prafta.app.attd.attd01.dto.response.WeekDayResponse;
import com.prafta.app.attd.attd01.dto.response.WeekSummaryResponse;
import com.prafta.app.attd.attd01.mapper.AppAttd01Mapper;
import com.prafta.app.attd.attd01.result.AttdRecordResult;
import com.prafta.app.attd.attd01.result.GpsResult;
import com.prafta.app.attd.attd01.result.HolidayResult;
import com.prafta.app.attd.attd01.result.LeaveUseResult;
import com.prafta.app.attd.attd01.result.OpenAttdResult;
import com.prafta.app.attd.attd01.result.ScheduleResult;
import com.prafta.app.attd.attd01.result.SiteGeofenceResult;
import com.prafta.app.attd.attd01.result.StdTimeRuleResult;
import com.prafta.app.attd.attd01.service.AppAttd01Service;
import com.prafta.common.cmm.leave.service.LeaveRefusalDetectService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-002: 앱 본인 근태조회(attd01) 서비스 구현.
 *
 * <p>설계 원칙:
 *   <ul>
 *     <li>Mapper 는 원천 데이터(스케줄/근태/GPS/표준화룰/휴일/연차/마감)만 로딩한다.</li>
 *     <li>workStatus / 표준화 / 액션 활성도 / dayType / 합계 산출은 전부 본 서비스에서 수행한다.</li>
 *     <li>식별자(cmpnyCd/siteCd/userCd)는 Param 에서 JWT 출처로 캐노니컬라이즈된 값만 사용(IDOR 가드).</li>
 *   </ul>
 *
 * <p>표준화 방향(회사 유리 — 사용자 확정): 출근은 올림(ceil, 더 늦은 경계), 퇴근은 내림(floor,
 *   더 이른 경계)하여 근로분을 회사에 유리하게 산정한다. 올림/내림 적용 후 표준 출근/퇴근의 시간
 *   순서가 원본 일자경계 방향과 모순되면(예: 출퇴근 시각 동일·근접 → 표준 출근&ge;표준 퇴근) 표준화를
 *   적용하지 않고(applied=false) 원본 근태값을 그대로 노출한다(산출은 {@link #buildStandardized}).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppAttd01ServiceImpl implements AppAttd01Service {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HHmm");
    private static final DateTimeFormatter HHMMSS = DateTimeFormatter.ofPattern("HHmmss");
    private static final String NOTE_COMPLETED_ONLY = "완료된 근무만 합산";

    // SYS031 출퇴근 방법: 01=사용자등록
    private static final String CHECK_OUT_METHOD_USER = "01";
    private static final String CHECK_IN_METHOD_USER = "01";
    // SYS028 GPS 정보타입: 01=출근, 02=퇴근
    private static final String GPS_TYPE_CHECK_IN = "01";
    private static final String GPS_TYPE_CHECK_OUT = "02";

    // SYS028 표준화 룰 타입: 01=출근, 02=퇴근
    private static final String STD_RULE_CHECK_IN = "01";
    private static final String STD_RULE_CHECK_OUT = "02";

    // workStatus 값
    private static final String STATUS_BEFORE_WORK = "BEFORE_WORK";
    private static final String STATUS_WORKING = "WORKING";
    private static final String STATUS_TWO_SLOT_WORKING = "TWO_SLOT_WORKING";
    private static final String STATUS_CHECKED_OUT = "CHECKED_OUT";
    private static final String STATUS_CHECK_OUT_MISSING = "CHECK_OUT_MISSING";

    // attendanceStatus 값 (week)
    private static final String ATTD_NORMAL = "NORMAL";
    private static final String ATTD_LATE = "LATE";
    private static final String ATTD_EARLY_LEAVE = "EARLY_LEAVE";
    private static final String ATTD_MISSING = "MISSING";
    private static final String ATTD_WORKING = "WORKING";
    private static final String ATTD_NOT_STARTED = "NOT_STARTED";

    // dayType 값 (month)
    private static final String DAY_WORK = "WORK";
    private static final String DAY_LEAVE = "LEAVE";
    private static final String DAY_OFF = "OFF";
    private static final String DAY_ACTION_REQUIRED = "ACTION_REQUIRED";

    private final AppAttd01Mapper appAttd01Mapper;

    /** PRAFTA-COM-001: 노무수령거부 대상일 출근 감지 + 관리자 PUSH(체크인 hook 전용, 예외 격리). */
    private final LeaveRefusalDetectService leaveRefusalDetectService;

    // ====================================================================
    // 1) 오늘 / 일자 상세 (동일 응답 구조)
    // ====================================================================

    @Override
    public MyAttendanceDayResponse selectToday(TodayParam param) {
        String today = param.workYmd();
        return buildDayResponse(
                param.cmpnyCd(), param.siteCd(), param.userCd(), resolveSiteName(param.cmpnyCd(), param.siteCd(), param.siteNm()),
                param.workYmd(), today
        );
    }

    @Override
    public MyAttendanceDayResponse selectDayDetail(DayDetailParam param) {
        String today = LocalDate.now().format(YMD);
        return buildDayResponse(
                param.cmpnyCd(), param.siteCd(), param.userCd(), resolveSiteName(param.cmpnyCd(), param.siteCd(), param.siteNm()),
                param.workYmd(), today
        );
    }

    /**
     * 단일 일자 카드 응답 빌드(오늘/일상세 공용).
     * @param targetYmd 조회 대상 일자, @param todayYmd 서버 today(상태/액션 판정 기준)
     */
    private MyAttendanceDayResponse buildDayResponse(
            String cmpnyCd, String siteCd, String userCd, String siteName,
            String targetYmd, String todayYmd) {

        AttdRangeQuery q = new AttdRangeQuery(cmpnyCd, siteCd, userCd, targetYmd, targetYmd);

        List<ScheduleResult> schedules = nullSafe(appAttd01Mapper.selectScheduleByRange(q));
        List<AttdRecordResult> attds = nullSafe(appAttd01Mapper.selectAttdByRange(q));
        List<StdTimeRuleResult> stdRules = nullSafe(appAttd01Mapper.selectStdTimeRules(cmpnyCd));

        // prafta-app-018-E: 그날 연차 사용내역(CONFIRMED) 단일일 조회 — 부분연차 상세 표시 전용.
        //   ⚠️ 표시만 한다. isLeaveDay/slotCount/slots/workStatus/dayType/합계 등 근무일 산출 로직은
        //   일절 건드리지 않는다(근무일 유지). 다건이면 첫 1건만 안전 채택(주간 putIfAbsent 와 일관).
        List<LeaveUseResult> dayLeaves = nullSafe(appAttd01Mapper.selectLeaveUseByRange(q));
        LeaveUseResult dayLeave = dayLeaves.isEmpty() ? null : dayLeaves.get(0);

        // GPS 는 해당 일자 근태레코드(ATTD_ID) 기준으로만 로딩.
        Map<String, GpsResult> gpsByCheckIn = new HashMap<>();
        Map<String, GpsResult> gpsByCheckOut = new HashMap<>();
        loadGps(cmpnyCd, attds, gpsByCheckIn, gpsByCheckOut);

        // 마감 여부(해당 월) — 액션 활성도 산출에 사용.
        boolean closed = isMonthClosed(cmpnyCd, siteCd, userCd, targetYmd.substring(0, 6));

        ScheduleResult sched = schedules.isEmpty() ? null : schedules.get(0);
        boolean isLeaveDay = sched != null && sched.leaveCd() != null && sched.leaveNm() != null;
        boolean isTwoSlot = sched != null && StringUtils.hasText(sched.secSchStrTime());
        boolean hasScheduleDay = sched != null && !isLeaveDay;

        // workSeq -> 근태레코드 매핑.
        Map<Integer, AttdRecordResult> attdBySeq = new HashMap<>();
        for (AttdRecordResult a : attds) {
            attdBySeq.putIfAbsent(a.workSeq(), a);
        }

        // prafta-app-014: 화면/상태/액션의 단일 기준 = effectiveSlotCount(1~2).
        //   연차일은 슬롯 비대상(0). 스케줄도 출근기록도 없는 순수 휴무일도 0(종전과 동일하게 슬롯 미생성).
        //   스케줄 없는 날도 실제 출근기록이 있으면 슬롯을 만든다(추가근무 슬롯, D2).
        boolean hasAnyAttd = attdBySeq.get(1) != null || attdBySeq.get(2) != null;
        int slotCount = (isLeaveDay || (!hasScheduleDay && !hasAnyAttd))
                ? 0
                : effectiveSlotCount(isTwoSlot, hasScheduleDay, attdBySeq);

        int unitIn = unitMinutesOf(stdRules, STD_RULE_CHECK_IN);
        int unitOut = unitMinutesOf(stdRules, STD_RULE_CHECK_OUT);

        boolean isPast = targetYmd.compareTo(todayYmd) < 0;
        boolean isToday = targetYmd.equals(todayYmd);
        int nowMinIfToday = isToday ? nowMinutes() : -1;

        // prafta-app-015: 2구간 스케줄 구간 선택 버튼 게이팅(서버 산출, 프론트 표시 전용).
        //   퇴근 가능 윈도우 = 근무일 당일(D) ~ 다음날(D+1). 같은 구간 진행중(미퇴근) 여부도 반영한다.
        String yesterdayYmdForSlot = LocalDate.parse(todayYmd, YMD).minusDays(1).format(YMD);
        boolean withinCheckoutWindowForSlot = isToday || targetYmd.equals(yesterdayYmdForSlot);
        int attdCountForSlot = (attdBySeq.get(1) != null ? 1 : 0) + (attdBySeq.get(2) != null ? 1 : 0);

        List<SlotResponse> slots = new ArrayList<>();
        if (!isLeaveDay) {
            // prafta-app-014: effectiveSlotCount 만큼 슬롯 생성. 스케줄 미대응 슬롯은 schedule=null(D2).
            //   슬롯1 스케줄 대응 = 스케줄 있는 날. 슬롯2 스케줄 대응 = 스케줄 2구간일 때만.
            for (int seq = 1; seq <= slotCount; seq++) {
                boolean slotHasSchedule = (seq == 1) ? hasScheduleDay : isTwoSlot;
                AttdRecordResult slotAttd = attdBySeq.get(seq);

                // prafta-app-015: 구간 플래그(2구간 스케줄 한정). 1구간/스케줄없음은 단일 actions.canCheckIn 유지(false).
                boolean alreadyCheckedIn = slotAttd != null;
                boolean canCheckInThisSlot = isTwoSlot
                        && !alreadyCheckedIn
                        && !closed
                        && withinCheckoutWindowForSlot
                        && attdCountForSlot < 2;

                slots.add(buildSlot(sched, seq, slotHasSchedule, slotAttd,
                        gpsByCheckIn, gpsByCheckOut, unitIn, unitOut, isToday, isPast, nowMinIfToday,
                        canCheckInThisSlot, alreadyCheckedIn));
            }
        }

        String workStatus = computeWorkStatus(sched, isLeaveDay, slotCount, attdBySeq, isToday, isPast, nowMinIfToday);

        DayActionsResponse actions = computeDayActions(
                sched, isLeaveDay, attdBySeq, isToday, isPast, closed, targetYmd, todayYmd);

        // prafta-app-013: 오늘/일상세 바텀시트 4액션(+leaveFullDayOnly) — 이번주와 동일 규칙(결정 §2).
        // prafta-app-014: completed/합계/요약 게이트를 isTwoSlot → effectiveSlotCount 로 이관.
        boolean hasSchedule = hasScheduleDay;
        boolean completedForSheet = allSlotsCompleted(slotCount, attdBySeq);
        boolean hasAttendanceForSheet = attdBySeq.get(1) != null || attdBySeq.get(2) != null;
        WeekDayActionsResponse sheetActions = computeActionFlags(
                hasSchedule, isPast, isToday, hasAttendanceForSheet, completedForSheet, closed);

        String workPlanName = resolveWorkPlanName(sched, isLeaveDay);

        // prafta-app-013: 시트 메타 1줄(이번주와 동일) — 근무 스케줄 있을 때만 산출.
        String scheduleSummary = hasSchedule ? scheduleSummary(sched, isTwoSlot) : null;
        String attendanceSummary = hasSchedule ? attendanceSummary(attdBySeq, slotCount) : null;

        // dayType/hasIssue 산출 — selectMonth 와 동일 규칙(처리 필요 빠른 액션 노출 근거, 시안 §4.4.3).
        boolean hasSchCd = sched != null && !isLeaveDay;
        boolean actionRequired = isPast && hasSchCd
                && hasActionRequired(sched, slotCount, attdBySeq, attds, gpsByCheckIn, gpsByCheckOut, closed, targetYmd);
        String dayType;
        if (actionRequired) {
            dayType = DAY_ACTION_REQUIRED;
        } else if (isLeaveDay) {
            dayType = DAY_LEAVE;
        } else if (hasSchCd) {
            dayType = DAY_WORK;
        } else {
            dayType = DAY_OFF;
        }

        return MyAttendanceDayResponse.builder()
                .workDate(targetYmd)
                .siteName(siteName)
                .workPlanCode(sched == null ? null : sched.workPlanCd())
                .workPlanName(workPlanName)
                .scheduleSummary(scheduleSummary)
                .attendanceSummary(attendanceSummary)
                .workStatus(workStatus)
                .isTwoSlot(isTwoSlot)
                .slotCount(slotCount)
                .slots(slots)
                .actions(actions)
                .sheetActions(sheetActions)
                .dayType(dayType)
                .hasIssue(DAY_ACTION_REQUIRED.equals(dayType))
                // prafta-app-018-E: 부분연차 상세필드(표시 전용·근무일 유지). 사용내역 없으면 전부 null/false.
                .isLeaveUsed(dayLeave != null)
                .leaveTypeName(dayLeave == null ? null : dayLeave.leaveNm())
                .leaveUnitType(dayLeave == null ? null : dayLeave.useUnitType())
                .leaveTimeRange(dayLeave == null ? null : leaveTimeRange(dayLeave.startTime(), dayLeave.endTime()))
                .leaveDays(dayLeave == null ? null : dayLeave.leaveDays())
                .build();
    }

    /**
     * 구간(슬롯) 응답 빌드.
     *
     * <p>prafta-app-014: {@code hasScheduleForSlot} 가 false 면 "스케줄 미대응 슬롯"(스케줄 없는 날의
     *   슬롯, 1구간 스케줄의 2번째 출근 등)으로 보고 {@code schedule=null} 로 내리며 표준화/지각·조퇴를
     *   적용하지 않는다(원본 출퇴근 시각만). 미등록 판정도 스케줄 종료가 없으므로 적용하지 않는다(D2).
     */
    private SlotResponse buildSlot(
            ScheduleResult sched, int seq, boolean hasScheduleForSlot, AttdRecordResult attd,
            Map<String, GpsResult> gpsByCheckIn, Map<String, GpsResult> gpsByCheckOut,
            int unitIn, int unitOut, boolean isToday, boolean isPast, int nowMinIfToday,
            boolean canCheckInThisSlot, boolean alreadyCheckedIn) {

        // 스케줄 미대응 슬롯: 스케줄 메타/표준화/미등록 판정 비대상.
        String schStr = null;
        String schEnd = null;
        Integer brkMin = 0;
        ScheduleResponse scheduleRes = null;
        if (hasScheduleForSlot && sched != null) {
            schStr = seq == 1 ? sched.fstSchStrTime() : sched.secSchStrTime();
            schEnd = seq == 1 ? sched.fstSchEndTime() : sched.secSchEndTime();
            String schBrk = seq == 1 ? sched.fstSchBrkMin() : sched.secSchBrkMin();
            brkMin = parseIntOrZero(schBrk);
            Integer schWorkMin = computeWorkMinutes(schStr, schEnd, brkMin);
            scheduleRes = ScheduleResponse.builder()
                    .startTime(schStr)
                    .endTime(schEnd)
                    .breakMinutes(brkMin)
                    .workMinutes(schWorkMin)
                    .build();
        }

        AttendanceResponse attendanceRes = null;
        StandardizedResponse standardizedRes = null;

        if (attd != null) {
            boolean hasCheckOut = StringUtils.hasText(attd.checkOutTime());

            // 퇴근 미등록 판정: 출근有 && 퇴근NULL && 스케줄 종료 경과(당일) 또는 과거일.
            //   스케줄 미대응 슬롯은 종료 기준이 없으므로 미등록 표시 비대상(과거일만 보정 필요로 봄은 hasActionRequired 가 별도 처리).
            boolean missingCheckOut = hasScheduleForSlot
                    && !hasCheckOut && isCheckOutMissing(schEnd, isToday, isPast, nowMinIfToday);

            // 외근 여부(prafta-app-003 B-2): GPS 행 존재 = 지오펜스 밖(외근). 출근=01행, 퇴근=02행.
            //   GPS 행은 외근일 때만 저장되므로(A0-2 확정 모델) 존재여부만 본다(Mock/유효 판정 제거).
            boolean checkInOffsite = gpsByCheckIn.get(attd.attdId()) != null;
            boolean checkOutOffsite = hasCheckOut && gpsByCheckOut.get(attd.attdId()) != null;

            attendanceRes = AttendanceResponse.builder()
                    .checkInDate(attd.checkInDate())
                    .checkInTime(attd.checkInTime())
                    .checkInSiteName(attd.siteNm())
                    // 스키마 한계(plan §0-2): 퇴근지 분리 컬럼 없음 → 레코드 SITE_NM 동일.
                    .checkOutSiteName(hasCheckOut ? attd.siteNm() : null)
                    .checkInOffsite(checkInOffsite)
                    .checkOutOffsite(checkOutOffsite)
                    .checkOutDate(attd.checkOutDate())
                    .checkOutTime(attd.checkOutTime())
                    .isMissingCheckOut(missingCheckOut)
                    // 스키마 한계: 출근지=퇴근지(동일 SITE_CD) → 항상 false.
                    .isDifferentSite(false)
                    .build();

            // 표준화: 출퇴근 모두 등록 + 스케줄 대응 슬롯일 때만 산출(attd §10.2, prafta-app-014 D2).
            //   방향(회사 유리): 출근 올림 / 퇴근 내림. 규칙 위반 시 applied=false(원본 표시).
            //   스케줄 미대응 슬롯(추가 출근)은 표준화 비대상 → standardized=null(프론트 표준화 행 숨김).
            if (hasScheduleForSlot && hasCheckOut) {
                standardizedRes = buildStandardized(
                        attd.checkInTime(), attd.checkOutTime(), brkMin, unitIn, unitOut);
            }
        }

        return SlotResponse.builder()
                .workSeq(seq)
                .schedule(scheduleRes)
                .attendance(attendanceRes)
                .standardized(standardizedRes)
                .canCheckInThisSlot(canCheckInThisSlot)   // prafta-app-015: 2구간 구간 선택 게이팅.
                .alreadyCheckedIn(alreadyCheckedIn)
                .build();
    }

    /**
     * workStatus 산출 (시안 §2.2).
     *
     * <p>prafta-app-014: 판정 기준을 isTwoSlot → effectiveSlotCount 로 이관.
     *   slotCount==2(스케줄 2구간 또는 1구간/스케줄없음의 2번째 출근)면 2슬롯 진행/완료 로직,
     *   slotCount==1 이면 1슬롯 로직을 적용한다. 단 "2번째 출근 가능 여부"는 상태와 분리하여
     *   {@link #computeDayActions} 에서 effective 상한으로 산출한다(1구간 1회 퇴근 후 slotCount=1
     *   이어도 canCheckIn 이 뜰 수 있음).
     */
    private String computeWorkStatus(
            ScheduleResult sched, boolean isLeaveDay, int slotCount,
            Map<Integer, AttdRecordResult> attdBySeq, boolean isToday, boolean isPast, int nowMinIfToday) {

        if (sched == null || isLeaveDay) {
            return null; // 스케줄 없음(스케줄 없는 날의 카드 상태는 별도 표시 안함)/연차일은 카드 상태 비대상.
        }

        AttdRecordResult s1 = attdBySeq.get(1);
        AttdRecordResult s2 = attdBySeq.get(2);
        boolean s1In = s1 != null;
        boolean s1Out = s1 != null && StringUtils.hasText(s1.checkOutTime());
        boolean s2In = s2 != null;
        boolean s2Out = s2 != null && StringUtils.hasText(s2.checkOutTime());

        if (slotCount >= 2) {
            if (s1Out && s2Out) return STATUS_CHECKED_OUT;
            // prafta-app-015: 2구간 단독 진행(WORK_SEQ=2 먼저 출근, s1In=false·s2In=true)도 정식 도달 가능.
            //   1구간만/2구간만/양 구간 진행 중 어느 쪽이든 근무 중으로 통일(시안 §2.2).
            //   home01 buildAttendance 의 STATUS_WORKING(2구간 진행 중) 산출과 의미 정합.
            if (s1In || s2In) return STATUS_TWO_SLOT_WORKING;
            return STATUS_BEFORE_WORK;
        }

        // 1슬롯
        if (!s1In) {
            return STATUS_BEFORE_WORK;
        }
        if (s1Out) {
            return STATUS_CHECKED_OUT;
        }
        // 출근有 / 퇴근NULL
        if (isCheckOutMissing(sched.fstSchEndTime(), isToday, isPast, nowMinIfToday)) {
            return STATUS_CHECK_OUT_MISSING;
        }
        return STATUS_WORKING;
    }

    /**
     * 오늘/일상세 카드 액션 활성도 산출 (계약 §3.1).
     *
     * <p>퇴근 가능 기한(정책 확정): "익일(D+1)까지". 오버나이트 근무를 위해 근무일 당일 퇴근을
     *   못 찍어도 다음 날까지 퇴근 버튼을 열어둔다(예: 09~18 근무자가 18시 퇴근 미체크 → 다음 날까지 퇴근 가능).
     *   다음 날 출근은 전날 퇴근을 찍은 뒤에만 가능(이 게이팅은 출퇴근 등록 플로우에서 강제 — 본 화면은 표시만).
     *   활성도 매트릭스(시안 §3.1):
     *   <ul>
     *     <li>canCheckOut: (targetYmd==today || targetYmd==today-1) && 출근有 && 퇴근NULL (진행 중 구간).</li>
     *     <li>canCheckIn: 2구간 && 1구간 퇴근 완료 && 2구간 미출근 (attd §5.1/§5.2 재출근).</li>
     *     <li>canRequestModify: 출퇴근 완료(전 구간) && 해당월 미마감 (attd §11.1 확정 후 보정).</li>
     *   </ul>
     */
    private DayActionsResponse computeDayActions(
            ScheduleResult sched, boolean isLeaveDay,
            Map<Integer, AttdRecordResult> attdBySeq, boolean isToday, boolean isPast,
            boolean closed, String targetYmd, String todayYmd) {

        boolean canCheckOut = false;
        boolean canCheckIn = false;
        boolean canRequestModify = false;

        // 퇴근 가능 윈도우 = 근무일 당일(D) ~ 그 다음 날(D+1). 오버나이트 대응.
        String yesterdayYmd = LocalDate.parse(todayYmd, YMD).minusDays(1).format(YMD);
        boolean withinCheckoutWindow = isToday || targetYmd.equals(yesterdayYmd);

        // prafta-app-014: 연차일이 아니면(스케줄 유무 무관) effective 상한(2)으로 액션 산출.
        //   스케줄 없는 날도 실제 출근기록 기준으로 퇴근/추가 출근을 허용한다(D3·D4).
        if (!isLeaveDay) {
            AttdRecordResult s1 = attdBySeq.get(1);
            AttdRecordResult s2 = attdBySeq.get(2);
            boolean s1In = s1 != null;
            boolean s1Out = s1 != null && StringUtils.hasText(s1.checkOutTime());
            boolean s2In = s2 != null;
            boolean s2Out = s2 != null && StringUtils.hasText(s2.checkOutTime());

            int attdCount = (s1In ? 1 : 0) + (s2In ? 1 : 0);
            // 진행 중(출근有 퇴근NULL) 슬롯 존재 여부.
            boolean hasOpen = (s1In && !s1Out) || (s2In && !s2Out);
            // 전 슬롯 퇴근 완료(출근기록이 1건 이상이고 모두 퇴근).
            boolean allCompleted = attdCount > 0 && !hasOpen;

            if (withinCheckoutWindow) {
                // 퇴근: 진행 중 슬롯이 있으면 가능.
                if (hasOpen) {
                    canCheckOut = true;
                } else if (attdCount < 2) {
                    // prafta-app-014 §4-B 6항: 추가 출근 = 직전 슬롯 퇴근 완료 && 실제 출근기록<2 && 미마감.
                    //   1구간 스케줄에서 1회 퇴근 후(attdCount=1, slotCount=1)에도 canCheckIn=true 가 나오는 게 핵심.
                    //   stock §5.2(2구간 1구간 퇴근 후 재출근)도 이 식으로 포괄. attdCount==0(출근 전)도 출근 가능.
                    canCheckIn = !closed;
                }
            }

            // 수정요청: 확정(전 슬롯 출퇴근 완료) && 해당월 미마감.
            //   스케줄 없는 날에도 출퇴근이 모두 완료됐으면 수정요청(보정/초과근무 상신) 활성.
            canRequestModify = allCompleted && !closed;
        }

        return DayActionsResponse.builder()
                .canRequestModify(canRequestModify)
                .canCheckOut(canCheckOut)
                .canCheckIn(canCheckIn)
                .build();
    }

    // ====================================================================
    // 2) 이번주
    // ====================================================================

    @Override
    public MyWeekResponse selectWeek(WeekParam param) {
        String startYmd = param.weekStartYmd();
        LocalDate start = LocalDate.parse(startYmd, YMD);
        LocalDate end = start.plusDays(6);
        String endYmd = end.format(YMD);
        String todayYmd = LocalDate.now().format(YMD);

        AttdRangeQuery q = new AttdRangeQuery(param.cmpnyCd(), param.siteCd(), param.userCd(), startYmd, endYmd);

        Map<String, ScheduleResult> schByYmd = indexSchedule(nullSafe(appAttd01Mapper.selectScheduleByRange(q)));
        Map<String, List<AttdRecordResult>> attdByYmd = indexAttd(nullSafe(appAttd01Mapper.selectAttdByRange(q)));
        Map<String, HolidayResult> holidayByYmd = indexHoliday(nullSafe(appAttd01Mapper.selectHolidaysByRange(q)));
        Map<String, LeaveUseResult> leaveByYmd = expandLeave(nullSafe(appAttd01Mapper.selectLeaveUseByRange(q)), startYmd, endYmd);

        boolean closed = isMonthClosed(param.cmpnyCd(), param.siteCd(), param.userCd(), startYmd.substring(0, 6));
        // 주가 월 경계를 넘으면 종료일 월의 마감도 확인.
        boolean closedEnd = endYmd.substring(0, 6).equals(startYmd.substring(0, 6))
                ? closed
                : isMonthClosed(param.cmpnyCd(), param.siteCd(), param.userCd(), endYmd.substring(0, 6));

        List<WeekDayResponse> days = new ArrayList<>();
        int plannedSum = 0;
        int actualSum = 0;

        for (int i = 0; i < 7; i++) {
            LocalDate d = start.plusDays(i);
            String ymd = d.format(YMD);
            boolean isToday = ymd.equals(todayYmd);
            boolean isPast = ymd.compareTo(todayYmd) < 0;
            boolean dayClosed = ymd.substring(0, 6).equals(startYmd.substring(0, 6)) ? closed : closedEnd;

            ScheduleResult sched = schByYmd.get(ymd);
            boolean isLeaveDay = sched != null && sched.leaveCd() != null && sched.leaveNm() != null;
            boolean isTwoSlot = sched != null && StringUtils.hasText(sched.secSchStrTime());
            boolean hasSchCd = sched != null && !isLeaveDay; // 근무 스케줄(SCH_CD) 존재 여부

            HolidayResult holiday = holidayByYmd.get(ymd);
            LeaveUseResult leave = leaveByYmd.get(ymd);

            List<AttdRecordResult> dayAttds = attdByYmd.getOrDefault(ymd, Collections.emptyList());
            Map<Integer, AttdRecordResult> attdBySeq = new HashMap<>();
            for (AttdRecordResult a : dayAttds) attdBySeq.putIfAbsent(a.workSeq(), a);

            // prafta-app-014: 주간 요약/합계/상태도 effective 기준으로 일관화(1구간 2회 출근 반영).
            int slotCount = isLeaveDay ? 0 : effectiveSlotCount(isTwoSlot, hasSchCd, attdBySeq);

            // 합계: 예정=스케줄 합, 실=완료된 근무(출퇴근 모두 등록)만 합(slot2 기록 있으면 합산).
            if (hasSchCd) {
                plannedSum += plannedMinutes(sched, isTwoSlot);
            }
            actualSum += actualCompletedMinutes(sched, isTwoSlot, attdBySeq);

            String scheduleSummary = hasSchCd ? scheduleSummary(sched, isTwoSlot) : null;
            String attendanceSummary = hasSchCd ? attendanceSummary(attdBySeq, slotCount) : null;
            String attendanceStatus = hasSchCd
                    ? computeAttendanceStatus(sched, slotCount, attdBySeq, isToday, isPast)
                    : null;

            WeekDayActionsResponse actions = computeWeekActions(
                    hasSchCd, isLeaveDay, slotCount, attdBySeq, isToday, isPast, dayClosed);

            days.add(WeekDayResponse.builder()
                    .workYmd(ymd)
                    .dayOfWeek(dayOfWeek(d))
                    .isToday(isToday)
                    .isHoliday(holiday != null)
                    .holidayName(holiday == null ? null : holiday.holidayNm())
                    .isLeaveUsed(leave != null)
                    .leaveTypeName(leave == null ? null : leave.leaveNm())
                    // prafta-app-018-E: 부분연차 상세필드(표시 전용). leaveByYmd 의 첫 1건(putIfAbsent) 기준.
                    .leaveUnitType(leave == null ? null : leave.useUnitType())
                    .leaveTimeRange(leave == null ? null : leaveTimeRange(leave.startTime(), leave.endTime()))
                    .leaveDays(leave == null ? null : leave.leaveDays())
                    .workPlanCode(sched == null ? null : sched.workPlanCd())
                    .workPlanName(resolveWorkPlanName(sched, isLeaveDay))
                    .isTwoSlot(isTwoSlot)
                    .scheduleSummary(scheduleSummary)
                    .attendanceSummary(attendanceSummary)
                    .attendanceStatus(attendanceStatus)
                    .actions(actions)
                    .build());
        }

        WeekSummaryResponse summary = WeekSummaryResponse.builder()
                .plannedWorkMinutes(plannedSum)
                .actualWorkMinutes(actualSum)
                .note(NOTE_COMPLETED_ONLY)
                .build();

        return MyWeekResponse.builder()
                .weekStartYmd(startYmd)
                .weekEndYmd(endYmd)
                .days(days)
                .summary(summary)
                .build();
    }

    /**
     * attendanceStatus 산출 (attd §10.1).
     *
     * <p>prafta-app-014: 지각/조퇴는 "스케줄 대응 슬롯"에만 판정한다(D2). 주간은 hasSchCd 가
     *   true 일 때만 호출되므로 슬롯1은 항상 스케줄 대응이다. 슬롯2 는 스케줄 2구간(isTwoSlot)일
     *   때만 스케줄 대응 → 조퇴 기준 종료시각을 2구간 종료로 본다. 1구간 스케줄의 2번째 출근(추가
     *   근무)은 스케줄 미대응이므로 조퇴 판정 기준에서 제외하고 1구간 종료로 본다. 진행 여부는
     *   실제 마지막 출근기록(마지막 슬롯) 기준으로 본다.
     */
    private String computeAttendanceStatus(
            ScheduleResult sched, int slotCount, Map<Integer, AttdRecordResult> attdBySeq,
            boolean isToday, boolean isPast) {

        boolean isTwoSlot = StringUtils.hasText(sched.secSchStrTime());

        AttdRecordResult s1 = attdBySeq.get(1);
        if (s1 == null) {
            // 출근 자체가 없음: 과거=결근(MISSING), 미래/오늘=미시작(NOT_STARTED).
            return isPast ? ATTD_MISSING : ATTD_NOT_STARTED;
        }
        // 진행 여부 판정: 실제 마지막 출근기록(slot2 가 있으면 slot2, 없으면 slot1) 기준.
        AttdRecordResult last = (slotCount >= 2 && attdBySeq.get(2) != null) ? attdBySeq.get(2) : s1;
        boolean lastOut = StringUtils.hasText(last.checkOutTime());
        if (!lastOut) {
            return ATTD_WORKING; // 진행 중(퇴근 전).
        }

        // 완료: 지각/조퇴 판정. 시:분만 비교하면 야간/자정 넘김(슬롯 시작 00:00, 전일 출근,
        //   익일 퇴근)에서 오판하므로, 웹 Attd_11(PRAFTA-034)과 동일하게 실제 (일자+시각)
        //   타임스탬프로 스케줄 일시와 비교한다.
        // 지각 = 슬롯1 실제 출근일시(출근일자+시각) > 스케줄 시작일시(슬롯1, 항상 스케줄 대응).
        boolean late = isLateStamp(s1, sched.workYmd(), sched.fstSchStrTime());
        // 조퇴 기준 종료시각/대상 슬롯: 스케줄 2구간이면 2구간 종료·slot2 퇴근, 그 외(1구간 스케줄)는
        //   스케줄 대응 마지막 슬롯=slot1 기준(추가 근무 slot2 는 스케줄 미대응 → 조퇴 판정 제외).
        AttdRecordResult lastScheduledSlot = isTwoSlot && attdBySeq.get(2) != null ? attdBySeq.get(2) : s1;
        String schStrForEnd = isTwoSlot ? sched.secSchStrTime() : sched.fstSchStrTime();
        String schEnd = isTwoSlot ? sched.secSchEndTime() : sched.fstSchEndTime();
        // 조퇴 = 실제 퇴근일시 < 스케줄 종료일시. 야간(종료<시작)이면 종료를 익일로 본다.
        boolean early = isEarlyStamp(lastScheduledSlot, sched.workYmd(), schStrForEnd, schEnd);
        if (late) return ATTD_LATE;
        if (early) return ATTD_EARLY_LEAVE;
        return ATTD_NORMAL;
    }

    /**
     * 이번주 바텀시트 4액션 활성도 산출 (계약 §3.2, 매트릭스 UI-A006).
     *
     * <p>prafta-app-013: 산출 로직을 {@link #computeActionFlags} 공용 메서드로 위임한다.
     *   오늘/이번주/이번달 3탭이 동일 규칙(결정 §2)을 쓰도록 통일. 시그니처는 유지.
     */
    private WeekDayActionsResponse computeWeekActions(
            boolean hasSchCd, boolean isLeaveDay, int slotCount,
            Map<Integer, AttdRecordResult> attdBySeq, boolean isToday, boolean isPast, boolean closed) {

        // prafta-app-014: completed 를 effective 기준으로(1구간 2회 출근·둘 다 퇴근해야 완료).
        boolean completed = allSlotsCompleted(slotCount, attdBySeq);
        // hasAttendance = 출근 기록 1건 이상 존재(퇴근/완료 무관, 결정 Q2=a).
        boolean hasAttendance = attdBySeq.get(1) != null || attdBySeq.get(2) != null;

        return computeActionFlags(hasSchCd, isPast, isToday, hasAttendance, completed, closed);
    }

    /**
     * 4액션(+힌트) 게이팅 산출 공용 메서드 (prafta-app-013 결정 §2, 오늘/이번주/이번달 공통).
     *
     * <p>정책 출처: attd §9.2(스케줄 수정)·§9.3(초과근무)·§9.4(연차)·§11.2(근태 보정), 재기획서 §3.2(마감 전까지).
     *
     * <p>프리미티브:
     *   <ul>
     *     <li>hasSchedule: 근무 스케줄(SCH_CD) 존재(연차/휴무-only 제외).</li>
     *     <li>hasAttendance: 출근 기록 1건 이상 존재(퇴근/완료 무관).</li>
     *     <li>completed: 전 구간 출퇴근 완료.</li>
     *     <li>isWorking = hasAttendance && !completed (출근했으나 전 구간 미완료).</li>
     *     <li>closed: 해당 월 근태마감(prafta-028).</li>
     *   </ul>
     *
     * <p>enabled 조건:
     *   <ul>
     *     <li>canRequestScheduleModify = hasSchedule && !closed (과거·현재·미래 모두).</li>
     *     <li>canRequestAttendanceCorrection = !isFuture && !closed && !isWorking.</li>
     *     <li>canRequestOvertime = !isFuture && !closed && hasAttendance && !isWorking.</li>
     *     <li>canRequestLeave = !closed && !hasAttendance (스케줄 무관, 출근기록 있으면 불가).</li>
     *     <li>leaveFullDayOnly = !hasSchedule (연차 폼이 full-day 강제에 소비, Follow-up F1).</li>
     *   </ul>
     * 공통: closed 면 4액션 전부 비활성. 미래 차단은 보정·초과근무에만.
     */
    private WeekDayActionsResponse computeActionFlags(
            boolean hasSchedule, boolean isPast, boolean isToday,
            boolean hasAttendance, boolean completed, boolean closed) {

        boolean isFuture = !isToday && !isPast;
        boolean isWorking = hasAttendance && !completed;

        boolean canScheduleModify = hasSchedule && !closed;
        boolean canAttendanceCorrection = !isFuture && !closed && !isWorking;
        boolean canOvertime = !isFuture && !closed && hasAttendance && !isWorking;
        boolean canLeave = !closed && !hasAttendance;

        return WeekDayActionsResponse.builder()
                .canRequestScheduleModify(canScheduleModify)
                .canRequestAttendanceCorrection(canAttendanceCorrection)
                .canRequestOvertime(canOvertime)
                .canRequestLeave(canLeave)
                .leaveFullDayOnly(!hasSchedule)
                .build();
    }

    // ====================================================================
    // 3) 이번달
    // ====================================================================

    @Override
    public MyMonthResponse selectMonth(MonthParam param) {
        String ym = param.yearMonth();
        LocalDate first = LocalDate.parse(ym + "01", YMD);
        LocalDate last = first.withDayOfMonth(first.lengthOfMonth());
        String fromYmd = first.format(YMD);
        String toYmd = last.format(YMD);
        String todayYmd = LocalDate.now().format(YMD);

        AttdRangeQuery q = new AttdRangeQuery(param.cmpnyCd(), param.siteCd(), param.userCd(), fromYmd, toYmd);

        Map<String, ScheduleResult> schByYmd = indexSchedule(nullSafe(appAttd01Mapper.selectScheduleByRange(q)));
        Map<String, List<AttdRecordResult>> attdByYmd = indexAttd(nullSafe(appAttd01Mapper.selectAttdByRange(q)));
        Map<String, HolidayResult> holidayByYmd = indexHoliday(nullSafe(appAttd01Mapper.selectHolidaysByRange(q)));
        Map<String, LeaveUseResult> leaveByYmd = expandLeave(nullSafe(appAttd01Mapper.selectLeaveUseByRange(q)), fromYmd, toYmd);

        // GPS 미확인 판정을 위해 월 전체 근태 ATTD_ID 의 GPS 일괄 로딩.
        Map<String, GpsResult> gpsByCheckIn = new HashMap<>();
        Map<String, GpsResult> gpsByCheckOut = new HashMap<>();
        List<AttdRecordResult> allMonthAttds = new ArrayList<>();
        attdByYmd.values().forEach(allMonthAttds::addAll);
        loadGps(param.cmpnyCd(), allMonthAttds, gpsByCheckIn, gpsByCheckOut);

        boolean closed = isMonthClosed(param.cmpnyCd(), param.siteCd(), param.userCd(), ym);

        List<MonthDayResponse> days = new ArrayList<>();
        int plannedSum = 0;
        int actualSum = 0;

        for (int day = 1; day <= last.getDayOfMonth(); day++) {
            LocalDate d = first.withDayOfMonth(day);
            String ymd = d.format(YMD);
            boolean isPast = ymd.compareTo(todayYmd) < 0;

            ScheduleResult sched = schByYmd.get(ymd);
            boolean isLeaveDay = sched != null && sched.leaveCd() != null && sched.leaveNm() != null;
            boolean isTwoSlot = sched != null && StringUtils.hasText(sched.secSchStrTime());
            boolean hasSchCd = sched != null && !isLeaveDay;

            HolidayResult holiday = holidayByYmd.get(ymd);
            LeaveUseResult leave = leaveByYmd.get(ymd);

            List<AttdRecordResult> dayAttds = attdByYmd.getOrDefault(ymd, Collections.emptyList());
            Map<Integer, AttdRecordResult> attdBySeq = new HashMap<>();
            for (AttdRecordResult a : dayAttds) attdBySeq.putIfAbsent(a.workSeq(), a);

            // prafta-app-014: 월간도 effective 기준(처리필요/합계에 1구간 2회 출근 반영).
            int slotCount = isLeaveDay ? 0 : effectiveSlotCount(isTwoSlot, hasSchCd, attdBySeq);

            if (hasSchCd) {
                plannedSum += plannedMinutes(sched, isTwoSlot);
            }
            actualSum += actualCompletedMinutes(sched, isTwoSlot, attdBySeq);

            // dayType 우선순위 ACTION_REQUIRED > LEAVE > WORK > OFF.
            boolean actionRequired = isPast && hasSchCd
                    && hasActionRequired(sched, slotCount, attdBySeq, dayAttds, gpsByCheckIn, gpsByCheckOut, closed, ymd);

            String dayType;
            if (actionRequired) {
                dayType = DAY_ACTION_REQUIRED;
            } else if (leave != null) {
                dayType = DAY_LEAVE; // 공휴일 겹쳐도 연차 우선(시안 §3.4).
            } else if (hasSchCd) {
                dayType = DAY_WORK;
            } else {
                dayType = DAY_OFF; // 주말/휴일/스케줄 없음.
            }

            days.add(MonthDayResponse.builder()
                    .workYmd(ymd)
                    .dayType(dayType)
                    .holidayName(holiday == null ? null : holiday.holidayNm())
                    .hasIssue(DAY_ACTION_REQUIRED.equals(dayType))
                    .build());
        }

        MonthSummaryResponse summary = MonthSummaryResponse.builder()
                .plannedWorkMinutes(plannedSum)
                .actualWorkMinutes(actualSum)
                .build();

        return MyMonthResponse.builder()
                .yearMonth(ym)
                .monthlySummary(summary)
                .days(days)
                .build();
    }

    // ====================================================================
    // 4) 셀프 퇴근 (check-out) — 쓰기
    // ====================================================================

    /**
     * 셀프 퇴근(check-out). 정책(prafta-app-003 A0-2 확정 모델):
     *   <ul>
     *     <li>① 셀프 퇴근은 감사이력(TB_USER_ATTD_HIST) 미기록(기존 유지).</li>
     *     <li>② GPS: 위치권한 하드게이트로 좌표는 통상 존재. Mock('Y')은 Param.from 에서 거부.
     *         좌표가 있으면 사업장 지오펜스(중심좌표 vs GPS_RANGE)로 외근 여부 판정.</li>
     *     <li>③ GPS 행 저장 = 지오펜스 "밖(외근)"일 때만(GPS_INFO_TYPE='02'). 안/폴백=정상(미저장).</li>
     *     <li>④ 폴백(A안): 사업장 LAT/LON/GPS_RANGE 결측 또는 클라이언트 좌표 결측 → 온사이트(정상).</li>
     *     <li>퇴근 시각 = 서버 NOW() 기준 raw 실제 시각(표준화 미적용). CHECK_OUT_METHOD='01'.</li>
     *     <li>Low-1: workYmd 가 주어지면 그 일자의 열린건만 대상. 미전달이면 최신 1건 폴백.</li>
     *     <li>D+1 윈도우: WORK_YMD 가 today 또는 today-1 인 열린 근태만. 초과분은 거부(처리필요로 잔존).</li>
     *     <li>사업장 동일성: 출근 레코드 SITE_CD != 세션 siteCd 면 거부(IDOR/스코프 가드).</li>
     *     <li>월마감(prafta-028): 해당 월 마감이면 쓰기 차단(거부).</li>
     *   </ul>
     */
    @Override
    @Transactional
    public MyAttendanceDayResponse checkOut(CheckOutParam param) {
        String cmpnyCd = param.cmpnyCd();
        String siteCd = param.siteCd();
        String userCd = param.userCd();

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String today = now.format(YMD);
        String checkOutTime = now.format(HHMM);     // HHMM (퇴근시간 컬럼 varchar4)
        String apiCallTime = now.format(HHMMSS);    // HHmmss (GPS 측정시간 컬럼 varchar6)
        // D+1 윈도우 하한 = today-1. 이 이상의 열린 근태만 후보로 본다.
        String yesterday = LocalDate.parse(today, YMD).minusDays(1).format(YMD);

        log.info("[attd01] 셀프 퇴근 진입 (userCd={}, today={}, workYmd={})", userCd, today, param.workYmd());

        // 1) 열린 근태(미퇴근) 후보 1건. workYmd 우선(Low-1), 미전달이면 D+1 윈도우 하한 최신 폴백.
        OpenAttdResult open = appAttd01Mapper.selectOpenAttd(cmpnyCd, siteCd, userCd, yesterday, param.workYmd());
        if (open == null) {
            // 퇴근 대상 없음(출근 안 했거나 이미 퇴근, 또는 D+1 초과/workYmd 불일치로 후보 제외).
            log.info("[attd01] 셀프 퇴근 거부: 퇴근 대상 없음 (userCd={})", userCd);
            throw new ApiException(AttdErrorCode.ATTD_404_010);
        }

        // 2) D+1 윈도우 재검증 (today 또는 today-1 만 허용).
        if (!today.equals(open.workYmd()) && !yesterday.equals(open.workYmd())) {
            log.info("[attd01] 셀프 퇴근 거부: D+1 윈도우 초과 (userCd={}, workYmd={})", userCd, open.workYmd());
            throw new ApiException(AttdErrorCode.ATTD_404_010);
        }

        // 3) 사업장 동일성 검증 (세션 siteCd 와 레코드 SITE_CD 비교 — 스코프/IDOR 가드).
        if (!siteCd.equals(open.siteCd())) {
            log.info("[attd01] 셀프 퇴근 거부: 사업장 불일치 (userCd={}, sessionSite={}, recordSite={})",
                    userCd, siteCd, open.siteCd());
            throw new ApiException(AttdErrorCode.ATTD_400_005);
        }

        // 4) 월마감 검증 (prafta-028 부서단위 마감이면 쓰기 차단).
        if (isMonthClosed(cmpnyCd, siteCd, userCd, open.workYmd().substring(0, 6))) {
            log.info("[attd01] 셀프 퇴근 거부: 마감된 기간 (userCd={}, closeYm={})",
                    userCd, open.workYmd().substring(0, 6));
            throw new ApiException(AttdErrorCode.ATTD_400_042);
        }

        // 5) 지오펜스 판정(밖=외근). 출근 레코드의 SITE_CD 기준 사업장 중심좌표 vs GPS_RANGE.
        boolean isOffsite = isOutsideGeofence(cmpnyCd, open.siteCd(), param.lat(), param.lon());

        // 5-1) 외근(지오펜스 밖)인데 사유 미작성이면 차단(prafta-app-008 §7.2~§7.3, P2-D3).
        //      온사이트(범위 안/폴백)면 사유는 무시한다(GPS 행 자체를 저장하지 않음).
        //      UPDATE 전에 차단하여 사유 없는 퇴근 기록이 남지 않게 한다(예외 시 트랜잭션 롤백).
        if (isOffsite && !StringUtils.hasText(param.offsiteReason())) {
            log.info("[attd01] 셀프 퇴근 거부: 외근 사유 미작성 (userCd={}, attdId={})", userCd, open.attdId());
            throw new ApiException(AttdErrorCode.ATTD_400_086);
        }

        // 6) 퇴근 UPDATE (동시성 가드: CHECK_OUT_TIME IS NULL). 0건이면 이미 퇴근됨.
        CheckOutCommand updateCmd = new CheckOutCommand(
                open.attdId(), cmpnyCd, userCd, today, checkOutTime, CHECK_OUT_METHOD_USER, param.deviceUuid());
        int updated = appAttd01Mapper.updateCheckOut(updateCmd);
        if (updated == 0) {
            log.info("[attd01] 셀프 퇴근 거부: 동시성(이미 퇴근됨) (userCd={}, attdId={})", userCd, open.attdId());
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }

        // 7) GPS 행은 "지오펜스 밖(외근)"일 때만 INSERT(GPS_INFO_TYPE='02'). 안/폴백=정상이면 미저장.
        //    (외근 판정 = TB_USER_ATTD_GPS 행 존재 — 기존 웹 attd07 관례와 동일.)
        if (isOffsite) {
            String gpsId = appAttd01Mapper.selectGpsId(cmpnyCd);
            CheckOutGpsCommand gpsCmd = new CheckOutGpsCommand(
                    gpsId, cmpnyCd, open.attdId(), open.siteCd(), userCd, GPS_TYPE_CHECK_OUT,
                    param.lat(), param.lon(), param.accuracy(),
                    today, apiCallTime, param.isMocked(), param.ipAddr(), param.offsiteReason(), userCd);
            appAttd01Mapper.insertCheckOutGps(gpsCmd);
        }

        log.info("[attd01] 셀프 퇴근 완료 (userCd={}, attdId={}, workYmd={}, checkOutTime={}, isOffsite={})",
                userCd, open.attdId(), open.workYmd(), checkOutTime, isOffsite);

        // 8) 갱신된 해당 근무일 카드 재빌드(오늘 탭과 동일 구조) + isOffsite 주입.
        MyAttendanceDayResponse card = buildDayResponse(
                cmpnyCd, siteCd, userCd, resolveSiteName(cmpnyCd, siteCd, param.tokenInfo().gv_siteNm()),
                open.workYmd(), today);
        return withOffsite(card, isOffsite);
    }

    // ====================================================================
    // 5) 셀프 출근 (check-in) — 쓰기
    // ====================================================================

    /**
     * 셀프 출근(check-in). 정책(prafta-app-003 A1 확정 모델 + attd §5/§7):
     *   <ul>
     *     <li>① Mock('Y')은 CheckInParam.from 에서 거부(부정 방지).</li>
     *     <li>② 대상일자: workYmd 미전달이면 서버 today(출근은 통상 당일).</li>
     *     <li>③ 월마감(prafta-028): 해당 월 마감이면 쓰기 차단(거부).</li>
     *     <li>④ 다음날 게이트(사용자 확정): 과거(WORK_YMD&lt;today) 열린 근태가 있으면 차단
     *         ("전날 퇴근을 먼저 등록하세요").</li>
     *     <li>⑤ 스케줄 조회: 연차일(LEAVE)이면 출근 차단(§8.3 노무수령거부).</li>
     *     <li>⑥ 출근횟수/구간 제한(§5.1~§5.4) + 2구간 스케줄 구간 명시 선택(prafta-app-015, 구 §5.5 정정):
     *         하루 최대 2회(prafta-app-014). 2구간 스케줄은 사용자가 출근 구간(targetWorkSeq=1|2)을
     *         선택하며 선택 구간이 곧 WORK_SEQ 가 된다(순서 자유·1구간 누락 허용·각 구간 1회).
     *         구간 미선택→087, 선택 구간 중복→088. 자동추정(구 §5.5 Case A/B/C·084·085)은 폐기.
     *         1구간 스케줄 재출근은 직전 구간 미퇴근이면 차단(081, §5.2).
     *         스케줄 없는 날은 허용(전량 추가근무 대상, §7.5 — 출근 등록까지).</li>
     *     <li>⑦ WORK_SEQ: 2구간 스케줄=선택 구간 직접 채번. 그 외=그 일자 기존 근태 개수 + 1.</li>
     *     <li>⑧ 지오펜스: 밖이면 외근(isOffsite) + GPS행(GPS_INFO_TYPE='01') INSERT. 안/폴백=미저장.</li>
     *     <li>출근 시각 = 서버 NOW() raw(표준화 미적용). CHECK_IN_METHOD='01'(SYS031).</li>
     *   </ul>
     */
    @Override
    @Transactional
    public MyAttendanceDayResponse checkIn(CheckInParam param) {
        String cmpnyCd = param.cmpnyCd();
        String siteCd = param.siteCd();
        String userCd = param.userCd();

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String today = now.format(YMD);
        String checkInTime = now.format(HHMM);    // HHMM (출근시간 컬럼 varchar4)
        String apiCallTime = now.format(HHMMSS);  // HHmmss (GPS 측정시간 컬럼 varchar6)

        // 2) 대상일자: 미전달이면 today.
        String workYmd = (param.workYmd() != null) ? param.workYmd() : today;

        log.info("[attd01] 셀프 출근 진입 (userCd={}, today={}, workYmd={})", userCd, today, workYmd);

        // 0) 동시 출근(TOCTOU) 직렬화: 본인 행 비관적 잠금. 자연키 UNIQUE 부재 보완 —
        //    이후의 count→insert(구간한도/WORK_SEQ) 전 구간을 사용자 단위로 직렬화한다.
        appAttd01Mapper.lockUserForCheckIn(cmpnyCd, siteCd, userCd);

        // 3) 월마감 검증 (prafta-028 부서단위 마감이면 쓰기 차단).
        if (isMonthClosed(cmpnyCd, siteCd, userCd, workYmd.substring(0, 6))) {
            log.info("[attd01] 셀프 출근 거부: 마감된 기간 (userCd={}, closeYm={})", userCd, workYmd.substring(0, 6));
            throw new ApiException(AttdErrorCode.ATTD_400_042);
        }

        // 4) 다음날 게이트(사용자 확정): 과거(전날 이전) 열린 근태가 있으면 출근 차단.
        int pastOpen = appAttd01Mapper.countPastOpenAttd(cmpnyCd, siteCd, userCd, today);
        if (pastOpen > 0) {
            log.info("[attd01] 셀프 출근 거부: 과거 미완료(미퇴근) 근태 존재 → 전날 퇴근 등록 안내 (userCd={}, count={})", userCd, pastOpen);
            throw new ApiException(AttdErrorCode.ATTD_400_082);
        }

        // 5) 스케줄 조회(그 일자). 연차일(LEAVE)이면 출근 차단(§8.3).
        AttdRangeQuery q = new AttdRangeQuery(cmpnyCd, siteCd, userCd, workYmd, workYmd);
        List<ScheduleResult> schedules = nullSafe(appAttd01Mapper.selectScheduleByRange(q));
        ScheduleResult sched = schedules.isEmpty() ? null : schedules.get(0);
        boolean isLeaveDay = sched != null && sched.leaveCd() != null && sched.leaveNm() != null;
        boolean hasSchedule = sched != null && !isLeaveDay;
        boolean isTwoSlot = hasSchedule && StringUtils.hasText(sched.secSchStrTime());

        if (isLeaveDay) {
            log.info("[attd01] 셀프 출근 거부: 연차일 출근 불가 (userCd={}, workYmd={})", userCd, workYmd);
            throw new ApiException(AttdErrorCode.ATTD_400_083);
        }

        // 6) 출근횟수/구간 제한(§5.1~§5.4) + 2구간 스케줄 출근 구간 명시 선택(prafta-app-015, 구 §5.5 정정).
        int existing = appAttd01Mapper.countAttdByYmd(cmpnyCd, siteCd, userCd, workYmd);

        // prafta-app-015: 2구간 스케줄에서 사용자가 선택한 구간(targetWorkSeq)을 그대로 WORK_SEQ 로 채번한다.
        //   selectWorkSeq != null 이면 그 값으로, null 이면 기존 existing+1 채번을 따른다(1구간/스케줄없음).
        Integer selectWorkSeq = null;

        if (hasSchedule) {
            // prafta-app-014: 출근 상한을 스케줄 구간 수 무관 "하루 최대 2회"로 고정(§5.1 정정).
            //   1구간 스케줄도 출근→퇴근 후 추가 1회(2번째=일반 근무) 허용. 2구간 스케줄은 구간별 1회로 최대 2회.
            int maxCheckIn = 2;

            // 초과 출근 차단(+"초과근무 상신"안내, §5.3/§5.4).
            if (existing >= maxCheckIn) {
                log.info("[attd01] 셀프 출근 거부: 하루 출근 상한(2회) 초과 → 초과근무 상신으로 처리 대상 (userCd={}, workYmd={}, existing={}, max={})",
                        userCd, workYmd, existing, maxCheckIn);
                throw new ApiException(AttdErrorCode.ATTD_400_080);
            }

            // ── prafta-app-015: 2구간 스케줄 출근 구간 명시 선택(자동추정·구 §5.5 Case A/B/C 폐기) ──
            //   사용자가 1구간/2구간을 선택하며(targetWorkSeq), 선택 구간이 곧 WORK_SEQ 가 된다.
            //   순서 자유(2구간 먼저 가능)·1구간 누락 허용·각 구간 1회 제한.
            //   잠금(lockUserForCheckIn) 범위 내에서 같은 일자 레코드를 다시 로딩해 구간 중복 INSERT 를 직렬화 방어한다.
            if (isTwoSlot) {
                Integer target = param.targetWorkSeq();
                // (a) 구간 미선택/범위외(Param.from 에서 1·2 만 채택) → 400(구간 선택 필요).
                if (target == null) {
                    log.info("[attd01] 셀프 출근 거부: 2구간 스케줄 구간 미선택 (userCd={}, workYmd={})", userCd, workYmd);
                    throw new ApiException(AttdErrorCode.ATTD_400_087);
                }

                // 잠금 범위 내 같은 일자 근태 로딩 → 구간(WORK_SEQ) 점유 여부 판정(동시 INSERT 중복 방어).
                AttdRangeQuery dayQ = new AttdRangeQuery(cmpnyCd, siteCd, userCd, workYmd, workYmd);
                Map<Integer, AttdRecordResult> attdBySeq = new HashMap<>();
                for (AttdRecordResult a : nullSafe(appAttd01Mapper.selectAttdByRange(dayQ))) {
                    attdBySeq.putIfAbsent(a.workSeq(), a);
                }

                // (b) 선택 구간에 이미 레코드 존재 → 구간 중복 차단(각 구간 1회).
                //   "동일 구간이 진행 중(미퇴근)인데 같은 구간 재출근"(구 081 의미)도 여기서 함께 차단된다.
                //   다른 구간 출근(예: 1구간 미퇴근 + 2구간 출근)은 허용한다(순서 자유 — 구 §5.5 Case B 폐기).
                if (attdBySeq.get(target) != null) {
                    log.info("[attd01] 셀프 출근 거부: 선택 구간 이미 등록됨(구간 중복) (userCd={}, workYmd={}, targetWorkSeq={})",
                            userCd, workYmd, target);
                    throw new ApiException(AttdErrorCode.ATTD_400_088);
                }

                // (c) WORK_SEQ = 선택 구간 직접 채번(순서 무관, existing+1 추정 폐기).
                selectWorkSeq = target;
            } else if (existing >= 1) {
                // 1구간 스케줄의 재출근 시도(§5.2): 직전 구간 미퇴근이면 차단.
                int open = appAttd01Mapper.countOpenAttdByYmd(cmpnyCd, siteCd, userCd, workYmd);
                if (open > 0) {
                    log.info("[attd01] 셀프 출근 거부: 직전 구간 미퇴근 상태 재출근 → 근태 보정 요청 대상 (userCd={}, workYmd={})",
                            userCd, workYmd);
                    throw new ApiException(AttdErrorCode.ATTD_400_081);
                }
            }
        } else {
            // 스케줄 없는 날(§7.5): 출근 허용(전량 추가근무 대상). 단 하루 2회로 상한(prafta-app-014, §7.5 정정).
            //   prafta-app-014 이전: "직전 퇴근만 했으면 무제한" → 2회 캡으로 변경.
            if (existing >= 2) {
                // 2회 초과 출근 차단(+"초과근무 상신"안내). 스케줄 있는 날과 동일 캡.
                log.info("[attd01] 셀프 출근 거부: (스케줄 없는 날) 하루 출근 상한(2회) 초과 (userCd={}, workYmd={}, existing={})",
                        userCd, workYmd, existing);
                throw new ApiException(AttdErrorCode.ATTD_400_080);
            }
            // 직전 미퇴근이면 재출근 차단(중복 출근 방지).
            if (existing >= 1) {
                int open = appAttd01Mapper.countOpenAttdByYmd(cmpnyCd, siteCd, userCd, workYmd);
                if (open > 0) {
                    log.info("[attd01] 셀프 출근 거부: (스케줄 없는 날) 직전 미퇴근 상태 재출근 (userCd={}, workYmd={})",
                            userCd, workYmd);
                    throw new ApiException(AttdErrorCode.ATTD_400_081);
                }
            }
        }

        // 7) WORK_SEQ 채번.
        //    prafta-app-015: 2구간 스케줄은 사용자가 선택한 구간(selectWorkSeq=1|2)을 직접 채번한다(순서 자유).
        //    그 외(1구간 스케줄/스케줄없음)는 기존 existing+1 채번을 따른다.
        int workSeq = (selectWorkSeq != null) ? selectWorkSeq : (existing + 1);

        // 8) 지오펜스 판정(밖=외근). 세션 siteCd 기준 사업장 중심좌표 vs GPS_RANGE.
        boolean isOffsite = isOutsideGeofence(cmpnyCd, siteCd, param.lat(), param.lon());

        // 8-1) 외근(지오펜스 밖)인데 사유 미작성이면 차단(prafta-app-008 §7.2~§7.3, P2-D3).
        //      온사이트(범위 안/폴백)면 사유는 무시한다(GPS 행 자체를 저장하지 않음).
        if (isOffsite && !StringUtils.hasText(param.offsiteReason())) {
            log.info("[attd01] 셀프 출근 거부: 외근 사유 미작성 (userCd={}, workYmd={})", userCd, workYmd);
            throw new ApiException(AttdErrorCode.ATTD_400_086);
        }

        // NODE_CD: JWT 우선, 없으면 사용자 NODE_CD 폴백.
        String nodeCd = StringUtils.hasText(param.nodeCd())
                ? param.nodeCd()
                : appAttd01Mapper.selectUserNodeCd(cmpnyCd, siteCd, userCd);

        // 9) 출근 레코드 INSERT (ATTD_ID 선채번).
        String attdId = appAttd01Mapper.selectAttdId(cmpnyCd);
        CheckInCommand insertCmd = new CheckInCommand(
                attdId, cmpnyCd, siteCd, userCd, workYmd, nodeCd, workSeq,
                today, checkInTime, CHECK_IN_METHOD_USER, param.deviceUuid(), userCd);
        appAttd01Mapper.insertCheckIn(insertCmd);

        // GPS 행은 "지오펜스 밖(외근)"일 때만 INSERT(GPS_INFO_TYPE='01'). 안/폴백=정상이면 미저장.
        if (isOffsite) {
            String gpsId = appAttd01Mapper.selectGpsId(cmpnyCd);
            CheckInGpsCommand gpsCmd = new CheckInGpsCommand(
                    gpsId, cmpnyCd, attdId, siteCd, userCd, GPS_TYPE_CHECK_IN,
                    param.lat(), param.lon(), param.accuracy(),
                    today, apiCallTime, param.isMocked(), param.ipAddr(), param.offsiteReason(), userCd);
            appAttd01Mapper.insertCheckInGps(gpsCmd);
        }

        log.info("[attd01] 셀프 출근 완료 (userCd={}, attdId={}, workYmd={}, workSeq={}, checkInTime={}, isOffsite={})",
                userCd, attdId, workYmd, workSeq, checkInTime, isOffsite);

        // 9-1) PRAFTA-COM-001: 노무수령거부 대상일 출근 감지 + 관리자 PUSH(기능2/3).
        //      전체 try-catch 로 격리 — 감지/알림 실패가 체크인을 롤백/실패시키지 않게 한다(실패 시 log.error).
        try {
            leaveRefusalDetectService.detectAndAlert(cmpnyCd, siteCd, userCd, nodeCd, workYmd, attdId, userCd);
        } catch (Exception e) {
            log.error("[attd01] 노무수령거부 감지 hook 실패(체크인 영향 없음) (userCd={}, workYmd={})", userCd, workYmd, e);
        }

        // 10) 갱신된 해당 근무일 카드 재빌드(오늘 탭과 동일 구조) + isOffsite 주입.
        MyAttendanceDayResponse card = buildDayResponse(
                cmpnyCd, siteCd, userCd, resolveSiteName(cmpnyCd, siteCd, param.tokenInfo().gv_siteNm()),
                workYmd, today);
        return withOffsite(card, isOffsite);
    }

    /**
     * 사업장 지오펜스 판정: 사용자 좌표가 사업장 중심좌표 기준 GPS_RANGE(m)를 초과하면 외근(밖=true).
     *
     * <p>폴백(A안): 사업장 LAT/LON 또는 GPS_RANGE 가 결측(NULL/빈값)이거나 클라이언트 좌표가 결측이면
     *   거리판정 불가 → 온사이트(정상, false)로 간주(GPS행 미저장).
     * <p>거리 계산은 Java haversine(WGS84 평균반경 6371000m)으로 수행한다.
     */
    private boolean isOutsideGeofence(String cmpnyCd, String siteCd, BigDecimal userLat, BigDecimal userLon) {
        // 클라이언트 좌표 결측 → 폴백 온사이트.
        if (userLat == null || userLon == null) {
            return false;
        }
        SiteGeofenceResult geo = appAttd01Mapper.selectSiteGeofence(cmpnyCd, siteCd);
        if (geo == null || geo.lat() == null || geo.lon() == null) {
            // 사업장 중심좌표 미입력 → 폴백 온사이트.
            return false;
        }
        Double rangeM = parseDoubleOrNull(geo.gpsRange());
        if (rangeM == null) {
            // 허용반경 미설정 → 폴백 온사이트.
            return false;
        }
        double distM = haversineMeters(
                userLat.doubleValue(), userLon.doubleValue(),
                geo.lat().doubleValue(), geo.lon().doubleValue());
        // 거리 ≤ 반경 → 안(정상). 초과 → 밖(외근).
        return distM > rangeM;
    }

    /** WGS84 평균반경 기준 두 좌표 간 대권거리(m). */
    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadiusM = 6371000.0d;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusM * c;
    }

    /** 기존 카드 응답에 isOffsite 만 덮어쓴 새 응답을 만든다(빌더 재조립). */
    private MyAttendanceDayResponse withOffsite(MyAttendanceDayResponse src, boolean isOffsite) {
        return MyAttendanceDayResponse.builder()
                .workDate(src.getWorkDate())
                .siteName(src.getSiteName())
                .workPlanCode(src.getWorkPlanCode())
                .workPlanName(src.getWorkPlanName())
                .scheduleSummary(src.getScheduleSummary())
                .attendanceSummary(src.getAttendanceSummary())
                .workStatus(src.getWorkStatus())
                .isTwoSlot(src.isTwoSlot())
                .slotCount(src.getSlotCount())
                .slots(src.getSlots())
                .actions(src.getActions())
                .sheetActions(src.getSheetActions())
                .dayType(src.getDayType())
                .hasIssue(src.isHasIssue())
                .isOffsite(isOffsite)
                .build();
    }

    /**
     * 처리 필요(ACTION_REQUIRED) 판정 — 근태 마감 차단 사유 보유 여부 (attd §13.3).
     *
     * <p>본인 단일 일자 기준으로 산출 가능한 차단 사유:
     *   <ul>
     *     <li>퇴근 미등록: 출근有 && 퇴근NULL (과거일).</li>
     *     <li>GPS 미확인: 해당 일자 근태의 GPS 가 IS_MOCKED='Y' (확인 필요 = 마감 차단, attd §7.3).</li>
     *     <li>출근 미등록(스케줄 있음 + 과거 + 근태 전무)도 보정 필요로 본다.</li>
     *   </ul>
     * <p>한계: "미결 요청"/"미승인 초과근무"(TB_USER_ATTD_REQ)는 본 읽기전용 화면 범위에서
     *   세분 카운트하지 않는다(마감 화면이 단일 출처). 본인 화면은 위 3종 사유로 보수적으로 표시한다.
     */
    private boolean hasActionRequired(
            ScheduleResult sched, int slotCount, Map<Integer, AttdRecordResult> attdBySeq,
            List<AttdRecordResult> dayAttds, Map<String, GpsResult> gpsByCheckIn,
            Map<String, GpsResult> gpsByCheckOut, boolean closed, String ymd) {

        // 마감된 달은 더 이상 처리 필요로 표시하지 않는다(확정됨).
        if (closed) {
            return false;
        }

        // 출근 자체가 없음(과거 근무일 결근) → 보정 필요.
        if (attdBySeq.get(1) == null) {
            return true;
        }
        // prafta-app-014: 퇴근 미등록 판정을 effective 기준으로.
        //   slotCount==2(스케줄 2구간) 면 2구간 출근기록 부재(스케줄상 와야 할 2구간 누락)도 보정 필요.
        //   존재하는 출근기록(slot1·slot2)이 퇴근 누락이면 보정 필요(1구간 2번째 출근의 퇴근 누락 포함).
        AttdRecordResult s1 = attdBySeq.get(1);
        AttdRecordResult s2 = attdBySeq.get(2);
        boolean isTwoSlot = StringUtils.hasText(sched.secSchStrTime());
        if (s1 != null && !StringUtils.hasText(s1.checkOutTime())) return true;
        if (s2 != null && !StringUtils.hasText(s2.checkOutTime())) return true;
        // 스케줄 2구간인데 2구간 출근기록 자체가 없음 → 보정 필요(스케줄상 와야 할 구간 누락).
        if (isTwoSlot && s2 == null) return true;

        // GPS 미확인(IS_MOCKED='Y') → 마감 차단 사유(attd §7.3).
        for (AttdRecordResult a : dayAttds) {
            GpsResult in = gpsByCheckIn.get(a.attdId());
            GpsResult out = gpsByCheckOut.get(a.attdId());
            if (in != null && "Y".equals(in.isMocked())) return true;
            if (out != null && "Y".equals(out.isMocked())) return true;
        }
        return false;
    }

    // ====================================================================
    // 공통 헬퍼: 데이터 로딩 / 인덱싱
    // ====================================================================

    private String resolveSiteName(String cmpnyCd, String siteCd, String tokenSiteNm) {
        if (StringUtils.hasText(tokenSiteNm)) {
            return tokenSiteNm;
        }
        return appAttd01Mapper.selectSiteName(cmpnyCd, siteCd);
    }

    /** 근태레코드 목록의 ATTD_ID 로 출근(01)/퇴근(02) GPS 최신행을 적재. */
    private void loadGps(String cmpnyCd, List<AttdRecordResult> attds,
                         Map<String, GpsResult> gpsByCheckIn, Map<String, GpsResult> gpsByCheckOut) {
        if (attds.isEmpty()) {
            return;
        }
        List<String> attdIds = new ArrayList<>();
        for (AttdRecordResult a : attds) attdIds.add(a.attdId());
        List<GpsResult> gpsList = nullSafe(appAttd01Mapper.selectGpsByAttdIds(cmpnyCd, attdIds));
        // 쿼리가 ATTD_ID+TYPE 별 최신순 정렬 → 먼저 들어온 행(가장 최신)만 유지.
        for (GpsResult g : gpsList) {
            if (STD_RULE_CHECK_IN.equals(g.gpsInfoType())) {
                gpsByCheckIn.putIfAbsent(g.attdId(), g);
            } else if (STD_RULE_CHECK_OUT.equals(g.gpsInfoType())) {
                gpsByCheckOut.putIfAbsent(g.attdId(), g);
            }
        }
    }

    private Map<String, ScheduleResult> indexSchedule(List<ScheduleResult> list) {
        Map<String, ScheduleResult> map = new HashMap<>();
        for (ScheduleResult s : list) map.putIfAbsent(s.workYmd(), s);
        return map;
    }

    private Map<String, List<AttdRecordResult>> indexAttd(List<AttdRecordResult> list) {
        Map<String, List<AttdRecordResult>> map = new LinkedHashMap<>();
        for (AttdRecordResult a : list) {
            map.computeIfAbsent(a.workYmd(), k -> new ArrayList<>()).add(a);
        }
        return map;
    }

    private Map<String, HolidayResult> indexHoliday(List<HolidayResult> list) {
        Map<String, HolidayResult> map = new HashMap<>();
        for (HolidayResult h : list) map.putIfAbsent(h.holidayYmd(), h);
        return map;
    }

    /** 연차 사용행(START~END 범위)을 조회 범위 내 일자별로 펼친다(CONFIRMED 기준). */
    private Map<String, LeaveUseResult> expandLeave(List<LeaveUseResult> list, String fromYmd, String toYmd) {
        Map<String, LeaveUseResult> map = new HashMap<>();
        for (LeaveUseResult lv : list) {
            String s = max(lv.startDate(), fromYmd);
            String e = min(lv.endDate(), toYmd);
            if (s.compareTo(e) > 0) continue;
            LocalDate cur = LocalDate.parse(s, YMD);
            LocalDate end = LocalDate.parse(e, YMD);
            while (!cur.isAfter(end)) {
                map.putIfAbsent(cur.format(YMD), lv);
                cur = cur.plusDays(1);
            }
        }
        return map;
    }

    /**
     * prafta-app-018-E: 부분연차 시각 범위 "HHMM~HHMM" 조합(표시 전용).
     *
     * <p>START_TIME/END_TIME 이 둘 다 있을 때만 range 를 만들고, 하나라도 없으면 null.
     *   콜론(:) 삽입(HHMM→HH:MM)은 FE(attdFormat.js)가 한다. 단위 기준 시각 표시 여부도 FE 판단.
     */
    private String leaveTimeRange(String startTime, String endTime) {
        if (StringUtils.hasText(startTime) && StringUtils.hasText(endTime)) {
            return startTime + "~" + endTime;
        }
        return null;
    }

    /** 해당 월(YYYYMM)이 본인 부서를 덮는 마감으로 닫혀있는지. */
    private boolean isMonthClosed(String cmpnyCd, String siteCd, String userCd, String closeYm) {
        String nodeCd = appAttd01Mapper.selectUserNodeCd(cmpnyCd, siteCd, userCd);
        if (!StringUtils.hasText(nodeCd)) {
            // 소속부서 미상이면 '*'(전체 사업장) 마감만으로 판정.
            nodeCd = "*";
        }
        int covering = appAttd01Mapper.countCoveringClose(cmpnyCd, siteCd, nodeCd, closeYm);
        return covering > 0;
    }

    // ====================================================================
    // 공통 헬퍼: 시간/표준화 계산
    // ====================================================================

    /** HHMM -> 자정 기준 분. 잘못된 값이면 null. */
    private Integer toMinutes(String hhmm) {
        if (hhmm == null || !hhmm.matches("\\d{4}")) {
            return null;
        }
        int h = Integer.parseInt(hhmm.substring(0, 2));
        int m = Integer.parseInt(hhmm.substring(2, 4));
        return h * 60 + m;
    }

    /** 분 -> HHMM (24시 초과는 모듈로 처리, 자정넘김 정산용). */
    private String toHhmm(int minutes) {
        int mm = ((minutes % 1440) + 1440) % 1440;
        return String.format("%02d%02d", mm / 60, mm % 60);
    }

    /**
     * (종료-시작-휴게) 근로분. 자정 넘김(종료<시작)이면 +1440 보정.
     * 입력이 부정확하면 null.
     */
    private Integer computeWorkMinutes(String startHhmm, String endHhmm, Integer breakMin) {
        Integer s = toMinutes(startHhmm);
        Integer e = toMinutes(endHhmm);
        if (s == null || e == null) {
            return null;
        }
        int span = e - s;
        if (span < 0) span += 1440; // 야간 자정 넘김.
        int net = span - (breakMin == null ? 0 : breakMin);
        return Math.max(net, 0);
    }

    /**
     * 표준화 산출(회사 유리 방향: 출근 올림 / 퇴근 내림).
     *
     * <p>출근시각은 unit 경계로 올림(ceil — 더 늦은 시각), 퇴근시각은 내림(floor — 더 이른 시각)하여
     *   근로분이 회사에 유리하게 산정되도록 한다. unit&le;0(표준화 미설정)이면 해당 측은 원본 유지.
     *
     * <p>시간 규칙 검증: 원본의 일자경계 방향(같은 날/자정넘김)을 표준화 후에도 보존해야 한다.
     *   출퇴근 시각이 같거나 매우 짧아 올림/내림 후 표준 출근&ge;표준 퇴근(같은 날 기준)이 되면
     *   규칙 위반으로 보고 표준화를 적용하지 않는다({@code applied=false}). 검증은 toHhmm 의 모듈로(1440)
     *   래핑 이전의 정수 분 단위로 수행한다(올림이 24:00=1440 으로 넘어가는 경계 오판 방지).
     */
    private StandardizedResponse buildStandardized(
            String rawInHhmm, String rawOutHhmm, Integer brkMin, int unitIn, int unitOut) {

        Integer inM = toMinutes(rawInHhmm);
        Integer outM = toMinutes(rawOutHhmm);
        if (inM == null || outM == null) {
            return StandardizedResponse.builder().applied(false).build();
        }

        int stdInM = (unitIn > 0) ? ceilToUnit(inM, unitIn) : inM;     // 출근 올림.
        int stdOutM = (unitOut > 0) ? floorToUnit(outM, unitOut) : outM; // 퇴근 내림.

        // 원본 일자경계 방향(같은 날=출근<퇴근, 자정넘김=출근>퇴근)을 표준화 후에도 유지해야 유효.
        boolean rawOvernight = outM < inM;
        boolean valid = rawOvernight ? (stdOutM < stdInM) : (stdOutM > stdInM);
        if (!valid) {
            return StandardizedResponse.builder().applied(false).build();
        }

        int span = stdOutM - stdInM;
        if (span < 0) span += 1440; // 자정 넘김 보정.
        int net = Math.max(span - (brkMin == null ? 0 : brkMin), 0);

        return StandardizedResponse.builder()
                .applied(true)
                .startTime(toHhmm(stdInM))
                .endTime(toHhmm(stdOutM))
                .settledMinutes(net)
                .build();
    }

    /** 올림: unit(분) 배수 중 m 이상인 최소값. */
    private int ceilToUnit(int m, int unit) {
        return ((m + unit - 1) / unit) * unit;
    }

    /** 내림: unit(분) 배수 중 m 이하인 최대값. */
    private int floorToUnit(int m, int unit) {
        return (m / unit) * unit;
    }

    /** SYS028 룰 타입(01/02)의 표준화 단위(분). 없거나 null 이면 0(미설정). */
    private int unitMinutesOf(List<StdTimeRuleResult> rules, String ruleType) {
        for (StdTimeRuleResult r : rules) {
            if (ruleType.equals(r.stdTimeRuleType())) {
                return r.unitMinutes() == null ? 0 : r.unitMinutes();
            }
        }
        return 0;
    }

    /**
     * 퇴근 미등록(보정 대상) 판정: 스케줄 종료 경과 여부.
     * - 과거일: 항상 경과.
     * - 오늘: 현재 시각 > 스케줄 종료(분).
     * - 미래: false.
     */
    private boolean isCheckOutMissing(String schEndHhmm, boolean isToday, boolean isPast, int nowMinIfToday) {
        if (isPast) {
            return true;
        }
        if (isToday) {
            Integer end = toMinutes(schEndHhmm);
            if (end == null) {
                return false;
            }
            return nowMinIfToday > end;
        }
        return false;
    }

    /**
     * 지각 판정: 실제 출근 일시(출근일자+출근시각) &gt; 스케줄 시작 일시(근무일자+시작시각).
     *
     * <p>시:분만 비교하던 종전 방식은 야간/자정 넘김에서 오판했다. 예) 슬롯 시작이 00:00 이고
     *   근무자가 전일 23:54 에 출근하면 실제로는 6분 전 출근이지만 시:분 비교(2354&gt;0000)로는
     *   지각으로 잘못 잡혔다. 출근일자(CHECK_IN_DATE)를 포함한 통합 분 stamp 로 비교하여
     *   웹 Attd_11(PRAFTA-034) 과 동일 기준으로 판정한다. 시작에는 자정 보정을 적용하지 않는다.
     */
    private boolean isLateStamp(AttdRecordResult attd, String workYmd, String schStrHhmm) {
        if (attd == null || !hasHhmm(attd.checkInTime()) || !hasHhmm(schStrHhmm) || workYmd == null) {
            return false;
        }
        long schStartStamp = toMinuteStamp(workYmd, schStrHhmm);
        String inYmd = StringUtils.hasText(attd.checkInDate()) ? attd.checkInDate() : workYmd;
        long actInStamp = toMinuteStamp(inYmd, attd.checkInTime());
        return actInStamp > schStartStamp;
    }

    /**
     * 조퇴 판정: 실제 퇴근 일시(퇴근일자+퇴근시각) &lt; 스케줄 종료 일시.
     *
     * <p>야간(스케줄 종료 &lt; 시작)이면 종료는 근무일자 익일로 본다(웹 Attd_11 / Attd_08 동일).
     *   퇴근일자(CHECK_OUT_DATE)를 포함한 통합 분 stamp 로 비교한다.
     */
    private boolean isEarlyStamp(AttdRecordResult attd, String workYmd, String schStrHhmm, String schEndHhmm) {
        if (attd == null || !hasHhmm(attd.checkOutTime()) || !hasHhmm(schEndHhmm) || workYmd == null) {
            return false;
        }
        String endYmd = workYmd;
        if (hasHhmm(schStrHhmm) && schEndHhmm.compareTo(schStrHhmm) < 0) {
            endYmd = ymdPlusDays(workYmd, 1); // 야간 자정 넘김: 종료는 익일.
        }
        long schEndStamp = toMinuteStamp(endYmd, schEndHhmm);
        String outYmd = StringUtils.hasText(attd.checkOutDate()) ? attd.checkOutDate() : workYmd;
        long actOutStamp = toMinuteStamp(outYmd, attd.checkOutTime());
        return actOutStamp < schEndStamp;
    }

    /** HHMM(4자리 숫자) 유효성. */
    private boolean hasHhmm(String hhmm) {
        return hhmm != null && hhmm.matches("\\d{4}");
    }

    /** 일자(YYYYMMDD) + 시각(HHmm) 을 1970-01-01 기준 통합 분(minute) stamp 로 환산(웹 Attd_11 동일). */
    private long toMinuteStamp(String ymd, String hhmm) {
        LocalDate d = LocalDate.parse(ymd, YMD);
        int hh = Integer.parseInt(hhmm.substring(0, 2));
        int mm = Integer.parseInt(hhmm.substring(2, 4));
        return d.toEpochDay() * 1440L + (long) hh * 60L + mm;
    }

    /** YYYYMMDD 에 days 일을 더한 YYYYMMDD 반환. */
    private String ymdPlusDays(String ymd, int days) {
        return LocalDate.parse(ymd, YMD).plusDays(days).format(YMD);
    }

    // prafta-app-015: (제거) isSecondSlotTimeWindow / circularMinuteDistance —
    //   2구간 출근 구간 자동추정(구 §5.5 Case A/B/C)이 폐기되어 호출부 0건. 사용자 명시 선택으로 대체됨.

    /**
     * prafta-app-014: effectiveSlotCount 산출(1~2).
     *
     * <p>{@code min( max(scheduleSlots, attdSlots, 1), 2 )}.
     *   scheduleSlots = 스케줄이 정의한 구간 수(2구간=2, 1구간=1, 스케줄없음=0),
     *   attdSlots = 실제 출근기록(WORK_SEQ 1·2) 존재 개수.
     *   스케줄 유무와 무관하게 화면/상태/액션이 일관 동작하도록 항상 1~2 로 캡한다.
     */
    private int effectiveSlotCount(boolean isTwoSlot, boolean hasSchedule, Map<Integer, AttdRecordResult> attdBySeq) {
        int scheduleSlots = isTwoSlot ? 2 : (hasSchedule ? 1 : 0);
        int attdSlots = (attdBySeq.get(1) != null ? 1 : 0) + (attdBySeq.get(2) != null ? 1 : 0);
        int eff = Math.max(Math.max(scheduleSlots, attdSlots), 1);
        return Math.min(eff, 2);
    }

    /**
     * prafta-app-014: effective 기준 "전 슬롯 출퇴근 완료" 판정.
     *   출근기록 수 == slotCount && 모든 출근기록이 퇴근 완료.
     */
    private boolean allSlotsCompleted(int slotCount, Map<Integer, AttdRecordResult> attdBySeq) {
        AttdRecordResult s1 = attdBySeq.get(1);
        AttdRecordResult s2 = attdBySeq.get(2);
        int attdCount = (s1 != null ? 1 : 0) + (s2 != null ? 1 : 0);
        if (attdCount == 0 || attdCount != slotCount) {
            return false;
        }
        if (s1 != null && !StringUtils.hasText(s1.checkOutTime())) return false;
        if (s2 != null && !StringUtils.hasText(s2.checkOutTime())) return false;
        return true;
    }

    /** 스케줄 예정 근로분(1구간 + (2구간)). */
    private int plannedMinutes(ScheduleResult sched, boolean isTwoSlot) {
        int total = 0;
        Integer m1 = computeWorkMinutes(sched.fstSchStrTime(), sched.fstSchEndTime(), parseIntOrZero(sched.fstSchBrkMin()));
        if (m1 != null) total += m1;
        if (isTwoSlot) {
            Integer m2 = computeWorkMinutes(sched.secSchStrTime(), sched.secSchEndTime(), parseIntOrZero(sched.secSchBrkMin()));
            if (m2 != null) total += m2;
        }
        return total;
    }

    /**
     * 완료된 근무(출퇴근 모두 등록)만 합산한 실 근로분 (시안 §3.7).
     * 표준화 단위는 합계 산정에 적용하지 않고 실제 출퇴근 기준으로 (퇴근-출근-휴게)를 누적한다.
     * 스케줄 휴게분을 공제(attd §10.4 자동 휴게공제는 정규 근무시간 기준).
     */
    private int actualCompletedMinutes(ScheduleResult sched, boolean isTwoSlot, Map<Integer, AttdRecordResult> attdBySeq) {
        int total = 0;
        AttdRecordResult s1 = attdBySeq.get(1);
        if (s1 != null && StringUtils.hasText(s1.checkOutTime())) {
            Integer brk = sched == null ? 0 : parseIntOrZero(sched.fstSchBrkMin());
            Integer m = computeWorkMinutes(s1.checkInTime(), s1.checkOutTime(), brk);
            if (m != null) total += m;
        }
        // prafta-app-014 §4-B 8항: 실근로 합계는 실제 출퇴근 기준이므로 slot2 기록이 있으면 무조건 합산
        //   (isTwoSlot 게이트 제거). 1구간 스케줄/스케줄 없는 날의 2번째 근무도 합계에 포함.
        //   휴게 공제는 스케줄 2구간일 때만 SEC 휴게를 적용(스케줄 미대응 슬롯은 공제 없음).
        AttdRecordResult s2 = attdBySeq.get(2);
        if (s2 != null && StringUtils.hasText(s2.checkOutTime())) {
            Integer brk = (isTwoSlot && sched != null) ? parseIntOrZero(sched.secSchBrkMin()) : 0;
            Integer m = computeWorkMinutes(s2.checkInTime(), s2.checkOutTime(), brk);
            if (m != null) total += m;
        }
        return total;
    }

    /** 스케줄 요약 문자열 "0930~1800" 또는 2구간 "0700~1300 / 1700~2100". */
    private String scheduleSummary(ScheduleResult sched, boolean isTwoSlot) {
        StringBuilder sb = new StringBuilder();
        sb.append(nz(sched.fstSchStrTime())).append("~").append(nz(sched.fstSchEndTime()));
        if (isTwoSlot) {
            sb.append(" / ").append(nz(sched.secSchStrTime())).append("~").append(nz(sched.secSchEndTime()));
        }
        return sb.toString();
    }

    /**
     * 근태 요약 문자열. 미래/미생성(근태 전무)이면 null.
     * 진행 중(퇴근 전)은 종료 자리에 빈 값을 두지 않고 출근 시각만 노출(프론트가 "근무중" 표기).
     */
    private String attendanceSummary(Map<Integer, AttdRecordResult> attdBySeq, int slotCount) {
        AttdRecordResult s1 = attdBySeq.get(1);
        AttdRecordResult s2 = attdBySeq.get(2);
        // prafta-app-015: 2구간 먼저 출근(WORK_SEQ=2 단독, s1==null·s2!=null)이 정식 도달 가능.
        //   기존엔 s1==null 이면 즉시 null 반환(요약 공란) → s2 단독일 때도 요약을 산출한다.
        if (s1 == null && s2 == null) {
            return null; // 근태 미생성.
        }
        if (s1 == null) {
            // 2구간 단독: 2구간 시각만 단독 표기(s1 존재 경로의 "1 / 2" 포맷은 보존).
            return nz(s2.checkInTime()) + "~" + (s2.checkOutTime() == null ? "" : s2.checkOutTime());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(nz(s1.checkInTime())).append("~").append(s1.checkOutTime() == null ? "" : s1.checkOutTime());
        // prafta-app-014: slot2 요약은 실제 출근기록 존재 기준(스케줄 구간 수 무관) — 1구간 2회 출근도 포함.
        if (slotCount >= 2 && s2 != null) {
            sb.append(" / ").append(nz(s2.checkInTime())).append("~")
                    .append(s2.checkOutTime() == null ? "" : s2.checkOutTime());
        }
        return sb.toString();
    }

    private String resolveWorkPlanName(ScheduleResult sched, boolean isLeaveDay) {
        if (sched == null) {
            return null;
        }
        if (isLeaveDay) {
            return sched.leaveNm(); // 연차일은 연차명.
        }
        // 근무일: 스케줄 한글명 컬럼이 스키마에 없어 SCH_NO 사용(plan 확정 §1).
        return sched.schNo();
    }

    // ====================================================================
    // 소규모 유틸 (모듈 내부 전용 — common.util 승격 대상 아님)
    // ====================================================================

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    private Integer parseIntOrZero(String s) {
        Integer v = parseIntOrNull(s);
        return v == null ? 0 : v;
    }

    private Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 문자열(GPS_RANGE varchar 등)을 Double 로 파싱. 빈값/NULL/형식오류면 null. */
    private Double parseDoubleOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return Double.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private String max(String a, String b) {
        return a.compareTo(b) >= 0 ? a : b;
    }

    private String min(String a, String b) {
        return a.compareTo(b) <= 0 ? a : b;
    }

    private int nowMinutes() {
        java.time.LocalTime t = java.time.LocalTime.now();
        return t.getHour() * 60 + t.getMinute();
    }

    /** LocalDate -> 요일 약어 (MON~SUN). */
    private String dayOfWeek(LocalDate d) {
        DayOfWeek dow = d.getDayOfWeek();
        switch (dow) {
            case MONDAY:    return "MON";
            case TUESDAY:   return "TUE";
            case WEDNESDAY: return "WED";
            case THURSDAY:  return "THU";
            case FRIDAY:    return "FRI";
            case SATURDAY:  return "SAT";
            case SUNDAY:    return "SUN";
            default:        return "";
        }
    }
}
