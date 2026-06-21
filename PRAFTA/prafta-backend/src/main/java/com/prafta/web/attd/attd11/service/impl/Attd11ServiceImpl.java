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

import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.attd.attd11.application.param.MonthlyAttdSummaryParam;
import com.prafta.web.attd.attd11.application.query.MonthlyAttdSummaryQuery;
import com.prafta.web.attd.attd11.dto.response.MonthlyAttdSummaryResponse;
import com.prafta.web.attd.attd11.mapper.Attd11Mapper;
import com.prafta.web.attd.attd11.result.AbsentDayCountResult;
import com.prafta.web.attd.attd11.result.AttdSummaryRowResult;
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

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public MonthlyAttdSummaryResponse getMonthlyAttdSummary(MonthlyAttdSummaryParam param) {

        log.info("Attd_11 월별 사용자 근태 판정 조회 진입 - workYm={}, siteCd={}, nodeCd={}, incSub={}",
                param.workYm(), param.siteCd(), param.nodeCd(), param.incSubNodeYn());

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

        // 사용자별 초과근무 분 합 (decisions §3-4 : COMPLETED 만)
        Map<String, Long> otMinutesByUser = new HashMap<>();
        for (OvertimeSummaryResult ot : otRows) {
            otMinutesByUser.put(ot.userCd(), ot.otMinutes());
        }

        // 사용자별 미출근일 수 (COM-016-F 8-3 : 스케줄 있으나 미출근, 휴일·연차·미래일 제외)
        //   기존 결과 행 집합/모수는 변경하지 않고 매핑만 한다(보수적/무회귀 — 미출근만 있는
        //   사용자를 새 행으로 노출하지 않음. 출근/초과근무 기록이 있는 기존 행에만 주입).
        List<AbsentDayCountResult> absentRows = attd11Mapper.selectAbsentDayCount(query);
        Map<String, Integer> absentByUser = new HashMap<>();
        for (AbsentDayCountResult ab : absentRows) {
            absentByUser.put(ab.userCd(), ab.absentDayCnt());
        }

        // 사용자별 누적기(등장 순서 유지 — 매퍼가 USER_CD, WORK_YMD, WORK_SEQ 순 정렬)
        Map<String, UserAccumulator> accByUser = new LinkedHashMap<>();

        for (AttdSummaryRowResult r : rows) {
            UserAccumulator acc = accByUser.computeIfAbsent(r.userCd(), k -> new UserAccumulator(r));

            String inDate = blankToNull(r.actInDate());
            String inTime = blankToNull(r.actInTime());
            String outDate = blankToNull(r.actOutDate());
            String outTime = blankToNull(r.actOutTime());
            String workYmd = blankToNull(r.workYmd());
            String planStart = blankToNull(r.planStart());
            String planEnd = blankToNull(r.planEnd());
            int breakMin = r.planBreakMin() == null ? 0 : r.planBreakMin();

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

            // 지각: 출근 기록 + 스케줄 시작 존재 시, 실제출근일시 > 스케줄시작일시
            if (inTime != null && planStart != null && workYmd != null) {
                long schStartStamp = toMinuteStamp(workYmd, planStart);
                long actInStamp = toMinuteStamp(inDate != null ? inDate : workYmd, inTime);
                if (actInStamp > schStartStamp) {
                    acc.lateCnt += 1;
                    acc.lateMinutes += (actInStamp - schStartStamp);
                }
            }

            // 조퇴: 퇴근 기록 + 스케줄 종료 존재 시, 실제퇴근일시 < 스케줄종료일시
            //   야간(스케줄 종료 < 시작)이면 종료는 근무일자 익일로 본다 (Attd_08 computeStatus)
            if (outTime != null && planEnd != null && workYmd != null) {
                String endYmd = workYmd;
                if (planStart != null && planEnd.compareTo(planStart) < 0) {
                    endYmd = ymdPlusDays(workYmd, 1);
                }
                long schEndStamp = toMinuteStamp(endYmd, planEnd);
                long actOutStamp = toMinuteStamp(outDate != null ? outDate : workYmd, outTime);
                if (actOutStamp < schEndStamp) {
                    acc.earlyLeaveCnt += 1;
                    acc.earlyLeaveMinutes += (schEndStamp - actOutStamp);
                }
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
                    , absentByUser.getOrDefault(e.getKey(), 0)
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
                    , absentByUser.getOrDefault(ot.userCd(), 0)
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

    private String blankToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
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

        UserAccumulator(AttdSummaryRowResult r) {
            this.userCd = r.userCd();
            this.userId = r.userId();
            this.userNm = r.userNm();
            this.deptNm = r.deptNm();
            this.authCd = r.authCd();
            this.authNm = r.authNm();
        }
    }
}
