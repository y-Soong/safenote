package com.prafta.common.cmm.schedule.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.schedule.mapper.ScheduleGuardMapper;
import com.prafta.common.cmm.schedule.mapper.result.SchWindowResult;
import com.prafta.common.cmm.schedule.service.ScheduleOverlapGuardService;
import com.prafta.common.util.DateTimeUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ScheduleOverlapGuardService} 구현.
 *
 * <p>핵심 아이디어: 대상일(D) 00:00 을 원점(0분)으로 하는 단일 분(minute) 타임라인에
 * D / D-1 / D+1 의 근무 구간을 모두 올려놓고 교집합을 본다.
 * <ul>
 *   <li>각 구간은 자기 날짜 자정 기준 분 [s, e] 로 환산하되, 종료가 시작 이하이면 자정 넘김으로 보고
 *       +1440 한다(attd07 {@code AttdScheduleUtils.buildPlanSegment} 와 동일 규약).</li>
 *   <li>그 뒤 (이웃일 − 대상일) 일수만큼 *1440 오프셋을 더해 같은 타임라인으로 맞춘다.
 *       D-1 의 오버나이트 구간은 양수 영역(=D 의 새벽)으로, D 의 오버나이트는 D+1 영역으로 자연히 들어온다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleOverlapGuardServiceImpl implements ScheduleOverlapGuardService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int DAY_MIN = 1440;

    private final ScheduleGuardMapper scheduleGuardMapper;

    @Override
    public boolean hasCrossDayOverlap(String cmpnyCd, String siteCd, String userCd,
            String workYmd, String candidateSchCd, Map<String, String> pendingSchByYmd) {

        if (cmpnyCd == null || siteCd == null || userCd == null
                || workYmd == null || workYmd.length() != 8
                || candidateSchCd == null || candidateSchCd.isEmpty()) {
            return false;
        }

        // 대상일(D) 후보 스케줄의 구간(자기 자신 기준 delta=0).
        List<int[]> targetSegs = segmentsOf(cmpnyCd, siteCd, candidateSchCd, workYmd, 0);
        if (targetSegs.isEmpty()) {
            // 후보가 SCH 가 아니거나(예: 휴가코드) 시각 미확정 → 검사 불가 = 겹침 없음 취급.
            return false;
        }

        String prevYmd = shiftYmd(workYmd, -1);
        String nextYmd = shiftYmd(workYmd, 1);

        List<int[]> neighborSegs = new ArrayList<>();
        String prevSch = resolveNeighborSch(cmpnyCd, siteCd, userCd, prevYmd, pendingSchByYmd);
        if (prevSch != null) {
            neighborSegs.addAll(segmentsOf(cmpnyCd, siteCd, prevSch, prevYmd, -1));
        }
        String nextSch = resolveNeighborSch(cmpnyCd, siteCd, userCd, nextYmd, pendingSchByYmd);
        if (nextSch != null) {
            neighborSegs.addAll(segmentsOf(cmpnyCd, siteCd, nextSch, nextYmd, 1));
        }

        for (int[] t : targetSegs) {
            for (int[] n : neighborSegs) {
                if (overlaps(t, n)) {
                    log.info("교차일 스케줄 겹침 감지 - userCd={}, workYmd={}, schCd={}, target=[{},{}], neighbor=[{},{}]",
                            userCd, workYmd, candidateSchCd, t[0], t[1], n[0], n[1]);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 이웃 날짜의 적용 스케줄 코드 — 같은 저장 배치의 변경(pending) 우선, 없으면 work_plan DB 조회.
     * 빈 문자열/null 은 "그 날 스케줄 없음" 으로 보고 검사 대상에서 제외한다.
     */
    private String resolveNeighborSch(String cmpnyCd, String siteCd, String userCd, String ymd,
            Map<String, String> pendingSchByYmd) {
        if (pendingSchByYmd != null && pendingSchByYmd.containsKey(ymd)) {
            String v = pendingSchByYmd.get(ymd);
            return (v == null || v.isEmpty()) ? null : v;
        }
        String wp = scheduleGuardMapper.selectUserWorkPlanCd(cmpnyCd, siteCd, userCd, ymd);
        return (wp == null || wp.isEmpty()) ? null : wp;
    }

    /**
     * schCd 의 (asOfYmd 기준 effective) 1·2구간을 대상일 기준 분 타임라인 구간 목록으로 빌드한다.
     * @param delta 대상일 대비 일수 오프셋(D=0 / D-1=-1 / D+1=+1)
     */
    private List<int[]> segmentsOf(String cmpnyCd, String siteCd, String schCd, String asOfYmd, int delta) {
        List<int[]> out = new ArrayList<>(2);
        SchWindowResult w = scheduleGuardMapper.selectEffectiveSchWindow(cmpnyCd, siteCd, schCd, asOfYmd);
        if (w == null) {
            return out;
        }
        int[] s1 = seg(w.fstStart(), w.fstEnd(), delta);
        if (s1 != null) {
            out.add(s1);
        }
        int[] s2 = seg(w.secStart(), w.secEnd(), delta);
        if (s2 != null) {
            out.add(s2);
        }
        return out;
    }

    /**
     * (start,end) HHmm 한 쌍을 분 구간 [s,e] 로 빌드한다. 파싱 불가/빈값/0분 길이는 null(skip).
     * 종료 &lt; 시작이면 자정 넘김으로 +1440. 마지막에 delta*1440 오프셋을 더한다.
     * (attd07 {@code AttdScheduleUtils.buildPlanSegment} 와 동일 규약. 종료 "2400"=자정 경계 근무는
     *  {@code schEndToMinutes} 로 1440 인정[정책 attd/03 §3.3]. 시작/빈값/잘못된 시각은 여전히 파싱 불가로 skip)
     */
    private int[] seg(String startHHmm, String endHHmm, int delta) {
        Integer s = DateTimeUtils.hhmmToMinutes(startHHmm);
        // 종료는 자정 경계 근무 "2400"=1440 을 인정하는 종료 전용 파서 사용(시작은 strict 유지).
        Integer e = DateTimeUtils.schEndToMinutes(endHHmm);
        if (s == null || e == null) {
            return null;
        }
        int ss = s.intValue();
        int ee = e.intValue();
        if (ee == ss) {
            return null; // 0분 길이 = 데이터 결함, skip
        }
        if (ee < ss) {
            ee += DAY_MIN; // 자정 넘김(오버나이트)
        }
        int off = delta * DAY_MIN;
        return new int[] { ss + off, ee + off };
    }

    /** 인접 경계 허용(앞 종료 == 뒤 시작 은 비겹침). 그 외 교집합이 있으면 true. */
    private boolean overlaps(int[] a, int[] b) {
        return a[0] < b[1] && b[0] < a[1];
    }

    private String shiftYmd(String ymd, int days) {
        return LocalDate.parse(ymd, YMD).plusDays(days).format(YMD);
    }
}
