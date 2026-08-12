package com.prafta.web.attd.attd11.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.attd.util.FixedOtMinutesUtils;
import com.prafta.common.cmm.leave.util.PartialLeaveWindowUtils;
import com.prafta.common.cmm.schedule.util.FixedOtScheduleUtils;
import com.prafta.common.cmm.leave.util.ScheduleWorkMinutesUtils;
import com.prafta.common.cmm.leave.vo.DailyScheduleVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.attd.attd11.application.param.MonthlyAttdSummaryParam;
import com.prafta.web.attd.attd11.application.query.MonthlyAttdSummaryQuery;
import com.prafta.web.attd.attd11.dto.response.MonthlyAttdSummaryResponse;
import com.prafta.web.attd.attd11.mapper.Attd11Mapper;
import com.prafta.web.attd.attd11.result.AbsentDayRowResult;
import com.prafta.web.attd.attd11.result.AttdSummaryRowResult;
import com.prafta.web.attd.attd11.result.HalfLeaveWindowResult;
import com.prafta.web.attd.attd11.result.MonthlyAttdSummaryResult;
import com.prafta.web.attd.attd11.result.OvertimeSummaryResult;
import com.prafta.web.attd.attd11.service.Attd11Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * PRAFTA-034 - Attd_11 월별 사용자 근태 판정 서비스 구현.
 *
 * 매퍼가 내려준 (USER_CD, WORK_YMD, WORK_SEQ) 원시 근태 행을 일시(YYYYMMDDHHmm)
 * 기준으로 행별 판정(지각/조퇴/근무시간) 후 사용자(USER_CD) 단위로 집계한다.
 * 야간(스케줄 종료시각 &lt; 시작시각)은 종료를 익일로 보정한다 — Attd_08 computeStatus
 * 와 동일 기준. 초과근무 분은 매퍼 SUM 결과를 사용자별로 병합한다(decisions §4).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Attd11ServiceImpl implements Attd11Service {

    private final Attd11Mapper attd11Mapper;
    private final AttdCloseService attdCloseService;
    /** 사업장 접근 인가(공용 cmm 빈) — 토큰 사업장 등식 대신 User_03 원장(TB_USER_SITE_AUTH) 기반 인가. */
    private final com.prafta.common.cmm.siteauth.service.SiteAccessService siteAccessService;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public MonthlyAttdSummaryResponse getMonthlyAttdSummary(MonthlyAttdSummaryParam param) {

        log.info("Attd_11 월별 사용자 근태 판정 조회 진입 - workYm={}, siteCd={}, nodeCd={}, incSub={}",
                param.workYm(), param.siteCd(), param.nodeCd(), param.incSubNodeYn());

        // 사업장 접근 인가(구 토큰 사업장 등식 가드 대체 — User_03 원장 기반).
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());

        // 권한 게이트 (PRAFTA-028 / decisions §7) — master/hr 또는 노드 관리자만 허용.
        //   비 master/hr 이 nodeCd 미지정(=사업장 전체)이거나 관리 권한 없는 부서를 조회하면 차단.
        //   프론트 가드는 우회 가능하므로 서버에서 강제한다(PII 노출 화면).
        if (!attdCloseService.canManageNode(
                param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd(), param.nodeCd())) {
            log.warn("Attd_11 조회 권한 없음 - userCd={}, authCd={}, siteCd={}, nodeCd={}",
                    param.gvUserCd(), param.gvAuthCd(), param.siteCd(), param.nodeCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        MonthlyAttdSummaryQuery query = MonthlyAttdSummaryQuery.from(param);

        List<AttdSummaryRowResult> rows = attd11Mapper.selectAttdSummaryRows(query);
        List<OvertimeSummaryResult> otRows = attd11Mapper.selectOvertimeSummary(query);

        // HB-05(D1)/D-3: 그날 확정 반차 면제 구간을 (사용자, 근무일) 단위로 모은다.
        //   ★ 반차는 하루 2건(시작기준 0.5 + 종료기준 0.5 = 1.0)이 성립하므로 단건 가정(MIN 집계) 금지 —
        //     여러 건을 순차 적용해 합집합으로 판정한다(종일 쉰 사람에게 지각·조퇴가 뜨던 결함).
        Map<String, List<PartialLeaveWindowUtils.LeaveWindow>> leaveByUserYmd = new HashMap<>();
        for (HalfLeaveWindowResult w : attd11Mapper.selectHalfLeaveWindows(query)) {
            if (w.userCd() == null || w.workYmd() == null) {
                continue;
            }
            leaveByUserYmd.computeIfAbsent(w.userCd() + "|" + w.workYmd(), k -> new ArrayList<>())
                    .add(new PartialLeaveWindowUtils.LeaveWindow(w.startTime(), w.endTime()));
        }

        // 사용자별 초과근무 분 합 (decisions §3-4 : COMPLETED 만)
        Map<String, Long> otMinutesByUser = new HashMap<>();
        for (OvertimeSummaryResult ot : otRows) {
            otMinutesByUser.put(ot.userCd(), ot.otMinutes());
        }

        // 사용자별 결근 계상 (COM-016-F 8-3 + HB-06 : 스케줄 있으나 미출근, 휴일·종일연차·미래일 제외)
        //   ★ HB-06(2026-08-07 확정): 결근을 "일수"가 아니라 "분"으로 계상한다.
        //     결근분 = max(0, 그날 소정근로분 D - 그날 확정 부분연차 면제분). 출근기록이 있는 날은
        //     쿼리 단계에서 제외되므로 여기 오지 않는다(지각·조퇴 지표와 중복 계상 방지 — 현행 규칙 유지).
        //     D 는 ScheduleWorkMinutesUtils 로 계산해 연차 차감·주52·반차 경계와 산식을 공유한다.
        //   기존 결과 행 집합/모수는 변경하지 않고 매핑만 한다(보수적/무회귀 — 결근만 있는
        //   사용자를 새 행으로 노출하지 않음. 출근/초과근무 기록이 있는 기존 행에만 주입).
        Map<String, AbsentAccumulator> absentByUser = new HashMap<>();
        for (AbsentDayRowResult ab : attd11Mapper.selectAbsentDayRows(query)) {
            Integer daily = ScheduleWorkMinutesUtils.dailyStdWorkMinutes(toScheduleVO(ab));
            if (daily == null || daily <= 0) {
                // 스케줄 시각이 비정상이면 그날은 계상하지 않는다(추정 금지).
                log.warn("Attd_11 결근 계상 skip - 소정근로분 산출 불가: userCd={}, workYmd={}",
                        ab.userCd(), ab.workYmd());
                continue;
            }
            int absentMin = Math.max(0, daily - Math.max(0, ab.leaveExemptMinutes()));
            if (absentMin <= 0) {
                continue; // 면제분이 소정 전량을 덮은 날(시간차 합계 = D 등) → 결근 아님
            }
            absentByUser.computeIfAbsent(ab.userCd(), k -> new AbsentAccumulator())
                    .add(absentMin, daily);
        }

        // 사용자별 누적기(등장 순서 유지 — 매퍼가 USER_CD, WORK_YMD, WORK_SEQ 순 정렬)
        Map<String, UserAccumulator> accByUser = new LinkedHashMap<>();

        // PRAFTA-FIXEDOT-3: 일 단위 파생(실적·미이행)용 컨텍스트 — 같은 (사용자, 근무일)의 슬롯 행을 모은다.
        Map<String, FixedOtDayCtx> fixedOtDayByKey = new LinkedHashMap<>();

        for (AttdSummaryRowResult r : rows) {
            UserAccumulator acc = accByUser.computeIfAbsent(r.userCd(), k -> new UserAccumulator(r));

            // PRAFTA-FIXEDOT-3: 고정연장 보유 근무타입의 슬롯 실근태를 일 단위로 수집(판정과 무관 — additive).
            if (r.workYmd() != null && (blankToNull(r.fixedOtStrTime()) != null
                    || blankToNull(r.preFixedOtStrTime()) != null)) {
                fixedOtDayByKey.computeIfAbsent(r.userCd() + "|" + r.workYmd(), k -> new FixedOtDayCtx(r))
                        .addSlot(r);
            }

            String inDate = blankToNull(r.actInDate());
            String inTime = blankToNull(r.actInTime());
            String outDate = blankToNull(r.actOutDate());
            String outTime = blankToNull(r.actOutTime());
            String workYmd = blankToNull(r.workYmd());
            int breakMin = r.planBreakMin() == null ? 0 : r.planBreakMin();

            // HB-05(D1): 그날 확정 반차가 있으면 판정 기준을 "반차 반영 유효 소정 구간"으로 치환한다
            //   (정책 §10.1.1). 시작기준 반차 → planStart = 경계 / 종료기준 반차 → planEnd = 경계.
            //   구간 전체가 면제되면(반차 2건으로 종일 면제·2구간 중 한 구간 통째 면제) 그 구간은 판정 제외.
            //   시각 없는 구 반차·시간차·무연차일은 resolveAll 이 원값을 그대로 돌려주므로 판정 불변.
            //   ★ 판정용 시각의 일자 프레임은 "원 스케줄"(rawStart/rawEnd)로 잡는다 — 야간 스케줄에서
            //     스케줄 시작보다 이른 시각은 익일이다. 유효 시각끼리 비교하는 종전 규칙은 야간
            //     시작기준 반차(판정용 시작 01:15)를 당일로 두어 지각으로 오판정한다(Attd_08 동일 규칙).
            String rawStart = blankToNull(r.planStart());
            String rawEnd = blankToNull(r.planEnd());
            String planStart = rawStart;
            String planEnd = rawEnd;
            List<PartialLeaveWindowUtils.LeaveWindow> leaves =
                    leaveByUserYmd.get(r.userCd() + "|" + r.workYmd());
            if (leaves != null && !leaves.isEmpty() && planStart != null && planEnd != null) {
                PartialLeaveWindowUtils.EffectiveWorkWindow eff =
                        PartialLeaveWindowUtils.resolveAll(planStart, planEnd, leaves);
                planStart = eff.fullyExempt() ? null : blankToNull(eff.planStart());
                planEnd = eff.fullyExempt() ? null : blankToNull(eff.planEnd());
            }

            // 근무일수: 출근 기록(CHECK_IN_TIME)이 존재하는 distinct WORK_YMD (차수 무관)
            if (inTime != null && workYmd != null) {
                acc.workDays.add(workYmd);
            }

            // 총 근무시간(분): 출근·퇴근 모두 존재하는 행 — (퇴근일시 - 출근일시) - 휴게, 음수 0
            if (inTime != null && outTime != null && workYmd != null) {
                long inStamp = toMinuteStamp(inDate != null ? inDate : workYmd, inTime);
                long outStamp = toMinuteStamp(outDate != null ? outDate : workYmd, outTime);
                long worked = (outStamp - inStamp) - breakMin;
                if (worked > 0) {
                    acc.workMinutes += worked;
                }
            }

            // 지각: 출근 기록 + 스케줄 시작 존재 시, 실제출근일시 > 유효 소정 시작 일시
            if (inTime != null && planStart != null && workYmd != null) {
                long schStartStamp = toMinuteStamp(
                        shiftYmd(workYmd, rawStart, rawEnd, planStart), planStart);
                long actInStamp = toMinuteStamp(inDate != null ? inDate : workYmd, inTime);
                if (actInStamp > schStartStamp) {
                    acc.lateCnt += 1;
                    acc.lateMinutes += (actInStamp - schStartStamp);
                }
            }

            // 조퇴: 퇴근 기록 + 스케줄 종료 존재 시, 실제퇴근일시 < 유효 소정 종료 일시
            //   야간(원 스케줄 종료 < 시작)이면 스케줄 시작보다 이른 시각은 근무일자 익일로 본다
            //   (Attd_08 Attd08ServiceImpl.resolveStatus 와 동일 규칙).
            if (outTime != null && planEnd != null && workYmd != null) {
                long schEndStamp = toMinuteStamp(
                        shiftYmd(workYmd, rawStart, rawEnd, planEnd), planEnd);
                long actOutStamp = toMinuteStamp(outDate != null ? outDate : workYmd, outTime);
                if (actOutStamp < schEndStamp) {
                    acc.earlyLeaveCnt += 1;
                    acc.earlyLeaveMinutes += (schEndStamp - actOutStamp);
                }
            }
        }

        // PRAFTA-FIXEDOT-3: 일 단위 파생(실적 합 + "연장 미이행" 카운트)을 사용자 누적기에 반영한다.
        //   정책 ①: 실적 = 실근태 슬롯 구간 ∩ 고정연장 구간(연차 계열 사용일에도 실적은 계상).
        //   정책 ②③: 미이행 = 후방 존재 + 마지막 스케줄 슬롯 퇴근 완료 + 퇴근 < 후방 종료 + 연차 계열 미사용.
        //   지각/조퇴/결근 지표는 위 루프에서 이미 확정 — 여기서는 신규 축만 더한다(완전 분리).
        for (FixedOtDayCtx day : fixedOtDayByKey.values()) {
            UserAccumulator acc = accByUser.get(day.userCd);
            if (acc == null) {
                continue;
            }
            List<int[]> fixedOtSegs = FixedOtScheduleUtils.fixedOtSegments(
                    day.fstSchStrTime, day.fstSchEndTime, day.secSchStrTime, day.secSchEndTime,
                    day.preFixedOtStrTime, day.preFixedOtEndTime, day.fixedOtStrTime, day.fixedOtEndTime);
            if (fixedOtSegs.isEmpty()) {
                continue;
            }
            acc.fixedOtMinutes += FixedOtMinutesUtils.coveredMinutes(fixedOtSegs, day.actualSegs);

            boolean unmet = !"Y".equals(day.fixedOtExemptYn)
                    && FixedOtMinutesUtils.isRearUnfulfilled(
                            FixedOtMinutesUtils.rearFixedOtEndAnchor(
                                    day.fstSchStrTime, day.fstSchEndTime, day.secSchStrTime, day.secSchEndTime,
                                    day.fixedOtStrTime, day.fixedOtEndTime),
                            day.lastSlotOutAnchor());
            if (unmet) {
                acc.fixedOtUnmetCnt += 1;
            }
        }

        List<MonthlyAttdSummaryResult> resultList = new ArrayList<>(accByUser.size() + otRows.size());
        for (Map.Entry<String, UserAccumulator> e : accByUser.entrySet()) {
            UserAccumulator acc = e.getValue();
            long otMinutes = otMinutesByUser.getOrDefault(e.getKey(), 0L);
            resultList.add(new MonthlyAttdSummaryResult(
                    acc.userCd
                    , acc.userId
                    , acc.userNm
                    , acc.deptNm
                    , acc.authCd
                    , acc.authNm
                    , acc.workDays.size()
                    , acc.workMinutes
                    , otMinutes
                    , acc.lateCnt
                    , acc.lateMinutes
                    , acc.earlyLeaveCnt
                    , acc.earlyLeaveMinutes
                    , absentDayCntOf(absentByUser.get(e.getKey()))
                    , absentMinutesOf(absentByUser.get(e.getKey()))
                    , absentDaysOf(absentByUser.get(e.getKey()))
                    , acc.fixedOtMinutes
                    , acc.fixedOtUnmetCnt
            ));
        }

        // 정규 출근기록이 없고 COMPLETED 초과근무만 있는 사용자(휴일 초과근무 등)도 1행으로 노출.
        //   accByUser 에 없는 OT 사용자만 근태지표 0 + otMinutes 로 추가한다.
        for (OvertimeSummaryResult ot : otRows) {
            if (accByUser.containsKey(ot.userCd())) {
                continue;
            }
            resultList.add(new MonthlyAttdSummaryResult(
                    ot.userCd()
                    , ot.userId()
                    , ot.userNm()
                    , ot.deptNm()
                    , ot.authCd()
                    , ot.authNm()
                    , 0
                    , 0L
                    , ot.otMinutes()
                    , 0
                    , 0L
                    , 0
                    , 0L
                    , absentDayCntOf(absentByUser.get(ot.userCd()))
                    , absentMinutesOf(absentByUser.get(ot.userCd()))
                    , absentDaysOf(absentByUser.get(ot.userCd()))
                    // PRAFTA-FIXEDOT-3: OT-only 사용자(정규 출근기록 없음)는 고정연장 실적/미이행 산출 불가 — 0.
                    , 0L
                    , 0
            ));
        }

        // USER_CD 오름차순 정렬 (출근기록 사용자 + OT-only 사용자 통합)
        resultList.sort(Comparator.comparing(MonthlyAttdSummaryResult::userCd));

        log.info("Attd_11 월별 사용자 근태 판정 조회 종료 - 사용자 {}명", resultList.size());

        return MonthlyAttdSummaryResponse.builder()
                .monthlyAttdSummaryResultList(resultList)
                .build();
    }

    /** 일자(YYYYMMDD) + 시각(HHmm) 을 1970-01-01 기준 통합 분(minute) stamp 로 환산. */
    private long toMinuteStamp(String ymd, String hhmm) {
        LocalDate d = LocalDate.parse(ymd, YMD);
        long epochDays = d.toEpochDay();
        int hh = Integer.parseInt(hhmm.substring(0, 2));
        int mm = Integer.parseInt(hhmm.substring(2, 4));
        return epochDays * 1440L + (long) hh * 60L + mm;
    }

    /** YYYYMMDD 에 days 일을 더한 YYYYMMDD 반환 (Attd_08 ymdPlusDays 와 동일). */
    private String ymdPlusDays(String ymd, int days) {
        return LocalDate.parse(ymd, YMD).plusDays(days).format(YMD);
    }

    /** 판정용 시각이 속한 일자(근무일 또는 익일) — 원 스케줄 프레임 기준(Attd_08 동일 규칙). */
    private String shiftYmd(String workYmd, String rawStart, String rawEnd, String hhmm) {
        int offset = PartialLeaveWindowUtils.dayOffsetOf(rawStart, rawEnd, hhmm);
        return (offset == 0) ? workYmd : ymdPlusDays(workYmd, offset);
    }

    private String blankToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    /** HB-06: 결근 원시 행 → 소정근로분 계산 입력 VO(ScheduleWorkMinutesUtils 단일 출처 재사용). */
    private DailyScheduleVO toScheduleVO(AbsentDayRowResult r) {
        DailyScheduleVO vo = new DailyScheduleVO();
        vo.setFstSchStrTime(r.fstSchStrTime());
        vo.setFstSchEndTime(r.fstSchEndTime());
        vo.setFstSchBrkMin(r.fstSchBrkMin());
        vo.setSecSchStrTime(r.secSchStrTime());
        vo.setSecSchEndTime(r.secSchEndTime());
        vo.setSecSchBrkMin(r.secSchBrkMin());
        return vo;
    }

    private int absentDayCntOf(AbsentAccumulator acc) {
        return (acc == null) ? 0 : acc.dayCnt;
    }

    private long absentMinutesOf(AbsentAccumulator acc) {
        return (acc == null) ? 0L : acc.minutes;
    }

    /** HB-06: 결근 일수 = Σ(그날 결근분 / 그날 D). 소수 1자리 반올림(연차 표기 관례와 동일). */
    private double absentDaysOf(AbsentAccumulator acc) {
        if (acc == null) {
            return 0d;
        }
        return Math.round(acc.days * 10d) / 10d;
    }

    /** HB-06: 사용자별 결근 누적기(분 + 일수 환산). */
    private static final class AbsentAccumulator {
        int dayCnt = 0;
        long minutes = 0L;
        double days = 0d;

        void add(int absentMinutes, int dailyStdMinutes) {
            dayCnt += 1;
            minutes += absentMinutes;
            days += (double) absentMinutes / (double) dailyStdMinutes;
        }
    }

    /** 사용자 단위 집계 누적기. */
    private static final class UserAccumulator {
        final String userCd;
        final String userId;
        final String userNm;
        final String deptNm;
        final String authCd;
        final String authNm;

        final Set<String> workDays = new HashSet<>();
        long workMinutes = 0L;
        int lateCnt = 0;
        long lateMinutes = 0L;
        int earlyLeaveCnt = 0;
        long earlyLeaveMinutes = 0L;
        // PRAFTA-FIXEDOT-3: 고정연장 실적 합(분) + "연장 미이행" 일수 — 기존 지표와 완전 분리(additive).
        long fixedOtMinutes = 0L;
        int fixedOtUnmetCnt = 0;

        UserAccumulator(AttdSummaryRowResult r) {
            this.userCd = r.userCd();
            this.userId = r.userId();
            this.userNm = r.userNm();
            this.deptNm = r.deptNm();
            this.authCd = r.authCd();
            this.authNm = r.authNm();
        }
    }

    /**
     * PRAFTA-FIXEDOT-3: (사용자, 근무일) 단위 고정연장 파생 계산 컨텍스트.
     * 스케줄/고정연장/면제 컬럼은 그날 모든 슬롯 행에서 동일하므로 첫 행 값을 쓰고,
     * 슬롯별 실근태 구간과 "마지막 스케줄 슬롯"의 퇴근 스탬프만 행마다 수집한다.
     */
    private final class FixedOtDayCtx {
        final String userCd;
        final String workYmd;
        final String fstSchStrTime;
        final String fstSchEndTime;
        final String secSchStrTime;
        final String secSchEndTime;
        final String preFixedOtStrTime;
        final String preFixedOtEndTime;
        final String fixedOtStrTime;
        final String fixedOtEndTime;
        final String fixedOtExemptYn;
        final List<int[]> actualSegs = new ArrayList<>(2);
        private Integer lastSlotOutAnchor;

        FixedOtDayCtx(AttdSummaryRowResult r) {
            this.userCd = r.userCd();
            this.workYmd = r.workYmd();
            this.fstSchStrTime = r.fstSchStrTime();
            this.fstSchEndTime = r.fstSchEndTime();
            this.secSchStrTime = r.secSchStrTime();
            this.secSchEndTime = r.secSchEndTime();
            this.preFixedOtStrTime = r.preFixedOtStrTime();
            this.preFixedOtEndTime = r.preFixedOtEndTime();
            this.fixedOtStrTime = r.fixedOtStrTime();
            this.fixedOtEndTime = r.fixedOtEndTime();
            this.fixedOtExemptYn = r.fixedOtExemptYn();
        }

        void addSlot(AttdSummaryRowResult r) {
            int[] seg = FixedOtMinutesUtils.actualSegment(
                    FixedOtMinutesUtils.dayAnchorMinutes(workYmd, r.actInDate(), blankToNull(r.actInTime())),
                    FixedOtMinutesUtils.dayAnchorMinutes(workYmd, r.actOutDate(), blankToNull(r.actOutTime())));
            if (seg != null) {
                actualSegs.add(seg);
            }
            // 마지막 스케줄 슬롯(2구간 스케줄이면 seq2, 아니면 seq1)의 퇴근 스탬프 — 미이행 판정 기준.
            int lastSlotSeq = (blankToNull(secSchStrTime) != null) ? 2 : 1;
            if (r.workSeq() == lastSlotSeq) {
                lastSlotOutAnchor = FixedOtMinutesUtils.dayAnchorMinutes(
                        workYmd, r.actOutDate(), blankToNull(r.actOutTime()));
            }
        }

        /** 마지막 스케줄 슬롯 퇴근 스탬프(일 anchor 분). 그 슬롯 행 부재/미퇴근이면 null(배지 비대상). */
        Integer lastSlotOutAnchor() {
            return lastSlotOutAnchor;
        }
    }
}
