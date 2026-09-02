package com.prafta.app.attd.attd01.service.impl;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.prafta.app.attd.attd01.dto.response.AppliedOvertimeItem;
import com.prafta.app.attd.attd01.dto.response.AttendanceResponse;
import com.prafta.app.attd.attd01.dto.response.DayActionsResponse;
import com.prafta.app.attd.attd01.dto.response.LeaveMarkerItem;
import com.prafta.app.attd.attd01.dto.response.MonthDayResponse;
import com.prafta.app.attd.attd01.dto.response.MonthSummaryResponse;
import com.prafta.app.attd.attd01.dto.response.LeaveExemptWindowItem;
import com.prafta.app.attd.attd01.dto.response.MyAttendanceDayResponse;
import com.prafta.app.attd.attd01.dto.response.MyMonthResponse;
import com.prafta.app.attd.attd01.dto.response.MyWeekResponse;
import com.prafta.app.attd.attd01.dto.response.ScheduleResponse;
import com.prafta.app.attd.attd01.dto.response.SlotResponse;
import com.prafta.app.attd.attd01.dto.response.WeekDayActionsResponse;
import com.prafta.app.attd.attd01.dto.response.WeekDayResponse;
import com.prafta.app.attd.attd01.dto.response.WeekSummaryResponse;
import com.prafta.app.attd.attd01.mapper.AppAttd01Mapper;
import com.prafta.app.attd.attd01.result.AttdRecordResult;
import com.prafta.app.attd.attd01.result.GpsResult;
import com.prafta.app.attd.attd01.result.HolidayResult;
import com.prafta.app.attd.attd01.result.LeaveUseResult;
import com.prafta.app.attd.attd01.result.OpenAttdResult;
import com.prafta.app.attd.attd01.result.RangeOvertimeResult;
import com.prafta.app.attd.attd01.result.ScheduleResult;
import com.prafta.app.attd.attd01.result.SiteGeofenceResult;
import com.prafta.app.attd.attd01.service.AppAttd01Service;
import com.prafta.app.tbm.tbm01.service.AppTbm01Service;
import com.prafta.common.cmm.leave.service.LeaveRefusalConst;
import com.prafta.common.cmm.leave.service.LeaveRefusalDetectService;
import com.prafta.common.cmm.leave.util.PartialLeaveWindowUtils;
import com.prafta.common.cmm.schedule.util.FixedOtScheduleUtils;
import com.prafta.common.cmm.location.service.LocationConsentService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.location.LocationErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.crypto.GpsCoordCrypto;
import com.prafta.common.util.AttdOverlapMessages;
import com.prafta.common.util.AttdOverlapUtils;
import com.prafta.common.util.DateTimeUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * prafta-app-002: 앱 본인 근태조회(attd01) 서비스 구현.
 *
 * <p>설계 원칙:
 *   <ul>
 *     <li>Mapper 는 원천 데이터(스케줄/근태/GPS/휴일/연차/마감)만 로딩한다.</li>
 *     <li>workStatus / 액션 활성도 / dayType / 합계 산출은 전부 본 서비스에서 수행한다.</li>
 *     <li>식별자(cmpnyCd/siteCd/userCd)는 Param 에서 JWT 출처로 캐노니컬라이즈된 값만 사용(IDOR 가드).</li>
 *   </ul>
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

    // workStatus 값
    private static final String STATUS_BEFORE_WORK = "BEFORE_WORK";
    private static final String STATUS_WORKING = "WORKING";
    private static final String STATUS_TWO_SLOT_WORKING = "TWO_SLOT_WORKING";
    private static final String STATUS_CHECKED_OUT = "CHECKED_OUT";
    private static final String STATUS_CHECK_OUT_MISSING = "CHECK_OUT_MISSING";
    // 과거 2구간 스케줄에서 일부 구간만 근태가 있고 전 구간이 완료되지 않은 상태(근태 누락).
    private static final String STATUS_ATTD_MISSING = "ATTD_MISSING";

    // attendanceStatus 값 (week)
    private static final String ATTD_NORMAL = "NORMAL";
    private static final String ATTD_LATE = "LATE";
    private static final String ATTD_EARLY_LEAVE = "EARLY_LEAVE";
    private static final String ATTD_MISSING = "MISSING";
    private static final String ATTD_WORKING = "WORKING";
    private static final String ATTD_NOT_STARTED = "NOT_STARTED";

    /** 사용 단위 [SYS025] '01' 반차 — HB-05 지각·조퇴 판정 보정 대상(시각 보유분 한정). */
    private static final String LEAVE_UNIT_HALF_DAY = "01";

    /**
     * 지각·조퇴 판정 면제 대상 사용 단위 [SYS025] — 반차('01') + 시간차('02'/'03'/'04').
     * ★2026-08-17 사용자 확정(HB-05 §2 비목표 해제): 소정 경계에 붙은 시간차가 조퇴로
     * 오판정되던 문제 — 시각 보유 부분연차 전부를 판정 면제 입력에 포함한다(웹 Attd_08/11 동일).
     */
    private static final java.util.Set<String> JUDGE_EXEMPT_LEAVE_UNITS =
            java.util.Set.of("01", "02", "03", "04");

    // dayType 값 (month)
    private static final String DAY_WORK = "WORK";
    private static final String DAY_LEAVE = "LEAVE";
    private static final String DAY_OFF = "OFF";
    private static final String DAY_ACTION_REQUIRED = "ACTION_REQUIRED";

    // prafta-com-008-E-2: 연차 사용 단위(SYS025) — '00'=종일. 종일 연차만 출근 차단/슬롯0(근무일 미부여).
    private static final String LEAVE_UNIT_FULL = "00";

    private final AppAttd01Mapper appAttd01Mapper;

    /** PRAFTA-COM-001: 노무수령거부 대상일 출근 감지 + 관리자 PUSH(체크인 hook 전용, 예외 격리). */
    private final LeaveRefusalDetectService leaveRefusalDetectService;

    /** PRAFTA-APP-021-3c(M1): 지각/조퇴 raw 감지 + 노드 관리자 PUSH(체크인/체크아웃 hook, 예외 격리). */
    private final com.prafta.common.cmm.push.AttdLateEarlyNotiService attdLateEarlyNotiService;

    /** prafta-app-022-6: 퇴근 시 진행중 TBM 자동 중도퇴실(attd01 → tbm01 단방향, best-effort). */
    private final AppTbm01Service appTbm01Service;

    /** GPS좌표-암호화-전환-02: 좌표 AES-GCM 암호화(저장 전용 — 지오펜스 판정은 요청 평문 좌표 그대로 사용). */
    private final GpsCoordCrypto gpsCoordCrypto;

    /** 위치정보 동의 판정 단일 출처(위치정보 동의철회·중지 S3) — 미동의 시 좌표를 저장하지 않는다. */
    private final LocationConsentService locationConsentService;

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

        // 휴일(웹 휴일관리 TB_HOLIDAY) — 오늘/일상세도 주/월과 동일하게 휴일을 반영(표시 전용).
        HolidayResult holiday = indexHoliday(nullSafe(appAttd01Mapper.selectHolidaysByRange(q))).get(targetYmd);

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
        boolean closed = isMonthClosed(cmpnyCd, siteCd, resolveNodeCdForClose(cmpnyCd, siteCd, userCd), targetYmd.substring(0, 6));
        // 초과근무(등록 or 신청) 보유 여부 — 스케줄 수정 요청 게이팅(보유일은 스케줄 변경 차단). 단일일 조회.
        boolean hasOvertime = !nullSafe(appAttd01Mapper.selectOvertimeYmds(q)).isEmpty();
        // prafta-app-030 후속: 그날 적용(승인) 초과근무 실적(표시 전용). 단일일이라 그룹 불필요 — 전체가 targetYmd.
        List<AppliedOvertimeItem> appliedOvertimes =
                toOvertimeItems(nullSafe(appAttd01Mapper.selectAppliedOvertimesByRange(q)));

        // 작업지시서_소속이동-이력가시성-보정(QA High 보정): 동일 targetYmd 에 SITE_CD 가 다른 WP 행이
        //   공존할 수 있어(소속이동 시 구 사업장 DEFAULT_SCH 행 잔존 등) 첫 행을 그대로 채택하면 실행계획에
        //   따라 비결정적으로 소실된다. indexSchedule 과 동일한 우선순위 규칙으로 결정론적으로 선택한다.
        ScheduleResult sched = indexSchedule(schedules, siteCd).get(targetYmd);
        // prafta-com-008-E-2: 연차일 판정을 work_plan(LEAVE_CD) → leave_use(종일 확정) 로 전환.
        boolean isLeaveDay = isFullDayLeave(dayLeave);
        // 작업지시서_연차변경화면_진입버튼: 연차 이동 가능 여부(부분연차 포함, isLeaveDay 무관).
        //   selectMovableLeaves(Attd13Mapper) 와 동일 조건 — CONFIRMED(쿼리 필터) + START_DATE >= todayYmd.
        boolean leaveMovable = dayLeave != null
                && StringUtils.hasText(dayLeave.leaveId())
                && dayLeave.startDate().compareTo(todayYmd) >= 0;
        boolean isTwoSlot = sched != null && StringUtils.hasText(sched.secSchStrTime());
        // 스케줄 존재 = SCH_CD 매칭까지 요구. selectScheduleByRange 는 근무계획(WP) 행 기준 LEFT JOIN 이라
        //   WORK_PLAN_CD NULL(휴무 지정)/스케줄 미매치 행도 sched 가 non-null 로 오지만 스케줄 없는 날이다.
        boolean hasScheduleDay = sched != null && sched.schCd() != null && !isLeaveDay;

        // workSeq -> 근태레코드 매핑.
        // 작업지시서_소속이동-이력가시성-보정(QA High 보정): 동일 targetYmd+workSeq 에 SITE_CD 가
        //   다른 근태행이 공존할 수 있어 putIfAbsent(선착순)면 실행계획에 따라 비결정적으로 소실된다.
        //   세션 SITE_CD 일치 우선 → 동률이면 최신 갱신시각 우선(mergeAttdBySeq)으로 결정론화한다.
        Map<Integer, AttdRecordResult> attdBySeq = mergeAttdBySeq(attds, siteCd);

        // prafta-app-014: 화면/상태/액션의 단일 기준 = effectiveSlotCount(1~2).
        //   연차일은 슬롯 비대상(0). 스케줄도 출근기록도 없는 순수 휴무일도 0(종전과 동일하게 슬롯 미생성).
        //   스케줄 없는 날도 실제 출근기록이 있으면 슬롯을 만든다(초과근무 슬롯, D2).
        boolean hasAnyAttd = attdBySeq.get(1) != null || attdBySeq.get(2) != null;
        int slotCount = (isLeaveDay || (!hasScheduleDay && !hasAnyAttd))
                ? 0
                : effectiveSlotCount(isTwoSlot, hasScheduleDay, attdBySeq);

        boolean isPast = targetYmd.compareTo(todayYmd) < 0;
        boolean isToday = targetYmd.equals(todayYmd);
        int nowMinIfToday = isToday ? nowMinutes() : -1;

        // prafta-app-015: 2구간 스케줄 구간 선택 버튼 게이팅(서버 산출, 프론트 표시 전용).
        //   퇴근 가능 윈도우 = 근무일 당일(D) ~ 다음날(D+1). 같은 구간 진행중(미퇴근) 여부도 반영한다.
        String yesterdayYmdForSlot = LocalDate.parse(todayYmd, YMD).minusDays(1).format(YMD);
        boolean withinCheckoutWindowForSlot = isToday || targetYmd.equals(yesterdayYmdForSlot);
        int attdCountForSlot = (attdBySeq.get(1) != null ? 1 : 0) + (attdBySeq.get(2) != null ? 1 : 0);
        // prafta-app-024: 진행 중(출근有·퇴근NULL) 구간이 하나라도 있으면 다른 구간 출근 게이팅을 닫는다.
        //   서버 강제(checkIn 의 081)와 표시 플래그를 정합. 이미 로딩된 attdBySeq 재사용(추가 쿼리 불요).
        boolean hasOpenSlot = attdBySeq.values().stream()
                .anyMatch(a -> a != null && !StringUtils.hasText(a.checkOutTime()));
        // PRAFTA-APP-024 후속: 2구간 기록 존재 시 1구간 역순 출근 불가(표시 플래그를 서버 089 가드와 정합).
        boolean slot2CheckedIn = attdBySeq.get(2) != null;

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
                        && attdCountForSlot < 2
                        && !hasOpenSlot // prafta-app-024: 다른 구간 진행 중이면 구간 출근 불가(서버 081 정합).
                        && !(seq == 1 && slot2CheckedIn); // PRAFTA-APP-024 후속: 2구간 등록 시 1구간 역순 출근 불가.

                slots.add(buildSlot(sched, seq, slotHasSchedule, slotAttd,
                        gpsByCheckIn, gpsByCheckOut, isToday, isPast, nowMinIfToday,
                        canCheckInThisSlot, alreadyCheckedIn));
            }
        }

        String workStatus = computeWorkStatus(sched, isLeaveDay, slotCount, attdBySeq, isToday, isPast, nowMinIfToday);

        DayActionsResponse actions = computeDayActions(
                sched, isLeaveDay, attdBySeq, isToday, isPast, closed, targetYmd, todayYmd);

        // prafta-app-013: 오늘/일상세 바텀시트 4액션(+leaveFullDayOnly) — 이번주와 동일 규칙(결정 §2).
        // prafta-app-031: 액션 게이팅의 "근무중"은 진행 중(퇴근 안 한) 슬롯 존재 기준(hasOpenSlot, 라인 205 재사용).
        //   2구간 중 1구간만 정상 근무(출근+퇴근)한 날은 근무중이 아니므로 근태 보정/초과근무를 허용한다.
        boolean hasSchedule = hasScheduleDay;
        boolean hasAttendanceForSheet = attdBySeq.get(1) != null || attdBySeq.get(2) != null;
        WeekDayActionsResponse sheetActions = computeActionFlags(
                hasSchedule, hasOvertime, isPast, isToday, hasAttendanceForSheet, hasOpenSlot, closed);

        String workPlanName = resolveWorkPlanName(sched, isLeaveDay,
                dayLeave == null ? null : dayLeave.leaveNm());

        // prafta-app-013: 시트 메타 1줄(이번주와 동일) — 근무 스케줄 있을 때만 산출.
        String scheduleSummary = hasSchedule ? scheduleSummary(sched, isTwoSlot) : null;
        // PRAFTA-FIXEDOT-2(표기): 고정연장 요약(소정과 구분 — FE 가 "고정연장" 라벨 부착).
        String fixedOtSummary = hasSchedule ? fixedOtSummary(sched) : null;
        String attendanceSummary = hasSchedule ? attendanceSummary(attdBySeq, slotCount) : null;

        // dayType/hasIssue 산출 — selectMonth 와 동일 규칙(처리 필요 빠른 액션 노출 근거, 시안 §4.4.3).
        boolean hasSchCd = hasScheduleDay;
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

        // 작업지시서_소속이동-이력가시성-보정 T3 데이터 소스: 그 날짜의 "당시 소속" — 실 근태기록(SITE_CD)이
        // 있으면 그 값을 우선(1구간 없으면 2구간), 없으면 그 날 배정 스케줄(WP.SITE_CD)로 폴백. 둘 다 없으면 null(배지 미노출).
        AttdRecordResult primaryAttdForSite = attdBySeq.get(1) != null ? attdBySeq.get(1) : attdBySeq.get(2);
        String recordSiteCd = null;
        String recordSiteName = null;
        if (primaryAttdForSite != null && StringUtils.hasText(primaryAttdForSite.siteCd())) {
            recordSiteCd = primaryAttdForSite.siteCd();
            recordSiteName = primaryAttdForSite.siteNm();
        } else if (sched != null && StringUtils.hasText(sched.siteCd())) {
            recordSiteCd = sched.siteCd();
            recordSiteName = sched.siteNm();
        }

        return MyAttendanceDayResponse.builder()
                .workDate(targetYmd)
                .siteName(siteName)
                .workPlanCode(sched == null ? null : sched.workPlanCd())
                .workPlanName(workPlanName)
                .scheduleSummary(scheduleSummary)
                .fixedOtSummary(fixedOtSummary)
                .attendanceSummary(attendanceSummary)
                .workStatus(workStatus)
                .isTwoSlot(isTwoSlot)
                .slotCount(slotCount)
                .slots(slots)
                .actions(actions)
                .sheetActions(sheetActions)
                .dayType(dayType)
                .hasIssue(DAY_ACTION_REQUIRED.equals(dayType))
                .isHoliday(holiday != null)
                .holidayName(holiday == null ? null : holiday.holidayNm())
                // prafta-app-018-E: 부분연차 상세필드(표시 전용·근무일 유지). 사용내역 없으면 전부 null/false.
                .isLeaveUsed(dayLeave != null)
                .leaveTypeName(dayLeave == null ? null : dayLeave.leaveNm())
                .leaveUnitType(dayLeave == null ? null : dayLeave.useUnitType())
                .leaveTimeRange(dayLeave == null ? null : leaveTimeRange(dayLeave.startTime(), dayLeave.endTime()))
                .leaveDays(dayLeave == null ? null : dayLeave.leaveDays())
                // 작업지시서_연차변경화면_진입버튼: 이동 발의 식별자 + 이동 가능 여부.
                .leaveId(dayLeave == null ? null : dayLeave.leaveId())
                .leaveMovable(leaveMovable)
                // PRAFTA_COM_002-B-1: 단건 스칼라(첫 1건) 요청중 여부(다건은 leaves[].pendingApproval).
                .leavePending(isLeavePending(dayLeave))
                // 같은 날 부분연차 다건 표시용 마커 목록(시각순). dayLeaves 전체를 매핑(단건 스칼라는 첫 1건 하위호환).
                .leaves(toLeaveMarkers(dayLeaves))
                // OT 칩 정합(2026-08-08): 면제 구간(검증 ATTD_400_196 과 동일 산식) — OT 폼 칩이 그대로 뺀다.
                .leaveExemptWindows(toLeaveExemptWindows(targetYmd, dayLeaves, sched))
                // PRAFTA-FIXEDOT-2: 고정연장 점유 구간 — OT 폼 칩이 피감수에 합친다(서버 검증 정합).
                .fixedOtWindows(toFixedOtWindows(targetYmd, sched))
                // prafta-app-030 후속: 그날 적용(승인) 초과근무 목록(없으면 빈 리스트).
                .appliedOvertimes(appliedOvertimes)
                // 작업지시서_소속이동-이력가시성-보정 T3: "당시 소속" 배지 데이터 소스.
                .recordSiteCd(recordSiteCd)
                .recordSiteName(recordSiteName)
                .build();
    }

    /**
     * 구간(슬롯) 응답 빌드.
     *
     * <p>prafta-app-014: {@code hasScheduleForSlot} 가 false 면 "스케줄 미대응 슬롯"(스케줄 없는 날의
     *   슬롯, 1구간 스케줄의 2번째 출근 등)으로 보고 {@code schedule=null} 로 내리며 지각·조퇴를
     *   적용하지 않는다(원본 출퇴근 시각만). 미등록 판정도 스케줄 종료가 없으므로 적용하지 않는다(D2).
     */
    private SlotResponse buildSlot(
            ScheduleResult sched, int seq, boolean hasScheduleForSlot, AttdRecordResult attd,
            Map<String, GpsResult> gpsByCheckIn, Map<String, GpsResult> gpsByCheckOut,
            boolean isToday, boolean isPast, int nowMinIfToday,
            boolean canCheckInThisSlot, boolean alreadyCheckedIn) {

        // 스케줄 미대응 슬롯: 스케줄 메타/미등록 판정 비대상.
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
        }

        return SlotResponse.builder()
                .workSeq(seq)
                .schedule(scheduleRes)
                .attendance(attendanceRes)
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
            // prafta-app-032: 과거일인데 전 구간이 완료(출퇴근 모두)되지 않았고 일부 구간에 근태가 있으면
            //   "근태 누락"으로 본다(예: 1구간 출퇴근만 등록·2구간 스케줄은 있으나 출근기록 없음, 또는
            //   1구간 퇴근 미등록). 진행 중(오늘) 2구간은 종전대로 근무중(TWO_SLOT_WORKING).
            if (isPast && (s1In || s2In)) return STATUS_ATTD_MISSING;
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
     *   활성도 매트릭스(시안 §3.1, prafta-app-026 갱신):
     *   <ul>
     *     <li>canCheckOut: (targetYmd==today || targetYmd==today-1) && 미마감 && (진행 중 구간 || 출근기록&gt;0).
     *         tail 슬롯이 이미 퇴근완료여도 윈도우 내·미마감이면 true(퇴근시각 재등록, last-write-wins).</li>
     *     <li>canCheckIn: 진행 중 구간 없음 && 출근기록&lt;2 && 미마감 (최초 출근 ∪ 재량 2회차).
     *         canCheckOut 와 더 이상 배타적이지 않다(FE 가 canCheckOut 우선·canCheckIn 보조로 표시).</li>
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
                // prafta-app-026: 퇴근 가능 = 윈도우 내 && 미마감 && (진행 중 슬롯 존재 || 출근기록>0).
                //   진행 중 슬롯이 있으면 최초 퇴근, tail 슬롯이 이미 퇴근완료여도 "퇴근시각 재등록"으로 열어 둔다.
                //   !closed 가드 추가: 마감 시 쓰기 차단(checkOut 의 ATTD_400_042)과 정합(종전 hasOpen 분기엔 가드 없었음).
                if ((hasOpen || attdCount > 0) && !closed) {
                    canCheckOut = true;
                }
                // 추가/최초 출근(canCheckIn): 진행 중 슬롯이 없고 출근기록<2 && 미마감.
                //   prafta-app-014 §4-B 6항: attdCount==0(출근 전 최초 출근) ∪ attdCount==1(직전 슬롯 퇴근 후 재량 2회차).
                //   prafta-app-026: 더 이상 canCheckOut 와 배타적이지 않다(전 슬롯 퇴근완료 시 canCheckOut+canCheckIn 동시 true).
                //   FE 가 canCheckOut 우선·canCheckIn 보조로 표시(2회차 출근 보조 버튼).
                if (!hasOpen && attdCount < 2) {
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

        // 작업지시서_소속이동-이력가시성-보정(QA High 보정): 세션 SITE_CD 를 넘겨 동일 WORK_YMD 다건
        //   공존 시 결정론적으로 병합한다(indexSchedule 참조).
        Map<String, ScheduleResult> schByYmd =
                indexSchedule(nullSafe(appAttd01Mapper.selectScheduleByRange(q)), param.siteCd());
        Map<String, List<AttdRecordResult>> attdByYmd = indexAttd(nullSafe(appAttd01Mapper.selectAttdByRange(q)));
        Map<String, HolidayResult> holidayByYmd = indexHoliday(nullSafe(appAttd01Mapper.selectHolidaysByRange(q)));
        List<LeaveUseResult> weekLeaves = nullSafe(appAttd01Mapper.selectLeaveUseByRange(q));
        Map<String, LeaveUseResult> leaveByYmd = expandLeave(weekLeaves, startYmd, endYmd);
        // 같은 날 부분연차 다건 표시용(시각순 마커). 단건 leaveByYmd 는 연차일/스케줄 판정 하위호환 유지.
        Map<String, List<LeaveUseResult>> leaveListByYmd = expandLeaveMulti(weekLeaves, startYmd, endYmd);
        // 초과근무(등록 or 신청) 보유 일자 집합 — 스케줄 수정 요청 게이팅(보유일은 스케줄 변경 차단).
        Set<String> overtimeYmds = new HashSet<>(nullSafe(appAttd01Mapper.selectOvertimeYmds(q)));
        // prafta-app-030 후속: 주 범위 1회 호출 후 workYmd 별 그룹(표시 전용·N+1 회피).
        Map<String, List<RangeOvertimeResult>> overtimeByYmd =
                groupOvertimeByYmd(nullSafe(appAttd01Mapper.selectAppliedOvertimesByRange(q)));

        // (재작업: qa High) isMonthClosed 시그니처 변경 — nodeCd 는 여기서 1회만 조회해 재사용(같은 siteCd).
        String weekNodeCd = resolveNodeCdForClose(param.cmpnyCd(), param.siteCd(), param.userCd());
        boolean closed = isMonthClosed(param.cmpnyCd(), param.siteCd(), weekNodeCd, startYmd.substring(0, 6));
        // 주가 월 경계를 넘으면 종료일 월의 마감도 확인.
        boolean closedEnd = endYmd.substring(0, 6).equals(startYmd.substring(0, 6))
                ? closed
                : isMonthClosed(param.cmpnyCd(), param.siteCd(), weekNodeCd, endYmd.substring(0, 6));

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
            HolidayResult holiday = holidayByYmd.get(ymd);
            LeaveUseResult leave = leaveByYmd.get(ymd);
            // prafta-com-008-E-2: 연차일 판정을 leave_use(종일 확정) 기준으로 전환.
            boolean isLeaveDay = isFullDayLeave(leave);
            // 작업지시서_연차변경화면_진입버튼: 연차 이동 가능 여부(부분연차 포함, isLeaveDay 무관).
            boolean leaveMovable = leave != null
                    && StringUtils.hasText(leave.leaveId())
                    && leave.startDate().compareTo(todayYmd) >= 0;
            boolean isTwoSlot = sched != null && StringUtils.hasText(sched.secSchStrTime());
            // 근무 스케줄 존재 = SCH_CD 매칭까지 요구(selectScheduleByRange 는 WP 행 기준 LEFT JOIN 이라
            //   WORK_PLAN_CD NULL(휴무)/미매치 행도 non-null 로 온다 — 스케줄 없는 날 오판 방지).
            boolean hasSchCd = sched != null && sched.schCd() != null && !isLeaveDay;

            List<AttdRecordResult> dayAttds = attdByYmd.getOrDefault(ymd, Collections.emptyList());
            // 작업지시서_소속이동-이력가시성-보정(QA High 보정): mergeAttdBySeq 로 결정론화(day 상세와 동일 규칙).
            Map<Integer, AttdRecordResult> attdBySeq = mergeAttdBySeq(dayAttds, param.siteCd());

            // prafta-app-014: 주간 요약/합계/상태도 effective 기준으로 일관화(1구간 2회 출근 반영).
            int slotCount = isLeaveDay ? 0 : effectiveSlotCount(isTwoSlot, hasSchCd, attdBySeq);

            // 합계: 예정=스케줄 합, 실=완료된 근무(출퇴근 모두 등록)만 합(slot2 기록 있으면 합산).
            if (hasSchCd) {
                plannedSum += plannedMinutes(sched, isTwoSlot);
                // PRAFTA-FIXEDOT-2(표기): 주간 예정 합계에 고정연장 분 편입(없는 타입은 0 — 무회귀).
                plannedSum += fixedOtPlannedMinutes(sched);
            }
            actualSum += actualCompletedMinutes(sched, isTwoSlot, attdBySeq);

            String scheduleSummary = hasSchCd ? scheduleSummary(sched, isTwoSlot) : null;
            // PRAFTA-FIXEDOT-2(표기): 고정연장 요약(소정과 구분 표기 — FE 가 "고정연장" 라벨 부착).
            String fixedOtSummary = hasSchCd ? fixedOtSummary(sched) : null;
            String attendanceSummary = hasSchCd ? attendanceSummary(attdBySeq, slotCount) : null;
            String attendanceStatus = hasSchCd
                    // D-3: 그날 반차 전건(하루 2건 가능)을 넘겨 합집합으로 판정한다.
                    ? computeAttendanceStatus(sched, slotCount, attdBySeq, isToday, isPast,
                            leaveListByYmd.get(ymd))
                    : null;

            WeekDayActionsResponse actions = computeWeekActions(
                    hasSchCd, overtimeYmds.contains(ymd), isLeaveDay, slotCount, attdBySeq, isToday, isPast, dayClosed);

            // prafta-app-030 후속: 그날 적용(승인) 초과근무 합계/목록(표시 전용). 없으면 0/빈 리스트.
            List<RangeOvertimeResult> dayOvertimes = overtimeByYmd.get(ymd);

            // 작업지시서_소속이동-이력가시성-보정 T3 데이터 소스: buildDayResponse 와 동일 규칙(실 근태 우선/스케줄 폴백).
            AttdRecordResult primaryAttdForSite = attdBySeq.get(1) != null ? attdBySeq.get(1) : attdBySeq.get(2);
            String recordSiteCd = null;
            String recordSiteName = null;
            if (primaryAttdForSite != null && StringUtils.hasText(primaryAttdForSite.siteCd())) {
                recordSiteCd = primaryAttdForSite.siteCd();
                recordSiteName = primaryAttdForSite.siteNm();
            } else if (sched != null && StringUtils.hasText(sched.siteCd())) {
                recordSiteCd = sched.siteCd();
                recordSiteName = sched.siteNm();
            }

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
                    // 작업지시서_연차변경화면_진입버튼: 이동 발의 식별자 + 이동 가능 여부.
                    .leaveId(leave == null ? null : leave.leaveId())
                    .leaveMovable(leaveMovable)
                    // PRAFTA_COM_002-B-1: 단건 스칼라(첫 1건) 요청중 여부(다건은 leaves[].pendingApproval).
                    .leavePending(isLeavePending(leave))
                    // 같은 날 부분연차 다건 표시용 마커 목록(시각순). 없으면 빈 리스트.
                    .leaves(toLeaveMarkers(leaveListByYmd.get(ymd)))
                    .workPlanCode(sched == null ? null : sched.workPlanCd())
                    .workPlanName(resolveWorkPlanName(sched, isLeaveDay, leave == null ? null : leave.leaveNm()))
                    .isTwoSlot(isTwoSlot)
                    .scheduleSummary(scheduleSummary)
                    .fixedOtSummary(fixedOtSummary)
                    .attendanceSummary(attendanceSummary)
                    .attendanceStatus(attendanceStatus)
                    .actions(actions)
                    // prafta-app-030 후속: 그날 적용(승인) 초과근무 합계 분 + 항목 목록.
                    .overtimeMinutes(sumOvertimeMinutes(dayOvertimes))
                    .overtimes(toOvertimeItems(dayOvertimes))
                    // 작업지시서_소속이동-이력가시성-보정 T3: "당시 소속" 배지 데이터 소스.
                    .recordSiteCd(recordSiteCd)
                    .recordSiteName(recordSiteName)
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
            boolean isToday, boolean isPast, List<LeaveUseResult> leaves) {

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
        // HB-05(D1): 그날 확정 반차가 있으면 판정 기준을 "반차 반영 유효 소정 구간"으로 치환한다
        //   (정책 §10.1.1). 시각 없는 구 반차·시간차·무연차일은 원값이 그대로 돌아와 판정 불변.
        String fstStr = effectiveStart(sched.fstSchStrTime(), sched.fstSchEndTime(), leaves);
        String fstEnd = effectiveEnd(sched.fstSchStrTime(), sched.fstSchEndTime(), leaves);
        String secEnd = effectiveEnd(sched.secSchStrTime(), sched.secSchEndTime(), leaves);

        // 지각 = 슬롯1 실제 출근일시(출근일자+시각) > 스케줄 시작일시(슬롯1, 항상 스케줄 대응).
        //   ★ qa N-2(2026-08-07): 판정용 시각의 일자 프레임은 "원 스케줄"로 잡는다(Attd_08/Attd_11 동일).
        boolean late = isLateStamp(s1, sched.workYmd(),
                sched.fstSchStrTime(), sched.fstSchEndTime(), fstStr);
        // 조퇴 기준 종료시각/대상 슬롯: 스케줄 2구간이면 2구간 종료·slot2 퇴근, 그 외(1구간 스케줄)는
        //   스케줄 대응 마지막 슬롯=slot1 기준(추가 근무 slot2 는 스케줄 미대응 → 조퇴 판정 제외).
        AttdRecordResult lastScheduledSlot = isTwoSlot && attdBySeq.get(2) != null ? attdBySeq.get(2) : s1;
        String rawEndFrameStr = isTwoSlot ? sched.secSchStrTime() : sched.fstSchStrTime();
        String rawEndFrameEnd = isTwoSlot ? sched.secSchEndTime() : sched.fstSchEndTime();
        String schEnd = isTwoSlot ? secEnd : fstEnd;
        // 조퇴 = 실제 퇴근일시 < 스케줄 종료일시. 야간이면 원 스케줄 프레임으로 익일 여부를 판정한다.
        boolean early = isEarlyStamp(lastScheduledSlot, sched.workYmd(),
                rawEndFrameStr, rawEndFrameEnd, schEnd);
        if (late) return ATTD_LATE;
        if (early) return ATTD_EARLY_LEAVE;
        return ATTD_NORMAL;
    }

    /**
     * HB-05(D1): 반차를 반영한 판정용 구간 시작(HHmm). 구간 전체가 면제되면 null(판정 제외).
     * 반차가 없거나 시각이 없으면 원값 그대로(회귀 0).
     */
    private String effectiveStart(String schStr, String schEnd, List<LeaveUseResult> leaves) {
        PartialLeaveWindowUtils.EffectiveWorkWindow eff = resolveHalfWindow(schStr, schEnd, leaves);
        return (eff == null) ? schStr : (eff.fullyExempt() ? null : eff.planStart());
    }

    /** HB-05(D1): 반차를 반영한 판정용 구간 종료(HHmm). 구간 전체가 면제되면 null(판정 제외). */
    private String effectiveEnd(String schStr, String schEnd, List<LeaveUseResult> leaves) {
        PartialLeaveWindowUtils.EffectiveWorkWindow eff = resolveHalfWindow(schStr, schEnd, leaves);
        return (eff == null) ? schEnd : (eff.fullyExempt() ? null : eff.planEnd());
    }

    /**
     * HB-05(D1): 그날 확정 반차('01', 시각 보유) <b>전건</b> 조회 — 지각·조퇴 PUSH 판정 입력.
     *
     * <p>기존 범위 조회({@code selectLeaveUseByRange}, CONFIRMED·DEL_YN='N')를 단일일로 재사용한다
     * (신규 쿼리 신설 회피).
     * <p>★ D-3(2026-08-07): 반차는 하루 2건(시작기준 0.5 + 종료기준 0.5 = 1.0)이 성립하므로
     * 단건만 보면 종일 쉰 사람에게 지각·조퇴 PUSH 가 나간다 → 전건을 모아 합집합으로 판정한다.
     * 비면 빈 목록 → 호출부가 원 스케줄 시각을 그대로 쓴다(회귀 0).
     */
    private List<LeaveUseResult> selectHalfDayLeavesOn(String cmpnyCd, String siteCd, String userCd, String workYmd) {
        List<LeaveUseResult> out = new ArrayList<>();
        try {
            List<LeaveUseResult> leaves = nullSafe(appAttd01Mapper.selectLeaveUseByRange(
                    new AttdRangeQuery(cmpnyCd, siteCd, userCd, workYmd, workYmd)));
            for (LeaveUseResult lv : leaves) {
                // ★2026-08-17: 반차 한정 → 시각 보유 부분연차(반차+시간차) 전체로 확장(판정 면제 입력).
                if (JUDGE_EXEMPT_LEAVE_UNITS.contains(lv.useUnitType())
                        && StringUtils.hasText(lv.startTime())
                        && StringUtils.hasText(lv.endTime())) {
                    out.add(lv);
                }
            }
        } catch (Exception e) {
            // 판정 보조 조회 실패가 출퇴근 본 흐름을 막지 않도록 흡수(원 스케줄 기준 판정으로 폴백).
            log.error("[attd01] 반차 조회 실패(원 스케줄 기준 판정으로 폴백) userCd={}, workYmd={}", userCd, workYmd, e);
        }
        return out;
    }

    /**
     * 시각 보유 부분연차(반차 '01' + 시간차 '02'/'03'/'04')를 골라 유효 소정 구간을 산출한다.
     * 대상이 없으면 null → 호출부가 원값을 유지한다.
     * ★2026-08-17 사용자 확정: 시간차도 판정 면제에 포함(HB-05 §2 비목표 해제 — 웹 Attd_08/11 동일).
     * 하루 2건(시작기준 + 종료기준)이면 순차 적용되어 구간 전체 면제로 수렴한다(D-3).
     */
    private PartialLeaveWindowUtils.EffectiveWorkWindow resolveHalfWindow(
            String schStr, String schEnd, List<LeaveUseResult> leaves) {
        if (leaves == null || leaves.isEmpty()
                || !StringUtils.hasText(schStr) || !StringUtils.hasText(schEnd)) {
            return null;
        }
        List<PartialLeaveWindowUtils.LeaveWindow> windows = new ArrayList<>(leaves.size());
        for (LeaveUseResult lv : leaves) {
            // ★2026-08-17: 반차 한정 → 시각 보유 부분연차(반차+시간차) 전체로 확장(판정 면제 입력).
            if (lv != null
                    && JUDGE_EXEMPT_LEAVE_UNITS.contains(lv.useUnitType())
                    && StringUtils.hasText(lv.startTime())
                    && StringUtils.hasText(lv.endTime())) {
                windows.add(new PartialLeaveWindowUtils.LeaveWindow(lv.startTime(), lv.endTime()));
            }
        }
        if (windows.isEmpty()) {
            return null;
        }
        return PartialLeaveWindowUtils.resolveAll(schStr, schEnd, windows);
    }

    /**
     * 이번주 바텀시트 4액션 활성도 산출 (계약 §3.2, 매트릭스 UI-A006).
     *
     * <p>prafta-app-013: 산출 로직을 {@link #computeActionFlags} 공용 메서드로 위임한다.
     *   오늘/이번주/이번달 3탭이 동일 규칙(결정 §2)을 쓰도록 통일. 시그니처는 유지.
     */
    private WeekDayActionsResponse computeWeekActions(
            boolean hasSchCd, boolean hasOvertime, boolean isLeaveDay, int slotCount,
            Map<Integer, AttdRecordResult> attdBySeq, boolean isToday, boolean isPast, boolean closed) {

        // prafta-app-031: 액션 게이팅의 "근무중"은 진행 중(퇴근 안 한) 슬롯 존재 기준.
        //   기존 allSlotsCompleted(스케줄 전 슬롯 충족)은 2구간 중 1구간만 정상 근무한 과거일까지
        //   "근무중"으로 오게이팅했다 → hasOpenSlot 으로 전환(미사용 슬롯은 근무중 아님).
        boolean isWorking = hasOpenSlot(attdBySeq);
        // hasAttendance = 출근 기록 1건 이상 존재(퇴근/완료 무관, 결정 Q2=a).
        boolean hasAttendance = attdBySeq.get(1) != null || attdBySeq.get(2) != null;

        return computeActionFlags(hasSchCd, hasOvertime, isPast, isToday, hasAttendance, isWorking, closed);
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
     *     <li>isWorking: 기록된 슬롯 중 퇴근하지 않은(진행 중) 슬롯이 존재(= {@link #hasOpenSlot}).
     *         prafta-app-031: 스케줄 슬롯 "미사용(기록 없음)"은 근무중이 아니다 — 2구간 중 1구간만
     *         정상 근무(출근+퇴근)한 날은 완료된 구간 기준으로 액션을 허용한다(기존 allSlotsCompleted
     *         기반 판정은 미사용 슬롯 때문에 과거일까지 "근무중"으로 오게이팅했다).</li>
     *     <li>closed: 해당 월 근태마감(prafta-028).</li>
     *   </ul>
     *
     * <p>enabled 조건:
     *   <ul>
     *     <li>canRequestScheduleModify = !closed && !hasOvertime (과거·현재·미래·스케줄 유무 무관).
     *         초과근무(등록 or 신청)는 스케줄 기준 정산이므로 보유일의 스케줄 수정은 막는다.</li>
     *     <li>canRequestAttendanceCorrection = !isFuture && !closed && !isWorking.</li>
     *     <li>canRequestOvertime = !isFuture && !closed && hasAttendance && !isWorking.</li>
     *     <li>canRequestLeave = !closed && !hasAttendance (스케줄 무관, 출근기록 있으면 불가).</li>
     *     <li>leaveFullDayOnly = !hasSchedule (연차 폼이 full-day 강제에 소비, Follow-up F1).</li>
     *   </ul>
     * 공통: closed 면 4액션 전부 비활성. 미래 차단은 보정·초과근무에만.
     */
    private WeekDayActionsResponse computeActionFlags(
            boolean hasSchedule, boolean hasOvertime, boolean isPast, boolean isToday,
            boolean hasAttendance, boolean isWorking, boolean closed) {

        boolean isFuture = !isToday && !isPast;

        // 스케줄 수정 요청: 마감 전 + 초과근무 미보유면 항상 허용(과거·현재·미래·스케줄 유무 무관).
        boolean canScheduleModify = !closed && !hasOvertime;
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

        // 작업지시서_소속이동-이력가시성-보정(QA High 보정): 세션 SITE_CD 를 넘겨 동일 WORK_YMD 다건
        //   공존 시 결정론적으로 병합한다(indexSchedule 참조).
        Map<String, ScheduleResult> schByYmd =
                indexSchedule(nullSafe(appAttd01Mapper.selectScheduleByRange(q)), param.siteCd());
        Map<String, List<AttdRecordResult>> attdByYmd = indexAttd(nullSafe(appAttd01Mapper.selectAttdByRange(q)));
        Map<String, HolidayResult> holidayByYmd = indexHoliday(nullSafe(appAttd01Mapper.selectHolidaysByRange(q)));
        Map<String, LeaveUseResult> leaveByYmd = expandLeave(nullSafe(appAttd01Mapper.selectLeaveUseByRange(q)), fromYmd, toYmd);

        // GPS 미확인 판정을 위해 월 전체 근태 ATTD_ID 의 GPS 일괄 로딩.
        Map<String, GpsResult> gpsByCheckIn = new HashMap<>();
        Map<String, GpsResult> gpsByCheckOut = new HashMap<>();
        List<AttdRecordResult> allMonthAttds = new ArrayList<>();
        attdByYmd.values().forEach(allMonthAttds::addAll);
        loadGps(param.cmpnyCd(), allMonthAttds, gpsByCheckIn, gpsByCheckOut);

        boolean closed = isMonthClosed(param.cmpnyCd(), param.siteCd(),
                resolveNodeCdForClose(param.cmpnyCd(), param.siteCd(), param.userCd()), ym);
        // prafta-app-030 후속: 월 범위 1회 호출 후 workYmd 별 분 합계(표시 전용·N+1 회피).
        Map<String, List<RangeOvertimeResult>> overtimeByYmd =
                groupOvertimeByYmd(nullSafe(appAttd01Mapper.selectAppliedOvertimesByRange(q)));

        List<MonthDayResponse> days = new ArrayList<>();
        int plannedSum = 0;
        int actualSum = 0;

        for (int day = 1; day <= last.getDayOfMonth(); day++) {
            LocalDate d = first.withDayOfMonth(day);
            String ymd = d.format(YMD);
            boolean isPast = ymd.compareTo(todayYmd) < 0;

            ScheduleResult sched = schByYmd.get(ymd);
            HolidayResult holiday = holidayByYmd.get(ymd);
            LeaveUseResult leave = leaveByYmd.get(ymd);
            // prafta-com-008-E-2: 슬롯/근무 산출용 연차일 = leave_use 종일 확정. (달력 점 dayType=LEAVE 는 부분연차도 포함 — 표시 유지)
            boolean isLeaveDay = isFullDayLeave(leave);
            boolean isTwoSlot = sched != null && StringUtils.hasText(sched.secSchStrTime());
            // 근무 스케줄 존재 = SCH_CD 매칭까지 요구(주간과 동일 — WORK_PLAN_CD NULL(휴무)/미매치 행이
            //   dayType=WORK(초록 셀)로 오판되던 문제 수정).
            boolean hasSchCd = sched != null && sched.schCd() != null && !isLeaveDay;

            List<AttdRecordResult> dayAttds = attdByYmd.getOrDefault(ymd, Collections.emptyList());
            // 작업지시서_소속이동-이력가시성-보정(QA High 보정): mergeAttdBySeq 로 결정론화(day/week 와 동일 규칙).
            Map<Integer, AttdRecordResult> attdBySeq = mergeAttdBySeq(dayAttds, param.siteCd());

            // prafta-app-014: 월간도 effective 기준(처리필요/합계에 1구간 2회 출근 반영).
            int slotCount = isLeaveDay ? 0 : effectiveSlotCount(isTwoSlot, hasSchCd, attdBySeq);

            if (hasSchCd) {
                plannedSum += plannedMinutes(sched, isTwoSlot);
                // PRAFTA-FIXEDOT-2(표기): 월간 예정 합계에도 고정연장 분 편입(주간과 동일 규칙).
                plannedSum += fixedOtPlannedMinutes(sched);
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
                    // prafta-app-030 후속: 그날 적용(승인) 초과근무 합계 분(없으면 0).
                    .overtimeMinutes(sumOvertimeMinutes(overtimeByYmd.get(ymd)))
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
     *     <li>(작업지시서_소속이동중_출퇴근-사업장필터-근본수정) 세션 siteCd 와 출근 레코드 SITE_CD 동일성
     *         검증은 제거했다 — 근무는 "시작된 사업장"(open.siteCd())에 귀속되며 이후 로직도 전부
     *         open.siteCd() 를 쓴다. 스코프 가드는 updateCheckOut/updateReCheckOut 의
     *         ATTD_ID+CMPNY_CD+USER_CD+DEL_YN='N' WHERE 절로 유지된다.</li>
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

        // ★위치정보 동의 게이트(S4) — 퇴근은 <b>진행 중인 근태의 후속 동작</b>이라 ForOngoing 을 쓴다.
        //   약관 시행 시점이 자정이라(STR_DATE 가 날짜) 오버나이트 근무자는 개정 때마다 퇴근에서 걸린다.
        //   재동의 대기(회사 사정)면 퇴근까지는 허용하고, 본인 의사인 중지·철회는 차단한다.
        //   ★차단 시 퇴근이 열린 채로 남으므로 앱이 즉시 재동의로 유도해야 한다(관리자 보정 방지).
        if (!locationConsentService.isCollectAllowedForOngoing(cmpnyCd, userCd)) {
            log.info("[attd01] 셀프 퇴근 거부: 위치정보 미동의 (userCd={})", userCd);
            throw new ApiException(LocationErrorCode.LOCATION_403_001);
        }

        // 1) prafta-app-026: 재퇴근 대상 = D+1 윈도우 내 최신 출근행(CHECK_OUT_TIME 유무 무관) tail 슬롯 1건.
        //    workYmd 우선(Low-1), 미전달이면 D+1 윈도우 하한 최신 폴백. 이미 퇴근한 슬롯도 대상에 포함되어
        //    "마지막 퇴근시각으로 덮어쓰기(last-write-wins)"가 가능하다(오탭 보정).
        OpenAttdResult open = appAttd01Mapper.selectReCheckOutTarget(cmpnyCd, siteCd, userCd, yesterday, param.workYmd());
        if (open == null) {
            // 퇴근 대상 없음(출근 안 했거나 D+1 초과/workYmd 불일치로 후보 제외).
            log.info("[attd01] 셀프 퇴근 거부: 퇴근 대상 없음 (userCd={})", userCd);
            throw new ApiException(AttdErrorCode.ATTD_404_010);
        }
        // prafta-app-026: 최초/재퇴근 분기 키. checkOutTime 이 비어 있으면 최초 퇴근, 있으면 재퇴근.
        boolean wasCheckedOut = StringUtils.hasText(open.checkOutTime());

        // 2) D+1 윈도우 재검증 (today 또는 today-1 만 허용).
        if (!today.equals(open.workYmd()) && !yesterday.equals(open.workYmd())) {
            log.info("[attd01] 셀프 퇴근 거부: D+1 윈도우 초과 (userCd={}, workYmd={})", userCd, open.workYmd());
            throw new ApiException(AttdErrorCode.ATTD_404_010);
        }

        // 3) (작업지시서_소속이동중_출퇴근-사업장필터-근본수정) 세션 siteCd 와 레코드 open.siteCd() 비교 가드를
        //    제거했다. 근무일 도중 소속이동이 발생해도(오버나이트+자정배치) 그 근무는 "시작된 사업장"
        //    (open.siteCd())에 통째로 귀속되며, 이후 로직(3-1/5/5-2/7 등)은 애초에 전부 open.siteCd()를
        //    쓰고 세션 siteCd 는 쓰지 않는다 — 이 가드만 이질적으로 세션 값을 기준 삼아 정당한 퇴근을 막고 있었다.
        //    스코프 가드는 updateCheckOut/updateReCheckOut 의 WHERE ATTD_ID+CMPNY_CD+USER_CD+DEL_YN='N' 로 유지된다.

        // 3-1) prafta-com-008-B: 노무수령거부 차단 가드(CHECK_OUT, 보수적). 퇴근 대상 근무일 기준.
        //   설계 §5-4: 정상 경로에선 체크인이 막혀 그날 열린 근태가 없어 본 가드에 도달하지 않는다.
        //   "촉진 지정이 체크인 이후 이루어진" 등 이상상태로 열린 근태가 남은 경우의 방어 가드다.
        //   대상이면 guard 내부에서 ATTD_400_150 차단 throw(BLOCKED 이력+PUSH 선커밋), 비대상이면 정상 진행.
        leaveRefusalDetectService.guardAndRecord(
                cmpnyCd, open.siteCd(), userCd, open.nodeCd(), open.workYmd(),
                LeaveRefusalConst.ATTEMPT_CHECK_OUT, userCd);

        // 4) 월마감 검증 (prafta-028 부서단위 마감이면 쓰기 차단).
        //    (재작업: qa High) 세션 siteCd 가 아니라 open.siteCd()(근태 행 실귀속 사업장) 기준으로 판정한다.
        //    TB_ATTD_CLOSE 는 사업장별 마감상태가 독립적이라, 세션 siteCd 로 판정하면 근태 행이 속한
        //    사업장이 이미 마감됐어도 세션의 다른(마감 전) 사업장 기준으로 마감 잠금을 우회할 수 있었다.
        //    (재작업 2차: qa High) nodeCd 도 세션 siteCd 로 TB_USER 를 재조회하지 않고, open.nodeCd()
        //    (이 근태 행에 스냅샷된 실귀속 부서)를 그대로 쓴다 — 이동한 사용자는 TB_USER 의 "현재" SITE_CD 가
        //    open.siteCd() 와 달라 재조회가 0건이 되어 nodeCd 가 '*' 로 폴백, 부서단위 마감을 놓쳤다.
        if (isMonthClosed(cmpnyCd, open.siteCd(), open.nodeCd(), open.workYmd().substring(0, 6))) {
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

        // 5-2) 근무 구간 시각 겹침 금지(정책서 attd §7.6). 대상 슬롯 open 확정 이후·UPDATE 이전에서
        //      [이 슬롯 출근 stamp, 신규 퇴근 stamp] 가 다른 구간(이 attdId 제외)과 겹치면 차단한다.
        //      재퇴근도 동일 검사(덮어쓸 퇴근시각 기준). OpenAttdResult 에 출근 필드가 없으므로
        //      selectAttdByRange 로 이 attdId 의 출근값을 확보한다. 인접 경계는 허용(findConflict 규약).
        //      당일 open 은 SENTINEL(같은 날 이중 출근 방지), 이웃날 open 은 그 근무일의 다음날 00:00 에서 종료.
        {
            // 겹침가드 개선(2026-08-06, §0-3 D-3): 신규 퇴근 시각이 근무일 다음날로 넘어가는 경우에 한해
            //   조회 범위를 D ~ D+1 로 확장한다. 앱은 자기 근무일 하루만 읽어 왔기 때문에, 오버나이트 퇴근을
            //   채울 때 다음날 근태와의 실제 겹침을 검출하지 못하는 공백이 있었다(웹 2경로는 D-1~D+1 조회).
            //   같은 날 퇴근이면 종전대로 당일만 조회한다(불필요한 조회 증가 방지).
            //   정상 흐름("전날 퇴근 먼저 → 오늘 재출근")은 퇴근 시점에 D+1 근태가 없어 영향받지 않는다.
            boolean crossesToNextDay = !open.workYmd().equals(today);
            String overlapToYmd = crossesToNextDay
                    ? DateTimeUtils.plusDays(open.workYmd(), 1)
                    : open.workYmd();
            if (overlapToYmd == null) overlapToYmd = open.workYmd(); // 형식 오류 fail-safe(종전 동작)
            AttdRangeQuery overlapQ = new AttdRangeQuery(cmpnyCd, open.siteCd(), userCd, open.workYmd(), overlapToYmd);
            List<AttdRecordResult> dayRecords = nullSafe(appAttd01Mapper.selectAttdByRange(overlapQ));
            // 이 attdId 의 출근값 확보.
            AttdRecordResult self = null;
            for (AttdRecordResult a : dayRecords) {
                if (open.attdId().equals(a.attdId())) { self = a; break; }
            }
            // 판정 기준선 = 이 슬롯의 근무일(open.workYmd()). 기존 구간은 자기 WORK_YMD 로 dayOffset 이 산출된다.
            AttdOverlapUtils.Segment newSeg = (self == null) ? null
                    : AttdOverlapUtils.buildSegment(open.workYmd(), self.workYmd(),
                            self.attdId(), String.valueOf(self.workSeq()),
                            self.checkInDate(), self.checkInTime(), today, checkOutTime, true);
            // 출근 stamp 미확정(또는 대상 행 미조회)이면 검사 제외(newSeg==null).
            if (newSeg != null) {
                List<AttdOverlapUtils.Segment> segs = new ArrayList<>();
                segs.add(newSeg);
                for (AttdRecordResult a : dayRecords) {
                    if (open.attdId().equals(a.attdId())) continue; // 이 슬롯 제외
                    AttdOverlapUtils.Segment exSeg = AttdOverlapUtils.buildSegment(
                            open.workYmd(), a.workYmd(), a.attdId(), String.valueOf(a.workSeq()),
                            a.checkInDate(), a.checkInTime(), a.checkOutDate(), a.checkOutTime(), false);
                    if (exSeg == null) continue;
                    if (!exSeg.judgeable()) {
                        // 손상 행(퇴근 ≤ 출근 등)은 판정 제외(§7.6 D-1). 운영 탐지용 경고만 남긴다.
                        log.warn("[겹침가드] 퇴근시각 미성립 근태 행 판정 제외. workYmd={}, workSeq={}, attdId={}, kind={}",
                                exSeg.workYmd(), exSeg.workSeq(), exSeg.attdId(), exSeg.kind());
                    }
                    segs.add(exSeg);
                }
                AttdOverlapUtils.Conflict conflict = AttdOverlapUtils.findConflict(segs);
                if (conflict != null) {
                    log.info("[attd01] 셀프 퇴근 거부: 근무 구간 시각 겹침(§7.6) (userCd={}, workYmd={}, attdId={}, newOut={}, otherWorkYmd={}, otherKind={})",
                            userCd, open.workYmd(), open.attdId(), checkOutTime,
                            conflict.other().workYmd(), conflict.other().kind());
                    throw new ApiException(AttdErrorCode.ATTD_400_113,
                            AttdOverlapMessages.overlapMessage(open.workYmd(), conflict.other()));
                }
            }
        }

        // 6) prafta-app-026: 퇴근 UPDATE 분기.
        //    - 최초 퇴근(wasCheckedOut==false): 기존 updateCheckOut(WHERE CHECK_OUT_TIME IS NULL) 유지.
        //      동시성 가드로 0건이면 이미 퇴근됨(ATTD_409_001).
        //    - 재퇴근(wasCheckedOut==true): updateReCheckOut(NULL 가드 없음)로 last-write-wins 덮어쓰기.
        CheckOutCommand updateCmd = new CheckOutCommand(
                open.attdId(), cmpnyCd, userCd, today, checkOutTime, CHECK_OUT_METHOD_USER, param.deviceUuid());
        if (!wasCheckedOut) {
            int updated = appAttd01Mapper.updateCheckOut(updateCmd);
            if (updated == 0) {
                log.info("[attd01] 셀프 퇴근 거부: 동시성(이미 퇴근됨) (userCd={}, attdId={})", userCd, open.attdId());
                throw new ApiException(AttdErrorCode.ATTD_409_001);
            }
        } else {
            // 재퇴근도 영향행수를 검사한다(최초 퇴근과 대칭). 조회~UPDATE 사이에 대상 행이
            // soft-delete(DEL_YN='Y') 되는 등으로 0건이면, GPS/응답만 갱신되는 무음 불일치를 막고 거부한다.
            int reUpdated = appAttd01Mapper.updateReCheckOut(updateCmd);
            if (reUpdated == 0) {
                log.info("[attd01] 재퇴근 거부: 대상 근태 없음(레이스/삭제) (userCd={}, attdId={})", userCd, open.attdId());
                throw new ApiException(AttdErrorCode.ATTD_404_010);
            }
            log.info("[attd01] 재퇴근(퇴근시각 덮어쓰기) (userCd={}, attdId={}, prevTime={}, newTime={})",
                    userCd, open.attdId(), open.checkOutTime(), checkOutTime);
        }

        // 7) prafta-app-026: GPS 매 퇴근 재판정 + delete-then-insert 덮어쓰기.
        //    해당 ATTD_ID 의 퇴근 GPS 행(02)을 먼저 hard DELETE 한 뒤, 외근(지오펜스 밖)일 때만 최신 값 INSERT.
        //    온사이트면 INSERT 안 함 → "외근 판정 = 퇴근 GPS행 존재" 불변식으로 외근↔온사이트 전환을 정확히 반영.
        appAttd01Mapper.deleteCheckOutGps(cmpnyCd, open.attdId());
        //    ★위치정보 동의철회·중지 S3: 퇴근은 <b>진행 중인 근태의 후속 동작</b>이라
        //      isCollectAllowedForOngoing 을 쓴다(② 오버나이트 예외). 약관 시행 시점이 자정이라
        //      오버나이트 근무자는 개정 때마다 퇴근 시점에 재동의 대기 상태가 되는데, 그때 퇴근을
        //      막으면 근태가 열린 채로 남아 관리자 보정으로 넘어간다.
        //      ★본인 의사인 중지/철회에는 이 예외가 적용되지 않는다(수집 중단).
        if (isOffsite && locationConsentService.isCollectAllowedForOngoing(cmpnyCd, userCd)) {
            String gpsId = appAttd01Mapper.selectGpsId(cmpnyCd);
            // GPS좌표-암호화-전환-02: 좌표는 암호문만 저장(평문 LAT/LON 기록 중단). 좌표/암호문 로그 금지.
            CheckOutGpsCommand gpsCmd = new CheckOutGpsCommand(
                    gpsId, cmpnyCd, open.attdId(), open.siteCd(), userCd, GPS_TYPE_CHECK_OUT,
                    gpsCoordCrypto.encrypt(param.lat()), gpsCoordCrypto.encrypt(param.lon()), param.accuracy(),
                    today, apiCallTime, param.isMocked(), param.ipAddr(), param.offsiteReason(), userCd);
            appAttd01Mapper.insertCheckOutGps(gpsCmd);
        }

        // prafta-app-022-6 / prafta-app-026: TBM 자동 중도퇴실은 "최초 퇴근"에서만 수행(부작용 멱등화).
        //   재퇴근은 오탭 보정 용도라 진행중 TBM 을 다시 건드리지 않는다(이미 최초 퇴근에서 중도퇴실됨).
        //   attd01 → tbm01 단방향(순환 금지). best-effort: withdraw 실패가 퇴근 커밋을 막지 않도록 예외 흡수.
        if (!wasCheckedOut) {
            try {
                int withdrawn = appTbm01Service.withdrawAllInProgress(param.tokenInfo());
                if (withdrawn > 0) {
                    log.info("[attd01] 퇴근 연동 TBM 자동 중도퇴실 처리 (userCd={}, count={})", userCd, withdrawn);
                }
            } catch (Exception e) {
                // 퇴근은 이미 커밋 대상. TBM 중도퇴실 실패는 퇴근을 막지 않는다(원인 로깅 후 계속).
                log.error("[attd01] 퇴근 연동 TBM 자동 중도퇴실 실패 (userCd={}) — 퇴근은 계속 진행", userCd, e);
            }
        }

        // PRAFTA-APP-021-3c(M1): 조퇴 감지(raw 실근태) + 노드 관리자 PUSH(예외 격리, 퇴근 영향 없음).
        //   퇴근 대상 근무일의 스케줄을 조회해 퇴근 구간(open.workSeq) 종료시각과 비교한다.
        //   스케줄 없으면 판정 기준이 없어 생략. 2구간 스케줄은 구간(workSeq)별 시작/종료를 짝지어 자정 보정한다.
        //   prafta-app-026: 최초/재퇴근 모두 호출한다. AttdLateEarlyNotiServiceImpl 이 dedupKey
        //   (ATTD_EARLY_{attdId}_{admin})로 DuplicateKeyException 을 흡수하므로 같은 근태 1건당 PUSH 1회 멱등.
        //   따라서 "최초 정시 → 재퇴근 조퇴" 전환 시 최초에 미발사된 조퇴 PUSH 가 재퇴근에서 올바르게 발사되고,
        //   이미 발사된 건은 dedupe 로 중복 발사되지 않는다.
        try {
            AttdRangeQuery schQ = new AttdRangeQuery(cmpnyCd, open.siteCd(), userCd, open.workYmd(), open.workYmd());
            List<ScheduleResult> outSchedules = nullSafe(appAttd01Mapper.selectScheduleByRange(schQ));
            ScheduleResult outSched = outSchedules.isEmpty() ? null : outSchedules.get(0);
            if (outSched != null && outSched.schCd() != null) {
                boolean outTwoSlot = StringUtils.hasText(outSched.secSchStrTime());
                boolean useSec = outTwoSlot && open.workSeq() == 2;
                String rawStr = useSec ? outSched.secSchStrTime() : outSched.fstSchStrTime();
                String rawEnd = useSec ? outSched.secSchEndTime() : outSched.fstSchEndTime();
                // ★ HB-05(D1): 조퇴 PUSH 도 반차 반영 유효 소정 구간 기준(화면 판정과 동일 근거).
                //   종료기준 반차자가 경계 시각에 퇴근하면 조퇴가 아니며 PUSH 도 발송하지 않는다.
                List<LeaveUseResult> halfLeaves =
                        selectHalfDayLeavesOn(cmpnyCd, open.siteCd(), userCd, open.workYmd());
                String schEndHhmm = effectiveEnd(rawStr, rawEnd, halfLeaves);
                // ★ qa N-2: 원 스케줄(rawStr/rawEnd)을 일자 프레임으로 함께 넘긴다(화면 판정과 동일 근거).
                attdLateEarlyNotiService.detectEarly(cmpnyCd, open.siteCd(), userCd, open.nodeCd(),
                        open.workYmd(), open.attdId(), today, checkOutTime,
                        rawStr, rawEnd, schEndHhmm, userCd);
            }
        } catch (Exception e) {
            log.error("[attd01] 조퇴 감지 hook 실패(퇴근 영향 없음) (userCd={}, workYmd={})", userCd, open.workYmd(), e);
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
     *         ("전날 퇴근을 먼저 등록하세요"). 마감 예외 판정은 그 D-1 근태가 실귀속된 사업장
     *         (selectPastOpenAttd 의 siteCd) 기준이며, 세션의 현재 siteCd 를 쓰지 않는다
     *         (작업지시서_소속이동중_출퇴근-사업장필터-근본수정 재작업 — qa 권고).</li>
     *     <li>⑤ 스케줄 조회: 연차일(LEAVE)이면 출근 차단(§8.3 노무수령거부).</li>
     *     <li>⑥ 출근횟수/구간 제한(§5.1~§5.4) + 2구간 스케줄 구간 명시 선택(prafta-app-015, 구 §5.5 정정):
     *         하루 최대 2회(prafta-app-014). 2구간 스케줄은 사용자가 출근 구간(targetWorkSeq=1|2)을
     *         선택하며 선택 구간이 곧 WORK_SEQ 가 된다(순서 자유·1구간 누락 허용·각 구간 1회).
     *         구간 미선택→087, 선택 구간 중복→088. 자동추정(구 §5.5 Case A/B/C·084·085)은 폐기.
     *         1구간 스케줄 재출근은 직전 구간 미퇴근이면 차단(081, §5.2).
     *         스케줄 없는 날은 허용(전량 초과근무 대상, §7.5 — 출근 등록까지).</li>
     *     <li>⑦ WORK_SEQ: 2구간 스케줄=선택 구간 직접 채번. 그 외=그 일자 기존 근태 개수 + 1.</li>
     *     <li>⑧ 지오펜스: 밖이면 외근(isOffsite) + GPS행(GPS_INFO_TYPE='01') INSERT. 안/폴백=미저장.</li>
     *     <li>출근 시각 = 서버 NOW() raw(표준화 미적용). CHECK_IN_METHOD='01'(SYS031).</li>
     *   </ul>
     */
    @Override
    @Transactional
    public MyAttendanceDayResponse checkIn(CheckInParam param) {
        // ── 근태 E2E #E6-1(F6): 셀프 출근 처리 중 도메인 예외가 아닌 런타임 오류(예: 특정 데이터 조합의
        //    2회차 재출근)로 500(COMMON_500_000)이 노출되던 것을 사용자 안내형 4xx(ATTD_400_116)로 전환한다.
        //    - 도메인 가드(ApiException: 042/080/081/082/086/087/088/089/113/150 등)는 그대로 재전파(4xx 유지).
        //    - 그 외 비ApiException 런타임만 승격한다. 승격된 예외도 unchecked → @Transactional 이 롤백하므로
        //      슬롯/원장 부작용은 없다(관찰된 '슬롯 미생성 클린 롤백' 유지). 전체 스택은 error 로그로 보존(진단).
        try {
            return checkInInternal(param);
        } catch (ApiException e) {
            throw e; // 도메인 4xx 는 원문 그대로 전파(과잉 변환 금지 — 정상 가드 메시지 보존).
        } catch (RuntimeException e) {
            log.error("[attd01] 셀프 출근 처리 중 예기치 못한 오류 → ATTD_400_116 전환 (userCd={}, workYmd={})",
                    param.userCd(), (param.workYmd() != null ? param.workYmd() : "(today)"), e);
            throw new ApiException(AttdErrorCode.ATTD_400_116);
        }
    }

    /** 셀프 출근 실제 처리(F6 방어 가드가 감싸는 내부 구현). 정책/시퀀스는 {@link #checkIn} javadoc 참조. */
    private MyAttendanceDayResponse checkInInternal(CheckInParam param) {
        String cmpnyCd = param.cmpnyCd();
        String siteCd = param.siteCd();
        String userCd = param.userCd();

        // ★위치정보 동의 게이트(S4) — 동의(AGREED) 상태가 아니면 출근을 차단한다.
        //   전용 오류코드(LOCATION_403_001)를 던져 앱이 안내 팝업 → 재동의 화면으로 분기하게 한다.
        //   ★로그인은 막지 않는다(005 는 LOGIN_GATE_YN='N'). 앱에 들어와야 재동의를 할 수 있다.
        if (!locationConsentService.isCollectAllowed(cmpnyCd, userCd)) {
            log.info("[attd01] 셀프 출근 거부: 위치정보 미동의 (userCd={})", userCd);
            throw new ApiException(LocationErrorCode.LOCATION_403_001);
        }

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
        //    (재작업: qa High) isMonthClosed 시그니처 변경 — 이 시점은 신규 출근(아직 근태 행 없음)이라
        //    "지금 이 siteCd(체크인 대상=세션 현재 사업장) 기준 현재 소속"을 재조회해서 넘긴다.
        if (isMonthClosed(cmpnyCd, siteCd, resolveNodeCdForClose(cmpnyCd, siteCd, userCd), workYmd.substring(0, 6))) {
            log.info("[attd01] 셀프 출근 거부: 마감된 기간 (userCd={}, closeYm={})", userCd, workYmd.substring(0, 6));
            throw new ApiException(AttdErrorCode.ATTD_400_042);
        }

        // 4) 다음날 게이트(prafta-app-021 §7.6): 직전일(today-1) 열린 근태가 있으면 출근 차단.
        //    D+2 이상 과거 미퇴근은 차단하지 않는다(출근 허용, 근태 보정 §7.4 로 해소).
        //    예외(prafta-app-021-6): 직전일이 이미 마감된 월(전월 말일 미퇴근 + 전월 마감 등)이면
        //    퇴근도 월마감(ATTD_400_042)으로 막혀 출근·퇴근 모두 닫히는 데드락이 된다. 이 경우는
        //    마감된 월의 직전일 미퇴근은 게이트 예외(보정 대상)로 두고 당월 출근을 허용한다.
        String yesterday = LocalDate.parse(today, YMD).minusDays(1).format(YMD);
        int pastOpen = appAttd01Mapper.countPastOpenAttd(cmpnyCd, siteCd, userCd, yesterday);
        if (pastOpen > 0) {
            // (재작업: qa 권고) 마감 예외 판정은 세션 siteCd(현재)가 아니라 D-1 열린 근태가 실귀속된
            // 사업장 기준이어야 한다(TB_ATTD_CLOSE 는 사업장별 독립 마감). 실제 SITE_CD 를 별도 조회.
            OpenAttdResult pastOpenAttd = appAttd01Mapper.selectPastOpenAttd(cmpnyCd, userCd, yesterday);
            String pastOpenSiteCd = (pastOpenAttd != null) ? pastOpenAttd.siteCd() : siteCd;
            // (재작업: qa High) nodeCd 도 pastOpenSiteCd 와 짝을 맞춘다 — 근태 행이 있으면 그 행에
            // 스냅샷된 실귀속 부서(pastOpenAttd.nodeCd())를 그대로 쓰고(TB_USER 재조회 시 이동한
            // 사용자는 0건→'*' 폴백되어 부서단위 마감을 놓침), 행이 없는 예외 상황만 세션 siteCd 기준
            // 현재 소속으로 재조회한다(종전 폴백과 동일).
            String pastOpenNodeCd = (pastOpenAttd != null)
                    ? pastOpenAttd.nodeCd()
                    : resolveNodeCdForClose(cmpnyCd, siteCd, userCd);
            if (!isMonthClosed(cmpnyCd, pastOpenSiteCd, pastOpenNodeCd, yesterday.substring(0, 6))) {
                log.info("[attd01] 셀프 출근 거부: 직전일(today-1) 미퇴근 근태 존재 → 전날 퇴근 등록 안내 (userCd={}, yesterday={}, count={}, pastOpenSiteCd={})", userCd, yesterday, pastOpen, pastOpenSiteCd);
                throw new ApiException(AttdErrorCode.ATTD_400_082);
            }
            // 마감된 월의 직전일 미퇴근은 게이트 예외(보정 대상) — prafta-app-021-6.
            log.info("[attd01] 셀프 출근 게이트 예외: 직전일이 마감된 월이라 퇴근으로 닫을 수 없음 → 출근 허용(보정 대상) (userCd={}, yesterday={}, count={}, pastOpenSiteCd={})", userCd, yesterday, pastOpen, pastOpenSiteCd);
        }

        // 5) 스케줄 조회(그 일자). 연차일이면 출근 차단(§8.3).
        //    prafta-com-008-E-2: 연차일 판정을 work_plan(LEAVE_CD) → leave_use(종일 확정) 로 전환.
        //    work_plan 은 항상 SCH_CD 유지(연차일이어도 근무계획은 보존) → 스케줄 존재 판정은 SCH_CD 로만.
        AttdRangeQuery q = new AttdRangeQuery(cmpnyCd, siteCd, userCd, workYmd, workYmd);
        List<ScheduleResult> schedules = nullSafe(appAttd01Mapper.selectScheduleByRange(q));
        ScheduleResult sched = schedules.isEmpty() ? null : schedules.get(0);
        boolean isLeaveDay = appAttd01Mapper.countFullDayLeaveOn(cmpnyCd, siteCd, userCd, workYmd) > 0;
        boolean hasSchedule = sched != null && sched.schCd() != null;
        boolean isTwoSlot = hasSchedule && StringUtils.hasText(sched.secSchStrTime());

        // prafta-com-008-B(함정4): 기존 "모든 연차일 ATTD_400_083 차단"을 노무수령거부 가드로 대체.
        //   촉진(1·2차) 확정 법정 연차일 + 비휴일이면 guard 내부에서 ATTD_400_150 차단 throw(BLOCKED 이력+PUSH 선커밋).
        //   자발(NONE)/비법정/촉진+휴일 연차일은 guard 가 정상 반환 → 출근 허용(안내는 프론트가 isLeaveDay 로 표시).
        //   INSERT 이전에 호출(레코드 생성 전 차단). 083 은 더 이상 발동하지 않는다(노무수령거부=전용 150 코드).
        //   근태 E2E(F2) 재확인: 본 가드가 "촉진분만 차단, 그 외 연차일 출근 허용"의 서버 단일출처(권위)다.
        //   프론트가 laborRefusal 플래그(home-summary)로 사전 분기해도, 위조/우회 시 최종 차단은 여기서만 수행한다.
        if (isLeaveDay) {
            // nodeCd 는 가드에서 미사용(추후 페이로드 보존용) → JWT 값(param.nodeCd()) 직접 전달.
            //   가드는 본인 식별자(userCd/workYmd)만으로 판정한다.
            leaveRefusalDetectService.guardAndRecord(
                    cmpnyCd, siteCd, userCd, param.nodeCd(), workYmd,
                    LeaveRefusalConst.ATTEMPT_CHECK_IN, userCd);
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

                // PRAFTA-APP-024 후속: 2구간 기록 존재(체크인됨, 퇴근 무관) 시 1구간 역순 출근 차단.
                //   순서 자유는 유지하되 "2구간 먼저 → 이후 1구간" 만 금지(1구간 누락 후 2구간만 출근은 허용).
                //   088(선택 구간 자체 중복) 직후·081(동시 열림) 이전에 위치해 우선순위를 명확히 한다.
                if (target == 1 && attdBySeq.get(2) != null) {
                    log.info("[attd01] 셀프 출근 거부: 2구간 출근 후 1구간 역순 출근 불가 (userCd={}, workYmd={})", userCd, workYmd);
                    throw new ApiException(AttdErrorCode.ATTD_400_089);
                }

                // (b-2) prafta-app-024: 다른 구간이 진행 중(미퇴근)이면 출근 거부(한 시점 열린 구간 최대 1개).
                //   088(선택 구간 자체 중복) 분기 직후에 위치하므로 088 → 081 우선순위가 자연 보장된다.
                //   순서 자유(2구간 먼저 가능)·1구간 누락 허용은 유지하되, "동시에 두 구간이 열려 있는 상태"만 막는다.
                //   잠금(lockUserForCheckIn) 범위 내에서 로딩한 attdBySeq 를 재사용(추가 쿼리/락 불필요, TOCTOU 방어).
                boolean anySlotOpen = attdBySeq.values().stream()
                        .anyMatch(a -> a != null && !StringUtils.hasText(a.checkOutTime()));
                if (anySlotOpen) {
                    log.info("[attd01] 셀프 출근 거부: 다른 구간 미퇴근 진행 중 → 해당 구간 퇴근 후 출근 안내 (userCd={}, workYmd={}, targetWorkSeq={})",
                            userCd, workYmd, target);
                    throw new ApiException(AttdErrorCode.ATTD_400_081);
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
            // 스케줄 없는 날(§7.5): 출근 허용(전량 초과근무 대상). 단 하루 2회로 상한(prafta-app-014, §7.5 정정).
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

        // 8-2) 근무 구간 시각 겹침 금지(정책서 attd §7.6). 슬롯별 가드 통과 직후·INSERT 이전(lock 범위 내)에서
        //      신규 출근 시각이 같은 일자 기존 구간([출근, 퇴근 or open=SENTINEL]) 안에 떨어지면 차단한다.
        //      인접 경계(앞 구간 종료==신규 출근)는 허용(in <= newIn < end 만 겹침으로 판정).
        //      신규 구간은 아직 퇴근이 없는 open 이라, 기존 구간과의 겹침은 "신규 출근 instant 가 기존 구간 내부"
        //      검사로 충분하다(open↔open 동시열림은 081 가드가 이미 차단).
        //      겹침가드 개선(2026-08-06, §7.6 D-1): 손상 구간(퇴근 ≤ 출근 = 길이 0/역전, 퇴근일자 결측)은 판정에서
        //      제외한다 → "출근 누락 → 퇴근 시점에 자리 생성 → 보정 요청" 정상 흐름이 당일 재출근을 영구 차단하던
        //      결함(백로그 B-11) 해소. 당일 이중 출근 차단은 open 구간 SENTINEL + ATTD_400_081 로 그대로 유지된다.
        {
            Integer newInStamp = DateTimeUtils.toMinuteStamp(workYmd, today, checkInTime);
            if (newInStamp != null) {
                AttdRangeQuery overlapQ = new AttdRangeQuery(cmpnyCd, siteCd, userCd, workYmd, workYmd);
                for (AttdRecordResult a : nullSafe(appAttd01Mapper.selectAttdByRange(overlapQ))) {
                    AttdOverlapUtils.Segment exSeg = AttdOverlapUtils.buildSegment(
                            workYmd, a.workYmd(), a.attdId(), String.valueOf(a.workSeq()),
                            a.checkInDate(), a.checkInTime(), a.checkOutDate(), a.checkOutTime(), false);
                    if (exSeg == null) continue; // 출근 stamp 없는 구간은 검사 제외
                    if (!exSeg.judgeable()) {
                        // 손상 행은 점유 폭 0 으로 보고 판정 제외(운영 탐지용 경고만).
                        log.warn("[겹침가드] 퇴근시각 미성립 근태 행 판정 제외. workYmd={}, workSeq={}, attdId={}, kind={}",
                                exSeg.workYmd(), exSeg.workSeq(), exSeg.attdId(), exSeg.kind());
                        continue;
                    }
                    // exSeg.start <= newIn < exSeg.end 이면 신규 출근이 기존 구간 내부 → 겹침.
                    if (exSeg.start() <= newInStamp && newInStamp < exSeg.end()) {
                        log.info("[attd01] 셀프 출근 거부: 근무 구간 시각 겹침(§7.6) (userCd={}, workYmd={}, newIn={}, exAttdId={}, exKind={})",
                                userCd, workYmd, checkInTime, a.attdId(), exSeg.kind());
                        throw new ApiException(AttdErrorCode.ATTD_400_113,
                                AttdOverlapMessages.overlapMessage(workYmd, exSeg));
                    }
                }
            }
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
        //   ★위치정보 동의철회·중지 S3: 동의(AGREED) 상태가 아니면 좌표를 저장하지 않는다.
        //     중지·철회는 "이후 수집 중단"이 정의이고(법 제24조①②), 이를 무시하고 저장하면 그 자체가 위반이다.
        //     ★종전에는 005 가 로그인 게이트에 잡혀 미동의로는 진입 자체가 불가능했기에 이 판정이 없었다.
        if (isOffsite && locationConsentService.isCollectAllowed(cmpnyCd, userCd)) {
            String gpsId = appAttd01Mapper.selectGpsId(cmpnyCd);
            // GPS좌표-암호화-전환-02: 좌표는 암호문만 저장(평문 LAT/LON 기록 중단). 좌표/암호문 로그 금지.
            CheckInGpsCommand gpsCmd = new CheckInGpsCommand(
                    gpsId, cmpnyCd, attdId, siteCd, userCd, GPS_TYPE_CHECK_IN,
                    gpsCoordCrypto.encrypt(param.lat()), gpsCoordCrypto.encrypt(param.lon()), param.accuracy(),
                    today, apiCallTime, param.isMocked(), param.ipAddr(), param.offsiteReason(), userCd);
            appAttd01Mapper.insertCheckInGps(gpsCmd);
        }

        log.info("[attd01] 셀프 출근 완료 (userCd={}, attdId={}, workYmd={}, workSeq={}, checkInTime={}, isOffsite={})",
                userCd, attdId, workYmd, workSeq, checkInTime, isOffsite);

        // 9-1) PRAFTA-COM-008-B: 노무수령거부 사후 감지 hook 제거(detect→block 전환).
        //      차단은 진입부 5)의 guardAndRecord(CHECK_IN)가 INSERT 이전에 수행한다(여기 도달 = 차단 비대상).

        // 9-2) PRAFTA-APP-021-3c(M1): 지각 감지(raw 실근태) + 노드 관리자 PUSH(예외 격리, 체크인 영향 없음).
        //      스케줄 없는 날은 판정 기준이 없어 생략(연차일은 위에서 이미 출근 차단됨).
        //      선택 구간 시작 시각: 2구간 스케줄이면 출근 구간(workSeq)에 맞춰 1구간=FST/2구간=SEC, 그 외 FST.
        if (hasSchedule) {
            // ★ HB-05(D1): PUSH 도 화면과 동일한 "반차 반영 유효 소정 구간"으로 판정해야 한다.
            //   이걸 빼면 화면은 정시로 보이는데 관리자에게 지각 PUSH 만 오발송된다.
            String pushSchStr = (isTwoSlot && workSeq == 2) ? sched.secSchStrTime() : sched.fstSchStrTime();
            String pushSchEnd = (isTwoSlot && workSeq == 2) ? sched.secSchEndTime() : sched.fstSchEndTime();
            String schStartHhmm = effectiveStart(pushSchStr, pushSchEnd,
                    selectHalfDayLeavesOn(cmpnyCd, siteCd, userCd, workYmd));
            try {
                // ★ qa N-2: 원 스케줄(pushSchStr/pushSchEnd)을 일자 프레임으로 함께 넘긴다
                //   (야간 시작기준 반차의 유효 시작 01:15 를 당일로 두면 허위 지각 PUSH 가 나간다).
                attdLateEarlyNotiService.detectLate(cmpnyCd, siteCd, userCd, nodeCd, workYmd, attdId,
                        today, checkInTime, pushSchStr, pushSchEnd, schStartHhmm, userCd);
            } catch (Exception e) {
                log.error("[attd01] 지각 감지 hook 실패(체크인 영향 없음) (userCd={}, workYmd={})", userCd, workYmd, e);
            }
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
                // PRAFTA-FIXEDOT-2: 재조립 시 고정연장 요약도 보존.
                .fixedOtSummary(src.getFixedOtSummary())
                .attendanceSummary(src.getAttendanceSummary())
                .workStatus(src.getWorkStatus())
                .isTwoSlot(src.isTwoSlot())
                .slotCount(src.getSlotCount())
                .slots(src.getSlots())
                .actions(src.getActions())
                .sheetActions(src.getSheetActions())
                .dayType(src.getDayType())
                .hasIssue(src.isHasIssue())
                .isHoliday(src.isHoliday())
                .holidayName(src.getHolidayName())
                // 재조립 시 부분연차(시간차/반차) 표시필드도 보존 — 그날 휴가를 쓰고 오프사이트 퇴근하면
                //   마커가 사라지던 기존 누락을 함께 교정(단건 스칼라 + 다건 목록 모두 보존).
                .isLeaveUsed(src.isLeaveUsed())
                .leaveTypeName(src.getLeaveTypeName())
                .leaveUnitType(src.getLeaveUnitType())
                .leaveTimeRange(src.getLeaveTimeRange())
                .leaveDays(src.getLeaveDays())
                .leaves(src.getLeaves())
                // prafta-app-030 후속: 재조립 시 적용 초과근무 목록도 보존(buildDayResponse 산출값).
                .appliedOvertimes(src.getAppliedOvertimes())
                .leaveExemptWindows(src.getLeaveExemptWindows())
                // PRAFTA-FIXEDOT-2: 재조립 시 고정연장 점유 구간도 보존.
                .fixedOtWindows(src.getFixedOtWindows())
                .isOffsite(isOffsite)
                .build();
    }

    /**
     * OT 칩 정합(2026-08-08): 그날 확정 부분연차(시각 보유 = 반차/시간차)를 면제 구간 아이템으로 환산한다.
     *
     * <p>산출은 앱 OT 신청 검증({@code AppReq07ServiceImpl.assertNoLeaveExemptOverlap})과 동일 규칙:
     * 단일 진입점 {@code PartialLeaveWindowUtils.exemptStampRange}(그날 원 스케줄 프레임 정렬).
     * 환산 불가(스케줄 프레임 부재·시각 비정상)는 <b>그날 전체 구간</b>으로 보수 변환한다 —
     * 검증이 그날 OT 를 전면 거부(196)하므로 칩도 0건이 되어야 정합(§15-2-3 fail-open 금지 승계).
     * 종일(00)·시각 미보유 행은 대상 아님(종일연차일은 슬롯 자체가 0이라 OT 폼 미진입).
     */
    private List<LeaveExemptWindowItem> toLeaveExemptWindows(
            String targetYmd, List<LeaveUseResult> dayLeaves, ScheduleResult sched) {
        if (dayLeaves == null || dayLeaves.isEmpty()) {
            return List.of();
        }
        List<PartialLeaveWindowUtils.ScheduleSegment> frame = (sched == null)
                ? List.of()
                : PartialLeaveWindowUtils.scheduleSegments(
                        sched.fstSchStrTime(), sched.fstSchEndTime(),
                        sched.secSchStrTime(), sched.secSchEndTime());
        List<LeaveExemptWindowItem> out = new ArrayList<>();
        for (LeaveUseResult lv : dayLeaves) {
            if (lv == null || !StringUtils.hasText(lv.startTime()) || !StringUtils.hasText(lv.endTime())) {
                continue; // 종일 등 시각 미보유 — 면제 칩 대상 아님
            }
            int[] range = PartialLeaveWindowUtils.exemptStampRange(lv.startTime(), lv.endTime(), frame);
            if (range == null) {
                // 검증(196 전면 거부)과 정합 — 그날 전체를 면제로 내려 칩을 0건으로 만든다.
                log.warn("[OT칩] 연차 면제 구간 환산 불가(그날 칩 전체 차단). workYmd={}, leave={}~{}",
                        targetYmd, lv.startTime(), lv.endTime());
                range = PartialLeaveWindowUtils.fullDayBlockStampRange();
            }
            out.add(LeaveExemptWindowItem.fromStampRange(targetYmd, range));
        }
        return out;
    }

    /**
     * PRAFTA-FIXEDOT-2: 그날 스케줄의 고정연장(전방·후방) 점유 구간을 (일자,시각) 아이템으로 환산한다.
     *
     * <p>환산은 {@link FixedOtScheduleUtils#fixedOtSegments} 단일 출처(일 anchor) → +1440 으로
     * 공용 stamp 축(원점 = 전날 00:00)에 올린 뒤 {@link LeaveExemptWindowItem#fromStampRange} 재사용.
     * 서버 검증({@code AppReq07ServiceImpl.assertNoFixedOtOverlap})과 동일 프레임이라 칩과 검증이
     * 일치한다. 고정연장 없는 근무타입은 빈 리스트(무회귀).
     */
    private List<LeaveExemptWindowItem> toFixedOtWindows(String targetYmd, ScheduleResult sched) {
        if (sched == null) {
            return List.of();
        }
        List<int[]> segs = FixedOtScheduleUtils.fixedOtSegments(
                sched.fstSchStrTime(), sched.fstSchEndTime(),
                sched.secSchStrTime(), sched.secSchEndTime(),
                sched.preFixedOtStrTime(), sched.preFixedOtEndTime(),
                sched.fixedOtStrTime(), sched.fixedOtEndTime());
        if (segs.isEmpty()) {
            return List.of();
        }
        List<LeaveExemptWindowItem> out = new ArrayList<>(segs.size());
        for (int[] seg : segs) {
            out.add(LeaveExemptWindowItem.fromStampRange(targetYmd,
                    new int[] { seg[0] + 1440, seg[1] + 1440 }));
        }
        return out;
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
            if (GPS_TYPE_CHECK_IN.equals(g.gpsInfoType())) {
                gpsByCheckIn.putIfAbsent(g.attdId(), g);
            } else if (GPS_TYPE_CHECK_OUT.equals(g.gpsInfoType())) {
                gpsByCheckOut.putIfAbsent(g.attdId(), g);
            }
        }
    }

    /**
     * WORK_YMD -> 스케줄(근무계획) 매핑.
     *
     * <p>작업지시서_소속이동-이력가시성-보정(QA High 보정): TB_USER_WORK_PLAN 은 (CMPNY_CD, SITE_CD,
     *   USER_CD, WORK_YMD) 복합 PK 라 소속이동 시 구 사업장 DEFAULT_SCH 행이 정리되지 않으면 동일
     *   WORK_YMD 에 SITE_CD 가 다른 행이 공존할 수 있다(실측: 개발 DB 확인). 종전 putIfAbsent(선착순,
     *   ORDER BY WORK_YMD 뿐인 SQL 반환 순서 의존)는 실행계획에 따라 비결정적으로 한쪽을 조용히 버렸다.
     *   {@code sessionSiteCd}(JWT 캐노니컬라이즈된 현재 소속) 와 일치하는 행을 최우선으로 선택하고,
     *   둘 다 일치/불일치가 같으면 {@link ScheduleResult#effectiveDtime()}(최신 갱신시각) 이 큰 쪽을
     *   선택해 결과를 결정론적으로 고정한다.
     */
    private Map<String, ScheduleResult> indexSchedule(List<ScheduleResult> list, String sessionSiteCd) {
        Map<String, ScheduleResult> map = new HashMap<>();
        for (ScheduleResult s : list) {
            ScheduleResult current = map.get(s.workYmd());
            if (current == null || prefersCandidate(
                    s.siteCd(), s.effectiveDtime(), current.siteCd(), current.effectiveDtime(), sessionSiteCd)) {
                map.put(s.workYmd(), s);
            }
        }
        return map;
    }

    /**
     * WORK_SEQ -> 근태레코드 매핑(하루치 리스트 대상).
     *
     * <p>작업지시서_소속이동-이력가시성-보정(QA High 보정): TB_USER_ATTD_MGMT 는 ATTD_ID 가 PK 라
     *   SITE_CD 가 아니므로, 동일 WORK_YMD+WORK_SEQ 에 SITE_CD 가 다른 행이 공존할 수 있다(실측:
     *   개발 DB 확인). indexSchedule 과 동일한 우선순위 규칙(세션 SITE_CD 일치 우선 → 최신 갱신시각)
     *   으로 결정론적으로 병합한다.
     */
    private Map<Integer, AttdRecordResult> mergeAttdBySeq(List<AttdRecordResult> list, String sessionSiteCd) {
        Map<Integer, AttdRecordResult> map = new HashMap<>();
        for (AttdRecordResult a : list) {
            AttdRecordResult current = map.get(a.workSeq());
            if (current == null || prefersCandidate(
                    a.siteCd(), a.effectiveDtime(), current.siteCd(), current.effectiveDtime(), sessionSiteCd)) {
                map.put(a.workSeq(), a);
            }
        }
        return map;
    }

    /**
     * 동일 키(WORK_YMD 또는 WORK_YMD+WORK_SEQ)에 SITE_CD 가 다른 후보가 공존할 때의 결정론적 우선순위.
     *
     * <p>① 세션 SITE_CD(sessionSiteCd)와 일치하는 후보를 우선(현재 소속 기준 표시가 자연스럽다).
     *   ② 일치 여부가 같으면(둘 다 일치 또는 둘 다 불일치) {@code effectiveDtime}(UPDATE_DATE 우선,
     *   없으면 INSERT_DATE) 이 더 최신인 후보를 우선(더 최근에 반영된 값이 실제에 더 가깝다고 본다).
     *   ③ effectiveDtime 까지 동률(또는 둘 다 null)이면 기존 선택을 유지(안정적 선입 유지, 회귀 최소화).
     */
    private boolean prefersCandidate(
            String candidateSiteCd, java.time.LocalDateTime candidateDtime,
            String currentSiteCd, java.time.LocalDateTime currentDtime, String sessionSiteCd) {
        boolean candidateMatches = sessionSiteCd != null && sessionSiteCd.equals(candidateSiteCd);
        boolean currentMatches = sessionSiteCd != null && sessionSiteCd.equals(currentSiteCd);
        if (candidateMatches != currentMatches) {
            return candidateMatches;
        }
        if (candidateDtime == null) {
            return false;
        }
        if (currentDtime == null) {
            return true;
        }
        return candidateDtime.isAfter(currentDtime);
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
     * 연차 사용행을 조회 범위 내 일자별 "전체 목록"으로 펼친다(CONFIRMED 기준, 다건 보존).
     * expandLeave 가 일자당 첫 1건만 보존하던 것과 달리, 같은 날 다건(시간차 2건 등)을 모두 모은다.
     * 표시(마커) 전용 — 근무일/연차일 판정에는 사용하지 않는다(그쪽은 expandLeave 단건 유지).
     */
    private Map<String, List<LeaveUseResult>> expandLeaveMulti(List<LeaveUseResult> list, String fromYmd, String toYmd) {
        Map<String, List<LeaveUseResult>> map = new HashMap<>();
        for (LeaveUseResult lv : list) {
            String s = max(lv.startDate(), fromYmd);
            String e = min(lv.endDate(), toYmd);
            if (s.compareTo(e) > 0) continue;
            LocalDate cur = LocalDate.parse(s, YMD);
            LocalDate end = LocalDate.parse(e, YMD);
            while (!cur.isAfter(end)) {
                map.computeIfAbsent(cur.format(YMD), k -> new ArrayList<>()).add(lv);
                cur = cur.plusDays(1);
            }
        }
        return map;
    }

    /**
     * 그날 연차 사용내역(LeaveUseResult) 목록 → 표시 마커 항목(LeaveMarkerItem) 목록.
     * 시각 오름차순(시작시각 없는 종일/반차는 앞)으로 정렬해 일관 표시한다. 빈 입력이면 빈 리스트.
     * 단건 스칼라(leaveTypeName 등)와 동일 의미/포맷 — FE attdFormat.formatLeaveMarker 가 그대로 소비한다.
     */
    private List<LeaveMarkerItem> toLeaveMarkers(List<LeaveUseResult> leaves) {
        if (leaves == null || leaves.isEmpty()) return Collections.emptyList();
        List<LeaveUseResult> sorted = new ArrayList<>(leaves);
        sorted.sort(Comparator.comparing(LeaveUseResult::startTime,
                Comparator.nullsFirst(Comparator.naturalOrder())));
        List<LeaveMarkerItem> items = new ArrayList<>(sorted.size());
        for (LeaveUseResult lv : sorted) {
            items.add(new LeaveMarkerItem(
                    lv.leaveNm(),
                    lv.useUnitType(),
                    leaveTimeRange(lv.startTime(), lv.endTime()),
                    lv.leaveDays(),
                    // PRAFTA_COM_002-B-1: 건별 "요청중" 파생(다건 시 건마다 승인상태가 다를 수 있어 마커별 산출).
                    isLeavePending(lv)));
        }
        return items;
    }

    /**
     * PRAFTA_COM_002-B-1: 연차 사용행이 승인 대기(요청중)인지 판정.
     *
     * <p>요청중 = 연관 요청행이 존재(REQ_ID NOT NULL → reqStatus 조인됨)하고 그 상태가 '01'(신청)인 경우.
     *   무결재 즉시확정(REQ_ID NULL → reqStatus null) 또는 승인('02')은 확정으로 보고 false(배지 없음).
     *   (반려'03'/취소'04'는 표시 대상 행이 아니므로 실무상 도달하지 않으나, 정의상 false 처리.)
     */
    private boolean isLeavePending(LeaveUseResult leave) {
        return leave != null && "01".equals(leave.reqStatus());
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

    /**
     * 해당 월(YYYYMM)이 nodeCd 기준 부서를 덮는 마감으로 닫혀있는지.
     *
     * <p>(재작업: qa High) 종전에는 이 메서드 내부에서 {@code selectUserNodeCd(cmpnyCd, siteCd, userCd)}
     *   로 TB_USER 를 재조회했는데, TB_USER 는 "현재" SITE_CD 한 행만 가지므로 이동한 사용자의
     *   과거/실귀속 siteCd(예: {@code open.siteCd()})를 넘기면 0건이 나와 nodeCd 가 항상 '*' 로
     *   폴백되어 부서단위 마감을 놓쳤다. 이제 nodeCd 는 재조회하지 않고 호출부가 이미 확보한 값
     *   (근태 행의 NODE_CD 또는 세션 siteCd 기준 재조회 결과)을 그대로 받는다.
     */
    private boolean isMonthClosed(String cmpnyCd, String siteCd, String nodeCd, String closeYm) {
        // 소속부서 미상이면 '*'(전체 사업장) 마감만으로 판정(종전 폴백 유지, 방어적으로 이 메서드에서 정규화).
        String effectiveNodeCd = StringUtils.hasText(nodeCd) ? nodeCd : "*";
        int covering = appAttd01Mapper.countCoveringClose(cmpnyCd, siteCd, effectiveNodeCd, closeYm);
        return covering > 0;
    }

    /**
     * siteCd 기준 사용자 소속 노드 조회(월마감 판정 전용, 폴백 '*').
     *
     * <p>근태 행의 실귀속 nodeCd(open.nodeCd() 등)를 이미 확보한 경우는 이 헬퍼를 쓰지 않고 그 값을
     *   바로 isMonthClosed 에 넘긴다. 이 헬퍼는 "지금 이 siteCd 기준 현재 소속"이 필요한 조회/신규
     *   출근 시나리오에서만 사용한다(TB_USER 조회이므로 siteCd 는 반드시 사용자의 "현재" 소속 사업장이어야
     *   0건이 나지 않는다).
     */
    private String resolveNodeCdForClose(String cmpnyCd, String siteCd, String userCd) {
        String nodeCd = appAttd01Mapper.selectUserNodeCd(cmpnyCd, siteCd, userCd);
        return StringUtils.hasText(nodeCd) ? nodeCd : "*";
    }

    // ====================================================================
    // 공통 헬퍼: 시간 계산
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
     * 지각 판정: 실제 출근 일시(출근일자+출근시각) &gt; 판정용 시작 일시.
     *
     * <p>시:분만 비교하던 종전 방식은 야간/자정 넘김에서 오판했다. 예) 슬롯 시작이 00:00 이고
     *   근무자가 전일 23:54 에 출근하면 실제로는 6분 전 출근이지만 시:분 비교(2354&gt;0000)로는
     *   지각으로 잘못 잡혔다. 출근일자(CHECK_IN_DATE)를 포함한 통합 분 stamp 로 비교하여
     *   웹 Attd_11(PRAFTA-034) 과 동일 기준으로 판정한다.
     *
     * <p>★ qa N-2(2026-08-07): 판정용 시작을 <b>항상 근무일 당일</b>로 두던 종전 규칙은 야간
     *   시작기준 반차(판정용 시작 01:15 = 익일)를 지각으로 오판정했다(같은 사람이 Attd_08/Attd_11
     *   에서는 정상 — 3경로 불일치). 일자 프레임은 <b>원 스케줄</b>로 잡는다
     *   ({@code PartialLeaveWindowUtils.dayOffsetOf} — Attd_08 {@code shiftYmd} 와 동일 규칙).
     *
     * @param rawSchStrHhmm 원 스케줄 구간 시작(반차 반영 <b>전</b>)
     * @param rawSchEndHhmm 원 스케줄 구간 종료(반차 반영 <b>전</b>)
     * @param schStrHhmm    판정용(반차 반영 후) 시작 시각
     */
    private boolean isLateStamp(AttdRecordResult attd, String workYmd,
                                String rawSchStrHhmm, String rawSchEndHhmm, String schStrHhmm) {
        if (attd == null || !hasHhmm(attd.checkInTime()) || !hasHhmm(schStrHhmm) || workYmd == null) {
            return false;
        }
        long schStartStamp = toMinuteStamp(
                shiftYmd(workYmd, rawSchStrHhmm, rawSchEndHhmm, schStrHhmm), schStrHhmm);
        String inYmd = StringUtils.hasText(attd.checkInDate()) ? attd.checkInDate() : workYmd;
        long actInStamp = toMinuteStamp(inYmd, attd.checkInTime());
        return actInStamp > schStartStamp;
    }

    /**
     * 조퇴 판정: 실제 퇴근 일시(퇴근일자+퇴근시각) &lt; 판정용 종료 일시.
     *
     * <p>★ qa N-2(2026-08-07): 익일 여부를 "유효 종료 &lt; 유효 시작"으로 보던 종전 규칙은
     *   야간 종료기준 반차(유효 종료 01:15, 유효 시작 22:00)에서 자정 넘김을 놓쳐 조기 퇴근을
     *   조퇴로 잡지 못했다(fail-open). 일자 프레임은 <b>원 스케줄</b>로 잡는다(Attd_08/Attd_11 동일).
     *
     * @param rawSchStrHhmm 원 스케줄 구간 시작(반차 반영 <b>전</b>)
     * @param rawSchEndHhmm 원 스케줄 구간 종료(반차 반영 <b>전</b>)
     * @param schEndHhmm    판정용(반차 반영 후) 종료 시각
     */
    private boolean isEarlyStamp(AttdRecordResult attd, String workYmd,
                                 String rawSchStrHhmm, String rawSchEndHhmm, String schEndHhmm) {
        if (attd == null || !hasHhmm(attd.checkOutTime()) || !hasHhmm(schEndHhmm) || workYmd == null) {
            return false;
        }
        long schEndStamp = toMinuteStamp(
                shiftYmd(workYmd, rawSchStrHhmm, rawSchEndHhmm, schEndHhmm), schEndHhmm);
        String outYmd = StringUtils.hasText(attd.checkOutDate()) ? attd.checkOutDate() : workYmd;
        long actOutStamp = toMinuteStamp(outYmd, attd.checkOutTime());
        return actOutStamp < schEndStamp;
    }

    /**
     * 판정용 시각이 속한 일자(근무일 또는 익일) — 원 스케줄 프레임 기준.
     * 웹 {@code Attd08ServiceImpl.shiftYmd} / {@code Attd11ServiceImpl.shiftYmd} 이식(3경로 일치, D-1).
     */
    private String shiftYmd(String workYmd, String rawSchStr, String rawSchEnd, String hhmm) {
        int offset = PartialLeaveWindowUtils.dayOffsetOf(rawSchStr, rawSchEnd, hhmm);
        return (offset == 0) ? workYmd : ymdPlusDays(workYmd, offset);
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
    /**
     * prafta-app-031: "근무중(진행 중 슬롯 존재)" 판정 — 기록된 슬롯 중 퇴근 시각이 없는(열린) 슬롯이
     *   하나라도 있으면 true. 스케줄 슬롯이 단지 "미사용(기록 없음)"인 것은 근무중이 아니다.
     *   (2구간 스케줄에서 1구간 또는 2구간만 정상 근무(출근+퇴근)한 날은 근무중이 아님 → 완료된 구간 기준
     *    으로 근태 보정/초과근무 액션을 허용한다.) 라인 205(app-024 출근 게이팅)와 동일 술어.
     *   액션 게이팅에서 allSlotsCompleted(스케줄 전 슬롯 충족)의 보수적 판정 대신 사용한다.
     */
    private boolean hasOpenSlot(Map<Integer, AttdRecordResult> attdBySeq) {
        return attdBySeq.values().stream()
                .anyMatch(a -> a != null && !StringUtils.hasText(a.checkOutTime()));
    }

    /**
     * PRAFTA-FIXEDOT-2(표기): 예정 합계에 편입할 고정연장 분(전방+후방, 휴게 없음).
     * 환산은 {@link FixedOtScheduleUtils#totalFixedOtMinutes} 단일 출처. 없는 타입은 0(무회귀).
     */
    private int fixedOtPlannedMinutes(ScheduleResult sched) {
        if (sched == null) {
            return 0;
        }
        return FixedOtScheduleUtils.totalFixedOtMinutes(
                sched.fstSchStrTime(), sched.fstSchEndTime(),
                sched.secSchStrTime(), sched.secSchEndTime(),
                sched.preFixedOtStrTime(), sched.preFixedOtEndTime(),
                sched.fixedOtStrTime(), sched.fixedOtEndTime());
    }

    /**
     * PRAFTA-FIXEDOT-2(표기): 고정연장 요약 문자열 — scheduleSummary 와 동일 형상(raw HHMM).
     * 전방·후방 각각 "HHMM~HHMM", 둘 다 있으면 " / " 로 연결(전방 먼저). 없으면 null.
     * FE(attdFormat.formatTimeSummary)가 그대로 "HH:MM ~ HH:MM" 로 변환해 "고정연장" 라벨과 표기한다.
     */
    private String fixedOtSummary(ScheduleResult sched) {
        if (sched == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(sched.preFixedOtStrTime()) && StringUtils.hasText(sched.preFixedOtEndTime())) {
            sb.append(sched.preFixedOtStrTime()).append("~").append(sched.preFixedOtEndTime());
        }
        if (StringUtils.hasText(sched.fixedOtStrTime()) && StringUtils.hasText(sched.fixedOtEndTime())) {
            if (sb.length() > 0) {
                sb.append(" / ");
            }
            sb.append(sched.fixedOtStrTime()).append("~").append(sched.fixedOtEndTime());
        }
        return sb.length() > 0 ? sb.toString() : null;
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
     * 실제 출퇴근 기준으로 (퇴근-출근-휴게)를 누적한다.
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

    private String resolveWorkPlanName(ScheduleResult sched, boolean isLeaveDay, String leaveName) {
        // prafta-com-008-E-2: 연차일이면 leave_use 의 연차명(work_plan 은 SCH_CD 유지).
        if (isLeaveDay) {
            return leaveName;
        }
        if (sched == null) {
            return null;
        }
        // 근무일: 스케줄 한글명 컬럼이 스키마에 없어 SCH_NO 사용(plan 확정 §1).
        return sched.schNo();
    }

    /**
     * prafta-com-008-E-2: 그날이 "종일 연차일" 인지 판정(출근 차단/슬롯0 근거).
     * leave_use 의 종일(USE_UNIT_TYPE='00') 확정 행이 있으면 연차일. 부분연차(반차/시간차)는 근무일 유지.
     */
    private boolean isFullDayLeave(LeaveUseResult leave) {
        return leave != null && LEAVE_UNIT_FULL.equals(leave.useUnitType());
    }

    // ====================================================================
    // 소규모 유틸 (모듈 내부 전용 — common.util 승격 대상 아님)
    // ====================================================================

    private static <T> List<T> nullSafe(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    // ====================================================================
    // prafta-app-030 후속: 적용(승인) 초과근무 매핑/그룹/합계 (표시 전용)
    // ====================================================================

    /** RangeOvertimeResult 1건 → AppliedOvertimeItem(표시 항목). workYmd 는 상위 일자에 종속되므로 미포함. */
    private AppliedOvertimeItem toOvertimeItem(RangeOvertimeResult r) {
        return AppliedOvertimeItem.builder()
                .startDate(r.startDate())
                .startTime(r.startTime())
                .endDate(r.endDate())
                .endTime(r.endTime())
                .workMinutes(r.workMinutes())
                .build();
    }

    /** RangeOvertimeResult 목록 → workYmd 별 그룹 Map(범위 1회 호출 결과를 일자별로 분배 — N+1 회피). */
    private Map<String, List<RangeOvertimeResult>> groupOvertimeByYmd(List<RangeOvertimeResult> rows) {
        Map<String, List<RangeOvertimeResult>> byYmd = new HashMap<>();
        for (RangeOvertimeResult r : nullSafe(rows)) {
            byYmd.computeIfAbsent(r.workYmd(), k -> new ArrayList<>()).add(r);
        }
        return byYmd;
    }

    /** 일자 그룹의 WORK_MINUTES 합계(null 분은 0으로 무시). */
    private int sumOvertimeMinutes(List<RangeOvertimeResult> rows) {
        int sum = 0;
        for (RangeOvertimeResult r : nullSafe(rows)) {
            if (r.workMinutes() != null) sum += r.workMinutes();
        }
        return sum;
    }

    /** 일자 그룹 → AppliedOvertimeItem 목록(없으면 빈 리스트). */
    private List<AppliedOvertimeItem> toOvertimeItems(List<RangeOvertimeResult> rows) {
        List<AppliedOvertimeItem> items = new ArrayList<>();
        for (RangeOvertimeResult r : nullSafe(rows)) {
            items.add(toOvertimeItem(r));
        }
        return items;
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
