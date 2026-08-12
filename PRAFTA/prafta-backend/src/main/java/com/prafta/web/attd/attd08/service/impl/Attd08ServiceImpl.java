package com.prafta.web.attd.attd08.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.attd.util.FixedOtMinutesUtils;
import com.prafta.common.cmm.leave.util.PartialLeaveWindowUtils;
import com.prafta.common.cmm.schedule.util.FixedOtScheduleUtils;
import com.prafta.common.cmm.siteauth.service.SiteAccessService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.security.crypto.GpsCoordCrypto;
import com.prafta.common.util.DateTimeUtils;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.attd.attd08.application.param.AttdGpsTrailParam;
import com.prafta.web.attd.attd08.application.param.AttdListsParam;
import com.prafta.web.attd.attd08.application.query.AttdGpsTrailQuery;
import com.prafta.web.attd.attd08.application.query.AttdListsQuery;
import com.prafta.web.attd.attd08.dto.response.AttdGpsTrailResponse;
import com.prafta.web.attd.attd08.dto.response.AttdListsResponse;
import com.prafta.web.attd.attd08.mapper.Attd08Mapper;
import com.prafta.web.attd.attd08.result.AttdGpsTrailResult;
import com.prafta.web.attd.attd08.result.AttdGpsTrailRow;
import com.prafta.web.attd.attd08.result.AttdListsResult;
import com.prafta.web.attd.attd08.result.AttdOwnerScopeResult;
import com.prafta.web.attd.attd08.result.HalfLeaveWindowResult;
import com.prafta.web.attd.attd08.service.Attd08Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class Attd08ServiceImpl implements Attd08Service {

    private final Attd08Mapper attd08Mapper;

    /** GPS좌표-암호화-전환-03: 좌표 fallback 복호화(ENC 우선, NULL 이면 구 평문). */
    private final GpsCoordCrypto gpsCoordCrypto;

    /** 사업장 접근 인가(User_03 원장 TB_USER_SITE_AUTH 기반) — security H-1. */
    private final SiteAccessService siteAccessService;

    /** 부서 관리 권한 판정(master/hr 또는 노드 관리자) — security H-1. */
    private final AttdCloseService attdCloseService;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** OT 행 식별자(스케줄·판정 개념 없음). */
    private static final String ROW_TYPE_OT = "OT";

    private static final String STATUS_ABSENT = "ABSENT";
    private static final String STATUS_LATE = "LATE";
    private static final String STATUS_EARLY_LEAVE = "EARLY_LEAVE";
    private static final String STATUS_NORMAL = "NORMAL";

    @Override
    public AttdListsResponse getAttdLists(AttdListsParam param) {

        log.info("Attd_08 근태 현황 조회 진입 - from={}, to={}, siteCd={}, nodeCd={}, incSub={}",
                param.fromDate(), param.toDate(), param.siteCd(), param.nodeCd(), param.incSubNodeYn());

        // security H-1 ①: 사업장 접근 인가(원장 기반) — 타 사업장 siteCd 지정 차단.
        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(),
                param.gvSiteCd(), param.siteCd());

        // security H-1 ②: 부서 관리 권한 게이트(Attd_11:62,67 / Attd_15:84,87 동일 패턴).
        //   master/hr 이 아니면 nodeCd 미지정(사업장 전체)·관리 권한 없는 부서 조회를 차단한다.
        //   프론트 가드는 우회 가능하므로 서버에서 강제한다(PII + GPS 연쇄 노출 화면).
        if (!attdCloseService.canManageNode(
                param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), param.siteCd(), param.nodeCd())) {
            log.warn("Attd_08 조회 권한 없음 - userCd={}, authCd={}, siteCd={}, nodeCd={}",
                    param.gvUserCd(), param.gvAuthCd(), param.siteCd(), param.nodeCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        AttdListsQuery query = AttdListsQuery.from(param);

        List<AttdListsResult> rows = attd08Mapper.selectAttdLists(query);
        if (rows == null || rows.isEmpty()) {
            return AttdListsResponse.builder().attdListsResultList(new ArrayList<>()).build();
        }

        // D-1/D-3: 그날 확정 반차 면제 구간을 (사용자, 근무일) 단위로 모아 유효 소정 구간을 산출한다.
        Map<String, List<PartialLeaveWindowUtils.LeaveWindow>> leaveByUserYmd =
                groupHalfLeaveWindows(attd08Mapper.selectHalfLeaveWindows(query));

        List<AttdListsResult> judged = new ArrayList<>(rows.size());
        for (AttdListsResult r : rows) {
            judged.add(judge(r, leaveByUserYmd));
        }

        // PRAFTA-FIXEDOT-3: 고정연장 실적(자동 계상) + "연장 미이행" 배지 파생 산출(저장 없음).
        //   판정(judge)·조퇴 통계와 완전 분리 — 위 결과에는 손대지 않고 배지/실적 필드만 채운다.
        applyFixedOtDerivation(judged);

        return AttdListsResponse.builder()
                .attdListsResultList(judged)
                .build();
    }

    // ================================================================
    // PRAFTA-FIXEDOT-3: 고정연장 실적 + "연장 미이행" 배지 (파생 — 정책 ①·②·③)
    // ================================================================

    /**
     * 그날(사용자×근무일)의 고정연장 실적(분)과 후방 "연장 미이행" 배지를 파생 산출해
     * <b>마지막 스케줄 슬롯 행</b>에 싣는다(일 단위 값의 행 중복 방지).
     *
     * <ul>
     *   <li>실적(정책 ①): 실근태 슬롯 구간들 ∩ 고정연장 구간들 — {@code FixedOtMinutesUtils} 단일 출처.
     *       연차 계열 사용일에도 실근태가 있으면 그대로 계상("의무 면제, 근무하면 실적 계상").</li>
     *   <li>배지(정책 ②③): 후방 고정연장 존재 + 마지막 슬롯 퇴근 완료 + 퇴근 &lt; 후방 종료
     *       + 연차 계열 미사용(fixedOtExemptYn='N'). 미퇴근·결근·OT 행은 비대상.
     *       조퇴 판정(attdStatusCd)과 어떤 상태도 공유하지 않는다.</li>
     * </ul>
     *
     * <p>고정연장 없는 근무타입은 구간이 비어 전 행이 그대로 유지된다(무회귀).
     * TODO(developer): 전방 고정연장 미이행 배지는 1차 제외(plan §5-3) — 전방 실사용 타입 등장 시
     * FixedOtMinutesUtils 의 전방 판정 추가와 함께 확장.
     */
    private void applyFixedOtDerivation(List<AttdListsResult> judged) {
        // 그날 마지막 스케줄 슬롯 행 인덱스: 2구간 스케줄이면 seq2 행, 아니면 seq1 행.
        Map<String, Integer> targetIdxByDay = new HashMap<>();
        for (int i = 0; i < judged.size(); i++) {
            AttdListsResult r = judged.get(i);
            if (ROW_TYPE_OT.equals(r.rowType()) || r.workYmd() == null) {
                continue;
            }
            int lastSlotSeq = (blankToNull(r.plan2Start()) != null) ? 2 : 1;
            if (r.workSeq() != null && r.workSeq() == lastSlotSeq) {
                targetIdxByDay.put(leaveKey(r.userCd(), r.workYmd()), i);
            }
        }

        for (Map.Entry<String, Integer> e : targetIdxByDay.entrySet()) {
            int idx = e.getValue();
            AttdListsResult r = judged.get(idx);

            List<int[]> fixedOtSegs = FixedOtScheduleUtils.fixedOtSegments(
                    r.plan1Start(), r.plan1End(), r.plan2Start(), r.plan2End(),
                    r.preFixedOtStrTime(), r.preFixedOtEndTime(),
                    r.fixedOtStrTime(), r.fixedOtEndTime());
            if (fixedOtSegs.isEmpty()) {
                continue; // 고정연장 없는 근무타입 — 완전 불변 경로.
            }

            // 실적: 그날 1·2차 실근태 구간(행이 A1/A2 를 모두 들고 있어 한 행으로 충분) ∩ 고정연장.
            List<int[]> actualSegs = FixedOtMinutesUtils.buildActualSegments(r.workYmd(), new String[][] {
                    { r.act1InDate(), blankToNull(r.act1InTime()), r.act1OutDate(), blankToNull(r.act1OutTime()) },
                    { r.act2InDate(), blankToNull(r.act2InTime()), r.act2OutDate(), blankToNull(r.act2OutTime()) }
            });
            int actMinutes = FixedOtMinutesUtils.coveredMinutes(fixedOtSegs, actualSegs);

            // 배지: 마지막 슬롯(이 행의 차수) 퇴근 완료 + 퇴근 < 후방 종료 + 연차 계열 미사용.
            boolean isSeq2 = Integer.valueOf(2).equals(r.workSeq());
            Integer lastOutAnchor = FixedOtMinutesUtils.dayAnchorMinutes(
                    r.workYmd(),
                    isSeq2 ? r.act2OutDate() : r.act1OutDate(),
                    blankToNull(isSeq2 ? r.act2OutTime() : r.act1OutTime()));
            Integer rearEndAnchor = FixedOtMinutesUtils.rearFixedOtEndAnchor(
                    r.plan1Start(), r.plan1End(), r.plan2Start(), r.plan2End(),
                    r.fixedOtStrTime(), r.fixedOtEndTime());
            boolean unfulfilled = !"Y".equals(r.fixedOtExemptYn())
                    && FixedOtMinutesUtils.isRearUnfulfilled(rearEndAnchor, lastOutAnchor);

            judged.set(idx, r.withFixedOt(unfulfilled ? "Y" : null, actMinutes));
        }
    }

    @Override
    public AttdGpsTrailResponse getAttdGpsTrail(AttdGpsTrailParam param) {

        // security H-1 ③: 좌표 평문을 내리기 전에 "그 근태가 누구 것인지"부터 확인한다.
        //   파라미터가 attdId 뿐이라 파라미터만으로는 판정이 불가능하다 — 근태 행의 사업장/부서를
        //   먼저 읽고, 목록 조회와 동일한 게이트(사업장 인가 + 부서 관리 권한)를 적용한다.
        AttdOwnerScopeResult scope = attd08Mapper.selectAttdOwnerScope(param.gvCmpnyCd(), param.attdId());
        if (scope == null) {
            // 존재하지 않거나 타 회사 근태 — 존재 여부를 흘리지 않도록 권한 없음으로 수렴.
            log.warn("Attd_08 GPS 궤적 조회 거부(대상 없음) - userCd={}, attdId={}",
                    param.gvUserCd(), param.attdId());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        siteAccessService.assertSiteAccess(param.gvCmpnyCd(), param.gvUserCd(), param.gvAuthCd(),
                param.gvSiteCd(), scope.siteCd());

        if (!attdCloseService.canManageNode(
                param.gvAuthCd(), param.gvUserCd(), param.gvCmpnyCd(), scope.siteCd(), scope.nodeCd())) {
            log.warn("Attd_08 GPS 궤적 조회 권한 없음 - userCd={}, authCd={}, attdId={}, siteCd={}, nodeCd={}",
                    param.gvUserCd(), param.gvAuthCd(), param.attdId(), scope.siteCd(), scope.nodeCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        List<AttdGpsTrailRow> rows = attd08Mapper.selectAttdGpsTrail(AttdGpsTrailQuery.from(param));

        // GPS좌표-암호화-전환-03: 행 단위 fallback 복호화(ENC 우선, NULL 이면 구 평문) 후
        // 기존 AttdGpsTrailResult 로 재조립 — 응답 구조/필드명/타입 불변(웹 3개 소비처 무수정).
        // 좌표 평문/복호화값은 로그에 출력하지 않는다.
        List<AttdGpsTrailResult> attdGpsTrailResultList = new ArrayList<>();
        for (AttdGpsTrailRow row : rows) {
            attdGpsTrailResultList.add(new AttdGpsTrailResult(
                    row.gpsId()
                    , gpsCoordCrypto.resolveToBigDecimal(row.latEnc(), row.lat())
                    , gpsCoordCrypto.resolveToBigDecimal(row.lonEnc(), row.lon())
                    , row.accuracy()
                    , row.apiCallDate()
                    , row.apiCallTime()
                    , row.isMocked()
                    , row.gpsInfoType()
            ));
        }

        return AttdGpsTrailResponse.builder()
                .attdGpsTrailResultList(attdGpsTrailResultList)
                .build();
    }

    // ================================================================
    // D-1: 지각/조퇴/결근 판정 (SQL CASE → Java 이관, 2026-08-07)
    //   종전 SQL 은 HHMM "문자열" 비교라 야간(2200~0430)·자정 종료 스케줄에서 반차 보정이
    //   조용히 무발동했다. 판정 규칙을 Attd_11(Attd11ServiceImpl)·앱(AppAttd01ServiceImpl)과
    //   동일한 분 단위 일시(stamp) 비교 + PartialLeaveWindowUtils 로 통일한다.
    // ================================================================

    /** 면제 구간 행을 {@code userCd|workYmd} 키로 묶는다(한 날 다건 = 시작기준 + 종료기준). */
    private Map<String, List<PartialLeaveWindowUtils.LeaveWindow>> groupHalfLeaveWindows(
            List<HalfLeaveWindowResult> windows) {

        Map<String, List<PartialLeaveWindowUtils.LeaveWindow>> map = new HashMap<>();
        if (windows == null || windows.isEmpty()) {
            return map;
        }
        for (HalfLeaveWindowResult w : windows) {
            if (w.userCd() == null || w.workYmd() == null) {
                continue;
            }
            map.computeIfAbsent(leaveKey(w.userCd(), w.workYmd()), k -> new ArrayList<>())
                    .add(new PartialLeaveWindowUtils.LeaveWindow(w.startTime(), w.endTime()));
        }
        return map;
    }

    private String leaveKey(String userCd, String workYmd) {
        return userCd + "|" + workYmd;
    }

    /** 행 1건의 유효 소정 시각·상태를 산출해 반영한 결과를 돌려준다. */
    private AttdListsResult judge(AttdListsResult r,
                                  Map<String, List<PartialLeaveWindowUtils.LeaveWindow>> leaveByUserYmd) {

        if (ROW_TYPE_OT.equals(r.rowType())) {
            // 초과근무 행은 스케줄·판정 개념이 없다(화면에서 '-' 표기).
            return r;
        }

        String planStart = blankToNull(r.effPlanStart());
        String planEnd = blankToNull(r.effPlanEnd());

        // ① 반차 반영 유효 소정 구간(그날 면제 구간 전부의 합집합을 순차 적용 — D-3).
        List<PartialLeaveWindowUtils.LeaveWindow> leaves =
                leaveByUserYmd.get(leaveKey(r.userCd(), r.workYmd()));
        if (leaves != null && !leaves.isEmpty() && planStart != null && planEnd != null) {
            PartialLeaveWindowUtils.EffectiveWorkWindow eff =
                    PartialLeaveWindowUtils.resolveAll(planStart, planEnd, leaves);
            planStart = eff.fullyExempt() ? null : blankToNull(eff.planStart());
            planEnd = eff.fullyExempt() ? null : blankToNull(eff.planEnd());
        }

        // ② 상태 판정(Attd_11 / Attd_08.vue computeStatus 와 동일 규칙).
        return r.withJudgement(resolveStatus(r, planStart, planEnd), planStart, planEnd);
    }

    private String resolveStatus(AttdListsResult r, String planStart, String planEnd) {
        boolean isSeq2 = Integer.valueOf(2).equals(r.workSeq());
        String workYmd = blankToNull(r.workYmd());
        String inDate = blankToNull(isSeq2 ? r.act2InDate() : r.act1InDate());
        String inTime = blankToNull(isSeq2 ? r.act2InTime() : r.act1InTime());
        String outDate = blankToNull(isSeq2 ? r.act2OutDate() : r.act1OutDate());
        String outTime = blankToNull(isSeq2 ? r.act2OutTime() : r.act1OutTime());

        if (workYmd == null) {
            return null;
        }
        // 출근 기록이 없으면 결근(반차 유무와 무관 — 종전 동작).
        if (inTime == null) {
            return STATUS_ABSENT;
        }

        // 판정용 시각의 일자 프레임은 "원 스케줄"로 잡는다(야간이면 스케줄 시작보다 이른 시각 = 익일).
        //   유효 시각끼리 비교하는 종전 규칙은 야간 시작기준 반차(판정용 시작 01:15)를 당일로 두어
        //   "01:15 출근"을 지각으로 오판정했다.
        boolean isSeq2Row = Integer.valueOf(2).equals(r.workSeq());
        String rawStart = blankToNull(isSeq2Row ? r.plan2Start() : r.plan1Start());
        String rawEnd = blankToNull(isSeq2Row ? r.plan2End() : r.plan1End());

        // 지각: 실제 출근 일시 > 유효 소정 시작 일시.
        if (planStart != null) {
            Long schStart = stamp(shiftYmd(workYmd, rawStart, rawEnd, planStart), planStart);
            Long actIn = stamp(inDate != null ? inDate : workYmd, inTime);
            if (schStart != null && actIn != null && actIn > schStart) {
                return STATUS_LATE;
            }
        }

        // 조퇴: 실제 퇴근 일시 < 유효 소정 종료 일시.
        if (planEnd != null && outTime != null) {
            Long schEnd = stamp(shiftYmd(workYmd, rawStart, rawEnd, planEnd), planEnd);
            Long actOut = stamp(outDate != null ? outDate : workYmd, outTime);
            if (schEnd != null && actOut != null && actOut < schEnd) {
                return STATUS_EARLY_LEAVE;
            }
        }

        return STATUS_NORMAL;
    }

    /** 판정용 시각이 속한 일자(근무일 또는 익일) — 원 스케줄 프레임 기준. */
    private String shiftYmd(String workYmd, String rawStart, String rawEnd, String hhmm) {
        int offset = PartialLeaveWindowUtils.dayOffsetOf(rawStart, rawEnd, hhmm);
        return (offset == 0) ? workYmd : DateTimeUtils.plusDays(workYmd, offset);
    }

    /**
     * (일자 + HHmm) → 절대 분 stamp. 파싱 불가면 null → 그 판정은 건너뛴다(추정 금지).
     *
     * <p>Attd_11({@code Attd11ServiceImpl.toMinuteStamp})과 동일 산식이며, 스케줄 종료 표기
     * {@code "2400"}(= 익일 00:00)을 hh=24 로 그대로 환산한다.
     * ({@code DateTimeUtils.toMinuteStamp} 는 hh&gt;23 을 거부해 자정 종료 스케줄의 조퇴 판정이
     * 통째로 스킵되므로 여기서는 쓰지 않는다 — D-1 의 "2400 무발동"과 같은 함정.)
     */
    private Long stamp(String ymd, String hhmm) {
        if (ymd == null || ymd.length() != 8 || hhmm == null || hhmm.length() != 4) {
            return null;
        }
        try {
            long epochDays = LocalDate.parse(ymd, YMD).toEpochDay();
            int hh = Integer.parseInt(hhmm.substring(0, 2));
            int mm = Integer.parseInt(hhmm.substring(2, 4));
            if (hh < 0 || hh > 24 || mm < 0 || mm > 59) {
                return null;
            }
            return epochDays * 1440L + hh * 60L + mm;
        } catch (RuntimeException e) {
            log.warn("Attd_08 판정 시각 파싱 실패(판정 건너뜀) - ymd={}, hhmm={}", ymd, hhmm);
            return null;
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
