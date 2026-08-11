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
import com.prafta.app.home.home01.result.AppliedLeaveSummaryResult;
import com.prafta.app.home.home01.result.AttdMgmtResult;
import com.prafta.app.home.home01.result.LeaveSummaryResult;
import com.prafta.app.home.home01.result.OvernightScheduleResult;
import com.prafta.app.home.home01.result.PrevDayOpenAttdResult;
import com.prafta.app.home.home01.result.ScheduleResult;
import com.prafta.common.cmm.schedule.util.FixedOtScheduleUtils;
import com.prafta.app.home.home01.result.TbmStatusResult;
import com.prafta.app.home.home01.service.AppHome01Service;
import com.prafta.common.cmm.leave.mapper.LeavePolicyMapper;
import com.prafta.common.cmm.leave.service.LeaveRefusalDetectService;
import com.prafta.common.cmm.leave.util.FiscalYearUtils;
import com.prafta.common.cmm.leave.vo.LeavePolicyVO;

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
    /** 연차 개편(표시): 활성정책 AXIS2_FISCAL_START_MM/_DD 조회(회계연도 경계 산출 입력). 락 없는 조회 메서드 재사용. */
    private final LeavePolicyMapper leavePolicyMapper;
    /** 근태 E2E(F2): 촉진 노무수령거부 대상 여부 사전 판정(조회 전용, 부작용 없음) — 홈 카드 출근 분기용. */
    private final LeaveRefusalDetectService leaveRefusalDetectService;

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
                .attendance(buildAttendance(attdQuery, todayYmd, baseYmd))
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
     * <p>prevDayCheckoutPending(prafta-app-021 §7.6): baseYmd 가 오늘일 때만, 직전일(today-1) 미퇴근이 남아 있으면
     *   전날 마감 대기 신호를 켜고 퇴근 버튼을 활성화한다. 오버나이트(baseYmd=전일)면 기존 hasOpen 경로로 처리되므로
     *   중복 표시를 피하기 위해 신호를 켜지 않는다(E6).
     *
     * @param query    attendance 전용 기준일(baseYmd) 스코프 쿼리
     * @param todayYmd DB 기준 오늘(YYYYMMDD) — 직전일 산출용
     * @param baseYmd  attendance 기준일(YYYYMMDD) — 오버나이트면 today-1
     */
    private HomeSummaryResponse.Attendance buildAttendance(HomeSummaryQuery query, String todayYmd, String baseYmd) {

        List<AttdMgmtResult> attds = appHome01Mapper.selectTodayAttdList(query);
        // prafta-app-013-1: sch 는 attendance 전용 기준일(baseYmd) 로 조회된다(query 가 attdQuery).
        ScheduleResult sch = appHome01Mapper.selectTodaySchedule(query);

        // prafta-com-008-E (H1): 기준일(baseYmd) 종일 연차일 판정.
        //   E-2 전환으로 연차일에도 work_plan 에 SCH_CD 가 남아 selectTodaySchedule 이 스케줄을 반환 →
        //   카드가 BEFORE_WORK·canCheckIn=true 로 오표시되던 문제 보정.
        //   판정 단일출처는 attd01 checkIn(083) 과 동일(USE_UNIT_TYPE='00' 확정 연차 존재). query.todayYmd()=baseYmd.
        //   종일 연차일이면 근무 스케줄을 노출하지 않고 출근 버튼을 비활성화한다(부분연차는 근무일 유지 → 비대상).
        boolean isLeaveDay = appHome01Mapper.countFullDayLeaveOn(
                query.cmpnyCd(), query.siteCd(), query.userCd(), query.todayYmd()) > 0;
        if (isLeaveDay) {
            // 연차일은 근무 스케줄을 카드에 노출하지 않는다(연차 차단 정합). 실 출퇴근 레코드 표시/퇴근 가능 여부는 유지.
            sch = null;
        }
        // 근태 E2E(F2): 종일 연차일 중 "촉진(1·2차) 확정 법정 연차일 · 비휴일" = 노무수령거부 차단 대상 여부.
        //   연차일일 때만 조회(비연차일은 대상 아님 → 불필요 쿼리 회피). 서버 최종 차단은 ATTD_400_150(guardAndRecord),
        //   본 플래그는 프론트가 출근 시 확인 팝업 대신 차단 안내를 사전 노출하기 위한 표시 보조값이다.
        boolean laborRefusal = isLeaveDay && leaveRefusalDetectService.isRefusalTarget(
                query.cmpnyCd(), query.siteCd(), query.userCd(), query.todayYmd());
        // 외근 여부: 오늘 최근 출근 레코드의 출근 GPS 행(01) 존재 여부(존재=외근). 오늘 출근 없으면 0.
        boolean isOffsite = appHome01Mapper.selectTodayCheckInOffsite(query) > 0;

        // prafta-app-021 (§7.6): 직전일(today-1) 미퇴근 조회. baseYmd 가 오늘일 때만 신호로 사용한다(오버나이트 중복 표시 회피, E6).
        //   오버나이트(baseYmd != today)면 전일 근무는 기존 hasOpen 경로로 표시되므로 별도 신호를 켜지 않는다.
        boolean baseIsToday = baseYmd != null && baseYmd.equals(todayYmd);
        PrevDayOpenAttdResult prevDayOpen = null;
        if (baseIsToday) {
            String prevYmd = prevDayOf(todayYmd);
            if (prevYmd != null) {
                prevDayOpen = appHome01Mapper.selectPrevDayOpenAttd(query, prevYmd);
            }
        }

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
        // prafta-app-014: 출근 상한을 attd01 과 동일하게 "하루 2회"로 고정(스케줄 구간 수 무관).
        //   1구간/스케줄 없는 날에서도 1회 퇴근 후 2번째 출근 버튼이 뜨도록(canCheckIn = !hasOpen && existing<2).
        //   2구간 스케줄 표시 로직(아래 §5.5 "2구간을 예정으로 노출")은 그대로 보존.
        int maxSlots = 2;
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
            // 1구간 스케줄 또는 스케줄 없는 날(추가 출근 WORK_SEQ 2 포함).
            //   prafta-app-023: 추가 출근으로 생긴 진행 중(미퇴근) 구간을 status 에 반영한다.
            //   기존엔 s1(WORK_SEQ 1)만 보고 status 를 산출해, 1구간 퇴근 후 2번째 출근(미퇴근)
            //   상태가 "퇴근 완료"로 오표시되고 퇴근 버튼(hasOpen 기반 활성)과 불일치했다.
            if (!s1In && !s2In) {
                status = STATUS_BEFORE_WORK;
            } else if (s1In && !s1Out) {
                // 1구간 진행 중.
                status = STATUS_WORKING;
                checkInTime = s1.checkInTime();
            } else if (s2In && !s2Out) {
                // 2번째(추가) 출근 진행 중 → 근무중(2번째 출근시각 표시).
                status = STATUS_WORKING;
                checkInTime = s2.checkInTime();
            } else {
                // 전 구간 퇴근 완료 → 첫 출근 ~ 마지막 퇴근으로 표시.
                status = STATUS_OFF_WORK;
                checkInTime = s1In ? s1.checkInTime() : s2.checkInTime();
                checkOutTime = s2In ? s2.checkOutTime() : s1.checkOutTime();
            }
        }

        // PRAFTA_COM_003-A: 전일 미퇴근(열린 슬롯)을 status 의 WORKING 판정에 반영.
        //   기준일이 오늘(baseIsToday)이고, 당일 근태만으로 산출한 status 가 BEFORE_WORK(=당일 출근 없음)인데
        //   직전일(today-1) 미퇴근 열린 슬롯(prevDayOpen, CHECK_OUT NULL)이 남아 있으면 실제로는 "근무 중"이다.
        //   이 경우 status 를 WORKING 으로 보강해 안전점검/위험성 발굴 등 근무중 전용 기능 게이트가 열리도록 한다.
        //   - 오버나이트 경로(baseIsToday=false)는 prevDayOpen 이 null 이라 영향 없음(기존 hasOpen 경로 유지).
        //   - 당일 출퇴근 완료(OFF_WORK)/당일 근무중(WORKING)은 status 가 BEFORE_WORK 가 아니므로 무회귀.
        //   - 월마감 여부(prevDayClosedMonth)는 퇴근 가능 여부(canCheckOut)에만 관여하며, 실제 근무 상태(WORKING)와 무관하므로 여기서는 보지 않는다.
        //   - checkInTime/checkOutTime 등 표시 시각은 손대지 않는다(전날 출근시각은 기존 prevDayCheckInTime 으로 별도 안내).
        if (STATUS_BEFORE_WORK.equals(status) && prevDayOpen != null) {
            status = STATUS_WORKING;
        }

        // prafta-app-021 (§7.6): 전날 미퇴근 마감 대기 신호.
        //   오늘 진행 중(hasOpen) 케이스가 우선이므로, hasOpen 이 아닐 때만 전날 신호를 켠다(이중 표시 방지).
        //   예외(prafta-app-021-6): 직전일 미퇴근의 WORK_YMD 월이 이미 마감이면 퇴근이 월마감으로 막히므로
        //   퇴근 유도(prevDayCheckoutPending)를 켜지 않는다. attd01 출근 게이트 예외와 신호 일관성을 맞춰,
        //   출근 흐름이 정상 진행(게이트 예외로 출근 허용)되도록 둔다. 마감 판정 데이터소스는
        //   attd01.isMonthClosed 와 동일(부서단위 월마감, prafta-028 / TB_ATTD_CLOSE).
        boolean prevDayClosedMonth = prevDayOpen != null
                && isMonthClosed(query, prevDayOpen.workYmd().substring(0, 6));
        boolean prevDayCheckoutPending = !hasOpen && prevDayOpen != null && !prevDayClosedMonth;
        if (prevDayClosedMonth) {
            log.info("[home01] 직전일 미퇴근이 마감된 월이라 퇴근 유도 생략(보정 대상) — prafta-app-021-6 (prevDayWorkYmd={})", prevDayOpen.workYmd());
        }
        String prevDayCheckInTime = prevDayCheckoutPending ? prevDayOpen.checkInTime() : null;

        // 공통 액션 규칙(attd01 정합): 진행중 레코드 있으면 퇴근만, 없고 잔여 구간 있으면 출근만.
        //   prafta-app-021: 전날 미퇴근이 남아 있으면(prevDayCheckoutPending) 메인에서 전날 마감이 가능하도록 퇴근 버튼 활성화.
        //   canCheckOut = 오늘 진행 중(hasOpen) 우선, 없으면 전날 미퇴근(prevDayCheckoutPending).
        boolean canCheckOut = hasOpen || prevDayCheckoutPending;
        // canCheckIn 은 기존 유지(전날 미퇴근만으로 끄지 않음 — 서버 게이트 021-1 이 출근 차단). 프론트가 prevDayCheckoutPending 로 출근 버튼 비활성 표시.
        //   prafta-com-008-B: 자발 연차일 출근 "허용"으로 정책 전환(B-1) → 종일 연차일이어도 canCheckIn 을 끄지 않는다.
        //   (E 시절 '모든 연차일 출근 차단' 게이트를 lockstep 으로 완화) 촉진 확정 연차일의 차단은 서버 진입부
        //   guardAndRecord(checkIn) 가 ATTD_400_150 으로 강제(노무수령거부) — 버튼 게이트가 아니라 서버 가드가 단일출처.
        //   자발/촉진+휴일 연차일은 프론트가 isLeaveDay 로 출근 확인 팝업을 띄운 뒤 출근 진행. 퇴근(canCheckOut)은 기존 유지.
        boolean canCheckIn = !hasOpen && existing < maxSlots;

        // prafta-app-015: 2구간 스케줄 구간 선택 게이팅 플래그(attd01 SlotResponse 와 동일 의미).
        //   1구간/스케줄없음은 빈 리스트(MainView 는 단일 canCheckIn 버튼 유지).
        //   canCheckInThisSlot = isTwoSlot && 해당 구간 미등록 && 월미마감(!closed) && 하루 2회 미초과.
        //   진행 중(미퇴근) 구간 존재 시 "퇴근 우선" 게이팅은 프론트가 canCheckOut 으로 처리한다(attd01 today 카드와 동일).
        //   월마감 여부(!closed)는 attd01 buildSlot 의 canCheckInThisSlot(약 209행) 과 의미 정렬 — 마감 당일 버튼 활성 오노출 방지.
        boolean closed = isMonthClosed(query, query.todayYmd().substring(0, 6));
        java.util.List<HomeSummaryResponse.SlotFlag> slots = new java.util.ArrayList<>();
        if (isTwoSlot) {
            slots.add(HomeSummaryResponse.SlotFlag.builder()
                    .workSeq(1)
                    .alreadyCheckedIn(s1In)
                    .canCheckInThisSlot(!s1In && !s2In && !closed && existing < maxSlots) // PRAFTA-APP-024 후속: 2구간 등록 시 1구간 역순 출근 불가.
                    .build());
            slots.add(HomeSummaryResponse.SlotFlag.builder()
                    .workSeq(2)
                    .alreadyCheckedIn(s2In)
                    .canCheckInThisSlot(!s2In && !closed && existing < maxSlots)
                    .build());
        }

        return HomeSummaryResponse.Attendance.builder()
                .status(status)
                .scheduleExists(scheduleExists) // prafta-app-013-2: 기준일 스케줄 존재 여부.
                .scheduleStart(scheduleStart)
                .scheduleEnd(scheduleEnd)
                // PRAFTA-FIXEDOT-2(표기): 고정연장(전방·후방) — 연차일에는 스케줄과 동일하게 미노출.
                //   고정연장 없는 근무타입/스케줄 없는 날은 전부 null(기존 표기 불변).
                .preFixedOtStrTime(sch != null && !isLeaveDay ? sch.preFixedOtStrTime() : null)
                .preFixedOtEndTime(sch != null && !isLeaveDay ? sch.preFixedOtEndTime() : null)
                .fixedOtStrTime(sch != null && !isLeaveDay ? sch.fixedOtStrTime() : null)
                .fixedOtEndTime(sch != null && !isLeaveDay ? sch.fixedOtEndTime() : null)
                .checkInTime(checkInTime)
                .checkOutTime(checkOutTime)
                .isOffsite(isOffsite) // 오늘 최근 출근 GPS 행(01) 존재=외근 (prafta-app-003 B-1).
                .isLeaveDay(isLeaveDay) // prafta-com-008-E (H1): 기준일 종일 연차일 여부(출근 버튼 비활성/연차 표시 근거).
                .laborRefusal(laborRefusal) // 근태 E2E(F2): 촉진 노무수령거부 대상 연차일(true 면 출근 시 차단 안내).
                .canCheckIn(canCheckIn)
                .canCheckOut(canCheckOut)
                .prevDayCheckoutPending(prevDayCheckoutPending) // prafta-app-021 §7.6: 전날 미퇴근 마감 대기 신호.
                .prevDayCheckInTime(prevDayCheckInTime)         // prafta-app-021: 전날 출근시각 HHMM(카드 표기용).
                .isTwoSlot(isTwoSlot)   // prafta-app-015: 구간 선택 버튼 노출 판정.
                .slots(slots)           // prafta-app-015: 구간별 게이팅 플래그(2구간만).
                .build();
    }

    /**
     * prafta-app-015: 해당 월(closeYm, YYYYMM)이 본인 부서를 덮는 마감으로 닫혀 있는지.
     * <p>attd01 의 isMonthClosed 동일 로직(NODE_CD 미상이면 '*' 전체 마감만 판정).
     *   메인홈 구간 게이팅이 attd01 today 카드와 동일 의미를 갖도록 사용.
     */
    private boolean isMonthClosed(HomeSummaryQuery query, String closeYm) {
        String nodeCd = appHome01Mapper.selectUserNodeCd(query.cmpnyCd(), query.siteCd(), query.userCd());
        if (!StringUtils.hasText(nodeCd)) {
            // 소속부서 미상이면 '*'(전체 사업장) 마감만으로 판정.
            nodeCd = "*";
        }
        int covering = appHome01Mapper.countCoveringClose(query.cmpnyCd(), query.siteCd(), nodeCd, closeYm);
        return covering > 0;
    }

    /**
     * leave 영역 산출(법정+약정 합산). SUM 이 null 이면 0.0 폴백, 소수 1자리 반올림.
     */
    private HomeSummaryResponse.Leave buildLeave(HomeSummaryQuery query) {

        LeaveSummaryResult leave = appHome01Mapper.selectLeaveSummary(query);

        double granted = toScaledDouble(leave != null ? leave.grantedDays() : null);
        double remaining = toScaledDouble(leave != null ? leave.remainingDays() : null);

        // 연차 개편(표시): 신청형 휴가('01') 요약(보유 타입 수 + 총잔여) — GRANT 그룹(granted/remaining)과 분리.
        //   회계연도 경계는 단일출처 FiscalYearUtils 로 산출(연차 현황 화면 leave01 과 동일 경계 보장).
        FiscalYearUtils.FiscalWindow fiscal = resolveFiscalWindow(query.cmpnyCd());
        AppliedLeaveSummaryResult applied = appHome01Mapper.selectAppliedLeaveSummary(
                query.cmpnyCd(), query.userCd(),
                fiscal.fiscalStartYmd(), fiscal.fiscalEndYmdExclusive());

        int appliedTypeCount = (applied != null) ? applied.typeCount() : 0;
        double appliedRemaining = toScaledDouble(applied != null ? applied.totalRemainDays() : null);

        return HomeSummaryResponse.Leave.builder()
                .grantedDays(granted)
                .remainingDays(remaining)
                .appliedTypeCount(appliedTypeCount)
                .appliedRemainingDays(appliedRemaining)
                .build();
    }

    /**
     * 연차 개편(표시): 당해 회계연도 윈도우 산출(단일출처 {@link FiscalYearUtils}).
     * 활성정책 AXIS2_FISCAL_START_MM/_DD 로 산출하며, 정책 미존재/NULL 이면 1월 1일 폴백(유틸이 처리).
     * (leaveflow/leave01 의 resolveFiscalWindow 와 동일 — 신청 화면/연차 현황 잔여와 동일 경계 보장.)
     */
    private FiscalYearUtils.FiscalWindow resolveFiscalWindow(String cmpnyCd) {
        LeavePolicyVO policy = leavePolicyMapper.selectActivePolicy(cmpnyCd);
        String mm = (policy == null) ? null : policy.getAxis2FiscalStartMm();
        String dd = (policy == null) ? null : policy.getAxis2FiscalStartDd();
        return FiscalYearUtils.fiscalWindow(LocalDate.now(), mm, dd);
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

        // PRAFTA-FIXEDOT-2(지시서 지점 5): 전일 마지막 점유 구간이 후방 고정연장이면 그 종료가
        //   오버나이트 경계다. 환산은 FixedOtScheduleUtils 단일 출처(전일 anchor 분 — 익일 걸침이면
        //   1440 초과, 소정 wrap 타입의 후방(예: 소정 22~06 + 고정연장 06~08)도 익일 배치).
        //   후방 고정연장이 없으면(null) 기존 소정 기준 로직을 그대로 탄다(무회귀).
        Integer rearEnd = FixedOtScheduleUtils.rearFixedOtEnd(
                prevSch.fstSchStrTime(), prevSch.fstSchEndTime(),
                prevSch.secSchStrTime(), prevSch.secSchEndTime(),
                prevSch.fixedOtStrTime(), prevSch.fixedOtEndTime());
        if (rearEnd != null) {
            // 전일 anchor 기준 1440 초과 = 오늘 새벽에 끝나는 점유. 그 종료 전이면 전일 기준.
            if (rearEnd > 1440 && nowMinutes() < rearEnd - 1440) {
                return yesterdayYmd;
            }
            return todayYmd;
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

    /**
     * prafta-app-021: 직전일(today-1, YYYYMMDD) 산출. 파싱 실패 시 null(전날 신호 미사용).
     * resolveAttendanceBaseYmd 의 BASIC_ISO_DATE 해석과 동일.
     */
    private String prevDayOf(String todayYmd) {
        try {
            LocalDate today = LocalDate.parse(todayYmd, DateTimeFormatter.BASIC_ISO_DATE);
            return today.minusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE);
        } catch (Exception e) {
            log.warn("[home01] 직전일 산출 중 today 파싱 실패 today={} → 전날 미퇴근 신호 미사용", todayYmd);
            return null;
        }
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
