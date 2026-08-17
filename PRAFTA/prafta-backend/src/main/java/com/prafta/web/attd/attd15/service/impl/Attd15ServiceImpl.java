package com.prafta.web.attd.attd15.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.attd.util.FixedOtMinutesUtils;
import com.prafta.common.cmm.schedule.util.FixedOtScheduleUtils;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.attd.attd15.application.param.Weekly52hListsParam;
import com.prafta.web.attd.attd15.application.query.Weekly52hListsQuery;
import com.prafta.web.attd.attd15.dto.response.Weekly52hListsResponse;
import com.prafta.web.attd.attd15.mapper.Attd15Mapper;
import com.prafta.web.attd.attd15.result.ActualRowResult;
import com.prafta.web.attd.attd15.result.OvertimeSummaryResult;
import com.prafta.web.attd.attd15.result.ScheduledRowResult;
import com.prafta.web.attd.attd15.result.TargetUserResult;
import com.prafta.web.attd.attd15.result.TimeLeaveWindowResult;
import com.prafta.web.attd.attd15.result.Weekly52hListResult;
import com.prafta.web.attd.attd15.service.Attd15Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ATTD15-T1 - 주52시간 관리 서비스 구현.
 *
 * <p>계산 로직은 새로 만들지 않고 기존 자산을 이식한다(요청서 §4):
 * <ul>
 *   <li>등록된 스케줄 기준 합계 = {@code AppAttd01ServiceImpl.plannedMinutes} 계산식 포팅
 *       (1구간+2구간 근로분, 자정 넘김 +1440 보정).</li>
 *   <li>실제 근무 기준 합계 = {@code Attd11ServiceImpl.UserAccumulator.workMinutes} 계산식 포팅
 *       ((퇴근일시-출근일시)-스케줄휴게, 음수 0 클램프, 스케줄 클램프 없음) + otMinutes(COMPLETED 합).
 *       plan.md 확인필요 ① 채택안(Attd_11 방식) — 주52시간 판단은 실제 사업장 체류/근로 총량을
 *       보는 것이 안전측(under-count 방지)이라는 근거로 확정.</li>
 *   <li>마감(잠정치) 판정 = {@code AttdCloseService.isClosedForUser} 재사용. 주가 걸친 월(1~2개)
 *       중 하나라도 미마감이면 전체 잠정치(provisionalYn='Y') — §13.5 마감 커버리지 정의 재사용.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Attd15ServiceImpl implements Attd15Service {

    private final Attd15Mapper attd15Mapper;
    private final AttdCloseService attdCloseService;
    /** 사업장 접근 인가(공용 cmm 빈) — User_03 원장(TB_USER_SITE_AUTH) 기반 인가(신규 등식가드 금지). */
    private final com.prafta.common.cmm.siteauth.service.SiteAccessService siteAccessService;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /*
     * 상태 구간 경계(분). 사용자 결정(요청서 §2.2): 정상(~40h) / 주의(40h초과~48h) /
     * 위험(48h초과~52h) / 초과(52h초과) 3구간 경계.
     *
     * developer 노트(경계값 소속 판단, plan.md §7 확정 그대로 코드화):
     *   각 구간 상한을 "이하(&lt;=)"로 포함하는 하한 배타형을 택한다. 즉 정확히 40h(2400분)는
     *   아직 NORMAL 이고, 40h 를 1분이라도 초과한 시점부터 CAUTION 으로 올라간다. 마찬가지로
     *   정확히 48h/52h 도 각각 CAUTION/DANGER 에 포함되고, 그 상한을 초과해야 다음 구간으로 간다.
     *   이 화면의 목적(초과근로 위험을 사전에 놓치지 않는 것)상 "초과분이 실제로 발생한 시점부터"
     *   상위 위험 구간으로 표시하는 것이 안전측 판정이라 판단했다.
     */
    private static final int THRESHOLD_NORMAL_MAX = 40 * 60;   // 2400분
    private static final int THRESHOLD_CAUTION_MAX = 48 * 60;  // 2880분
    private static final int THRESHOLD_DANGER_MAX = 52 * 60;   // 3120분

    private static final String STATUS_NORMAL = "NORMAL";
    private static final String STATUS_CAUTION = "CAUTION";
    private static final String STATUS_DANGER = "DANGER";
    private static final String STATUS_EXCESS = "EXCESS";

    @Override
    public Weekly52hListsResponse getWeekly52hLists(Weekly52hListsParam param) {

        log.info("Attd_15 주52시간 관리 조회 진입 - weekStartYmd={}, weekEndYmd={}, siteCd={}, nodeCd={}, incSub={}",
                param.weekStartYmd(), param.weekEndYmd(), param.siteCd(), param.nodeCd(), param.incSubNodeYn());

        // 사업장 접근 인가(User_03 원장 기반 — 구 토큰 사업장 등식 가드 대체).
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(), param.gvSiteCd(), param.siteCd());

        // 권한 게이트 — master/hr 또는 노드 관리자만 허용(Attd11과 동일 원칙, PII 노출 화면).
        if (!attdCloseService.canManageNode(
                param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd(), param.nodeCd())) {
            log.warn("Attd_15 조회 권한 없음 - userCd={}, authCd={}, siteCd={}, nodeCd={}",
                    param.gvUserCd(), param.gvAuthCd(), param.siteCd(), param.nodeCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        Weekly52hListsQuery query = Weekly52hListsQuery.from(param);

        List<TargetUserResult> targetUsers = attd15Mapper.selectTargetUsers(query);
        List<ScheduledRowResult> schedRows = attd15Mapper.selectWeeklyScheduledRows(query);
        List<ActualRowResult> actualRows = attd15Mapper.selectWeeklyActualRows(query);
        List<OvertimeSummaryResult> otRows = attd15Mapper.selectWeeklyOvertimeSummary(query);

        // 사용자별 "등록된 스케줄 기준" 주간 합계(분) — AppAttd01ServiceImpl.plannedMinutes 포팅.
        Map<String, Long> scheduledMinutesByUser = new HashMap<>();
        for (ScheduledRowResult r : schedRows) {
            scheduledMinutesByUser.merge(r.userCd(), plannedMinutesOfRow(r), Long::sum);
        }

        // A안(2026-08-17): 사용자·일 단위 확정 "시각 보유" 연차(반차 01 + 시간차 02/03/04) 구간 맵.
        //   연차 사용 시간은 유급이되 근로시간이 아니므로(주52·연장 판정은 실근로 기준 — 고용부 해석),
        //   실제기준(raw) 합산에서 실근태와의 겹침만큼 차감한다. 시각 wrap(END<=START=익일)은 저장 규약.
        //   확정 구간은 겹침 가드(ATTD_400_112/HB-09)로 상호 배타지만 방어적으로 병합 후 사용한다.
        Map<String, List<int[]>> leaveSegsByUserDay = new HashMap<>();
        for (TimeLeaveWindowResult w : attd15Mapper.selectWeeklyTimeLeaveWindows(query)) {
            Integer s = FixedOtMinutesUtils.dayAnchorMinutes(w.workYmd(), w.workYmd(), w.startTime());
            Integer e = FixedOtMinutesUtils.dayAnchorMinutes(w.workYmd(), w.workYmd(), w.endTime());
            if (s == null || e == null) {
                continue; // 시각 비정상 행 — 차감 제외(추정 금지)
            }
            int end = (e <= s) ? e + 1440 : e;
            leaveSegsByUserDay
                    .computeIfAbsent(w.userCd() + "_" + w.workYmd(), k -> new ArrayList<>())
                    .add(new int[] { s, end });
        }
        leaveSegsByUserDay.replaceAll((k, v) -> mergeSegments(v));

        // 사용자별 "실제 근무 기준" 원시 합계(분) — Attd11ServiceImpl.UserAccumulator.workMinutes 포팅.
        //   A안: 슬롯별로 (raw − 휴게) − (확정 시각 연차 ∩ 실근태) 를 합산한다. 시간차는 휴게 가로지름
        //   금지(ATTD_400_055)라 휴게와 연차 차감이 이중으로 겹칠 수 없다.
        Map<String, Long> actualRawMinutesByUser = new HashMap<>();
        for (ActualRowResult r : actualRows) {
            String inDate = blankToNull(r.actInDate());
            String inTime = blankToNull(r.actInTime());
            String outDate = blankToNull(r.actOutDate());
            String outTime = blankToNull(r.actOutTime());
            String workYmd = blankToNull(r.workYmd());
            int breakMin = parseIntOrZero(r.planBreakMin());

            if (inTime != null && outTime != null && workYmd != null) {
                long inStamp = toMinuteStamp(inDate != null ? inDate : workYmd, inTime);
                long outStamp = toMinuteStamp(outDate != null ? outDate : workYmd, outTime);
                long worked = (outStamp - inStamp) - breakMin;
                List<int[]> leaveSegs = leaveSegsByUserDay.get(r.userCd() + "_" + workYmd);
                if (worked > 0 && leaveSegs != null && !leaveSegs.isEmpty()) {
                    int[] actSeg = FixedOtMinutesUtils.actualSegment(
                            FixedOtMinutesUtils.dayAnchorMinutes(workYmd, inDate, inTime),
                            FixedOtMinutesUtils.dayAnchorMinutes(workYmd, outDate, outTime));
                    if (actSeg != null) {
                        worked -= FixedOtMinutesUtils.coveredMinutes(leaveSegs, List.of(actSeg));
                    }
                }
                if (worked > 0) {
                    actualRawMinutesByUser.merge(r.userCd(), worked, Long::sum);
                }
            }
        }

        // PRAFTA-FIXEDOT-3(J2): 사용자별 고정연장 자동 계상 실적(분) 합 — 파생 계산(저장 없음, 정책 ①).
        //   슬롯(행) 단위 실근태 구간 ∩ 그날 고정연장 구간(FixedOtMinutesUtils 단일 출처, 일 anchor 프레임).
        //   ★ 총량(actualMinutes)에는 가산하지 않는다 — 고정연장 근무분은 raw 실근태에 이미 포함되어 있어
        //     여기 산출은 "연장 축 분류(actualOtMinutes)" 전용이다(이중 계상 금지).
        //   고정연장 없는 근무타입은 빈 구간 → 0(기존 화면 값 전부 불변).
        Map<String, Long> fixedOtMinutesByUser = new HashMap<>();
        for (ActualRowResult r : actualRows) {
            String workYmd = blankToNull(r.workYmd());
            if (workYmd == null) {
                continue;
            }
            int[] actSeg = FixedOtMinutesUtils.actualSegment(
                    FixedOtMinutesUtils.dayAnchorMinutes(workYmd, r.actInDate(), blankToNull(r.actInTime())),
                    FixedOtMinutesUtils.dayAnchorMinutes(workYmd, r.actOutDate(), blankToNull(r.actOutTime())));
            if (actSeg == null) {
                continue; // 미완결 슬롯(미출근/미퇴근)은 계상 대상 아님.
            }
            int covered = FixedOtMinutesUtils.dayFixedOtActualMinutes(
                    r.fstSchStrTime(), r.fstSchEndTime(), r.secSchStrTime(), r.secSchEndTime(),
                    r.preFixedOtStrTime(), r.preFixedOtEndTime(), r.fixedOtStrTime(), r.fixedOtEndTime(),
                    List.of(actSeg));
            if (covered > 0) {
                fixedOtMinutesByUser.merge(r.userCd(), (long) covered, Long::sum);
            }
        }

        // 사용자별 초과근무 분 합(COMPLETED만, decisions §3-4 동일 기준).
        Map<String, Long> otMinutesByUser = new HashMap<>();
        for (OvertimeSummaryResult ot : otRows) {
            otMinutesByUser.put(ot.userCd(), ot.otMinutes());
        }

        // 마감(잠정치) 판정 대상 월(1~2개, 주가 월 경계를 넘으면 2개).
        String startYm = param.weekStartYmd().substring(0, 6);
        String endYm = param.weekEndYmd().substring(0, 6);

        List<Weekly52hListResult> resultList = new ArrayList<>(targetUsers.size());
        for (TargetUserResult u : targetUsers) {
            long scheduledMinutes = scheduledMinutesByUser.getOrDefault(u.userCd(), 0L);
            // 2026-08-17: 실제 근무 기준 = 원시 실근로(raw)만 — 승인 OT 가산 제거(이중 계상 수정).
            //   OT 는 등록 가드(ATTD_400_104)상 항상 실근태 구간 안에서만 생성되므로 raw 에 이미
            //   포함되어 있다(운영 실발생: raw 21:42 + OT 3:24 = 25:06 과대 표시). 고정연장 J2 규약
            //   ("raw 포함분은 총량에 가산 금지")과 동일 원리. OT·고정연장 분류는 연장 축(actualOtMinutes)이 표시.
            long actualMinutes = actualRawMinutesByUser.getOrDefault(u.userCd(), 0L);
            // PRAFTA-FIXEDOT-3(J2): 연장 축 분류 = 승인 OT + 고정연장 자동 계상 실적(구간 배타 — 중복 없음).
            long actualOtMinutes = otMinutesByUser.getOrDefault(u.userCd(), 0L)
                    + fixedOtMinutesByUser.getOrDefault(u.userCd(), 0L);

            // 하나라도 미마감이면 전체 잠정치(§13.5 마감 커버리지 정의 재사용).
            boolean startClosed = attdCloseService.isClosedForUser(param.gvCmpnyCd(), param.siteCd(), u.userCd(), startYm);
            boolean endClosed = startYm.equals(endYm)
                    || attdCloseService.isClosedForUser(param.gvCmpnyCd(), param.siteCd(), u.userCd(), endYm);
            boolean allMonthsClosed = startClosed && endClosed;

            resultList.add(new Weekly52hListResult(
                    u.siteNm()
                    , u.deptNm()
                    , u.userCd()
                    , u.userId()
                    , u.userNm()
                    , param.weekStartYmd()
                    , param.weekEndYmd()
                    , scheduledMinutes
                    , classifyStatus(scheduledMinutes)
                    , actualMinutes
                    , classifyStatus(actualMinutes)
                    , allMonthsClosed ? "N" : "Y"
                    , actualOtMinutes
            ));
        }

        // 사용자 결정 §2.5: userCd 오름차순 정렬(Attd11 관례 동일).
        resultList.sort(Comparator.comparing(Weekly52hListResult::userCd));

        log.info("Attd_15 주52시간 관리 조회 종료 - 사용자 {}명", resultList.size());

        return Weekly52hListsResponse.builder()
                .weekly52hListsResultList(resultList)
                .build();
    }

    /**
     * 그날 소정근로분(1구간 + (2구간)) — AppAttd01ServiceImpl.plannedMinutes 동일 계산식.
     *
     * <p>HB-07(D3 / Q4 확정 2026-08-07): 여기서 <b>그날 확정 연차 면제분</b>을 뺀다.
     * <pre>일자별 소정 = max(0, planned - 그날 확정 연차 면제분 합계)</pre>
     * 종일 연차일은 면제분 = planned 이므로 0 이 된다. Attd_05 가 이미 종일 연차일 셀을 "연차"로
     * 대체 표시하므로(prafta-com-008-E-6) 표시 모델과도 정합한다.
     * 반차·시간차일은 면제분(LEAVE_MINUTES 합)만 빠지므로 스케줄은 살아 있는 것으로 보인다.
     *
     * <p>회귀 금지: "실제 근무 기준" 합계(actualRawMinutesByUser + OT)와 잠정치(마감) 판정은 불변.
     */
    private long plannedMinutesOfRow(ScheduledRowResult r) {
        long total = 0;
        Integer m1 = computeWorkMinutes(r.fstSchStrTime(), r.fstSchEndTime(), parseIntOrZero(r.fstSchBrkMin()));
        if (m1 != null) {
            total += m1;
        }
        boolean isTwoSlot = blankToNull(r.secSchStrTime()) != null;
        if (isTwoSlot) {
            Integer m2 = computeWorkMinutes(r.secSchStrTime(), r.secSchEndTime(), parseIntOrZero(r.secSchBrkMin()));
            if (m2 != null) {
                total += m2;
            }
        }

        // PRAFTA-FIXEDOT-2(지시서 지점 4): 예정 근로에 고정연장 분 편입 — 마감 예측(잠정치) 정합.
        //   환산은 FixedOtScheduleUtils 단일 출처(휴게 없음 — 1단계 정책상 고정연장 휴게 컬럼 미도입).
        //   고정연장 없는 근무타입은 0 → 종전 잠정치와 완전 동일(무회귀).
        //   종일 연차일(아래 'Y' 분기)은 고정연장 의무 면제(정책 ③)까지 포함해 그날 예정 0 유지.
        total += FixedOtScheduleUtils.totalFixedOtMinutes(
                r.fstSchStrTime(), r.fstSchEndTime(), r.secSchStrTime(), r.secSchEndTime(),
                r.preFixedOtStrTime(), r.preFixedOtEndTime(), r.fixedOtStrTime(), r.fixedOtEndTime());

        if ("Y".equals(r.fullDayLeaveYn())) {
            // 종일 연차일: 면제분 = 그날 소정 전량(고정연장 의무도 면제 — 정책 ③) → 예정 0.
            return 0L;
        }
        long exempt = Math.max(0, r.leaveExemptMinutes());
        return Math.max(0L, total - exempt);
    }

    /** HHMM -> 자정 기준 분. 잘못된 값이면 null. (AppAttd01ServiceImpl.toMinutes 동일) */
    private Integer toMinutes(String hhmm) {
        if (hhmm == null || !hhmm.matches("\\d{4}")) {
            return null;
        }
        int h = Integer.parseInt(hhmm.substring(0, 2));
        int m = Integer.parseInt(hhmm.substring(2, 4));
        return h * 60 + m;
    }

    /**
     * (종료-시작-휴게) 근로분. 자정 넘김(종료&lt;시작)이면 +1440 보정.
     * (AppAttd01ServiceImpl.computeWorkMinutes 동일 — 스케줄 예정분 계산용)
     */
    private Integer computeWorkMinutes(String startHhmm, String endHhmm, Integer breakMin) {
        Integer s = toMinutes(startHhmm);
        Integer e = toMinutes(endHhmm);
        if (s == null || e == null) {
            return null;
        }
        int span = e - s;
        if (span < 0) {
            span += 1440; // 야간 자정 넘김.
        }
        int net = span - (breakMin == null ? 0 : breakMin);
        return Math.max(net, 0);
    }

    /**
     * A안(2026-08-17): 구간 목록 병합(시작 오름차순 정렬 후 겹침/인접 통합).
     * 확정 연차 구간은 가드로 상호 배타지만, 겹치는 입력이 와도 이중 차감이 없도록 방어한다.
     */
    private static List<int[]> mergeSegments(List<int[]> segs) {
        if (segs == null || segs.size() <= 1) {
            return segs;
        }
        List<int[]> sorted = new ArrayList<>(segs);
        sorted.sort(Comparator.comparingInt(a -> a[0]));
        List<int[]> merged = new ArrayList<>();
        int[] cur = sorted.get(0).clone();
        for (int i = 1; i < sorted.size(); i++) {
            int[] next = sorted.get(i);
            if (next[0] <= cur[1]) {
                cur[1] = Math.max(cur[1], next[1]);
            } else {
                merged.add(cur);
                cur = next.clone();
            }
        }
        merged.add(cur);
        return merged;
    }

    /** 일자(YYYYMMDD) + 시각(HHmm) 을 1970-01-01 기준 통합 분(minute) stamp 로 환산(Attd11ServiceImpl 동일). */
    private long toMinuteStamp(String ymd, String hhmm) {
        LocalDate d = LocalDate.parse(ymd, YMD);
        long epochDays = d.toEpochDay();
        int hh = Integer.parseInt(hhmm.substring(0, 2));
        int mm = Integer.parseInt(hhmm.substring(2, 4));
        return epochDays * 1440L + (long) hh * 60L + mm;
    }

    private String blankToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    private int parseIntOrZero(String s) {
        if (s == null || s.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 상태 판정(정상/주의/위험/초과) — 클래스 상단 developer 노트(경계값 소속) 참조. */
    private String classifyStatus(long minutes) {
        if (minutes <= THRESHOLD_NORMAL_MAX) {
            return STATUS_NORMAL;
        }
        if (minutes <= THRESHOLD_CAUTION_MAX) {
            return STATUS_CAUTION;
        }
        if (minutes <= THRESHOLD_DANGER_MAX) {
            return STATUS_DANGER;
        }
        return STATUS_EXCESS;
    }
}
