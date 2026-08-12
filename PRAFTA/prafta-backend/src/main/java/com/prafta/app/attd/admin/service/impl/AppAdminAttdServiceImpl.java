package com.prafta.app.attd.admin.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.prafta.app.admin.common.scope.application.query.ScopedNodeQuery;
import com.prafta.app.admin.common.scope.mapper.AdminScopeMapper;
import com.prafta.app.attd.admin.application.param.AdminDailyAttdParam;
import com.prafta.app.attd.admin.application.param.AdminMonthlyAttdParam;
import com.prafta.app.attd.admin.application.query.AdminAttdScopeQuery;
import com.prafta.app.attd.admin.dto.response.DailyAttdResponse;
import com.prafta.app.attd.admin.dto.response.MonthlyAttdResponse;
import com.prafta.app.attd.admin.mapper.AppAdminAttdMapper;
import com.prafta.app.attd.admin.result.DailyAttdRow;
import com.prafta.app.attd.admin.result.HalfLeaveWindowRow;
import com.prafta.app.attd.admin.result.MonthlyAttdRow;
import com.prafta.app.attd.admin.service.AppAdminAttdService;
import com.prafta.common.cmm.attd.util.FixedOtMinutesUtils;
import com.prafta.common.cmm.leave.util.PartialLeaveWindowUtils;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * J1-5: 앱 관리자 근태 상세 서비스 구현(조회 전용 — 일자/월별).
 *
 * <p>web attd08(일자)/attd11(월별) 조회 SQL 을 app mapper 로 포팅하고, 식별자/스코프를 토큰·노드(Phase 1 CTE)로
 * 치환했다(comApi/webApi 직접 호출 없음). 매퍼가 내려준 (USER_CD, WORK_YMD, WORK_SEQ) 원시 행을 일시
 * (YYYYMMDDHHmm) stamp 기준으로 행별 판정(지각/조퇴/근무시간)한 뒤 사용자(USER_CD) 단위로 집계한다.
 *
 * <p>야간(스케줄 종료 &lt; 시작)은 종료를 익일로 보정한다(attd11 Attd_08 computeStatus 동일 — 자정 넘김 오판 방지).
 * 지각/조퇴/근무시간은 원본 CHECK_IN/OUT 으로 판정한다. 페이징은 사용자 단위
 * 집계 후 서비스에서 슬라이스한다(메모리 적재 상한 가드 포함).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppAdminAttdServiceImpl implements AppAdminAttdService {

    private final AppAdminAttdMapper mapper;
    private final AdminScopeMapper adminScopeMapper;   // 노드 자손 전개(재귀 CTE) — Phase 1 재사용

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 사용자 단위 집계 후 메모리 슬라이스 상한(노드/사업장 단위 조회라 통상 수백 명 이내 — DoS 방어용 넉넉한 천장).
    private static final int MAX_OFFSET = 10000;

    // ============================ 스코프 산출 ============================

    /**
     * 근태 상세 스코프: master=전사(회사 내 siteCd) / hr=사업장 / 노드관리자=자기노드+자손. safe 단독 ⛔.
     * <p>승인관리 AppAdminApprovalServiceImpl.resolveScope 와 동일 축(ATTD_DETAIL=APPROVAL 활성식).
     */
    private ScopeContext resolveScope(String cmpnyCd, String userCd, String siteCd, String authCd) {
        if (AuthRoleUtils.isAccessDenied(authCd)) {
            log.warn("근태 상세 접근 차단(권한 없음) - authCd={}", authCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
        if (!StringUtils.hasText(siteCd)) {
            // 사업장 미지정이면 어느 축이든 노드/사업장 필터를 적용할 수 없다 → 차단.
            log.warn("근태 상세 사업장 미지정 - userCd={}, authCd={}", userCd, authCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
        if (AuthRoleUtils.AUTH_MASTER.equals(authCd) || AuthRoleUtils.AUTH_HR_MANAGER.equals(authCd)) {
            // master/hr: 사업장 전부서(노드 무필터). companyWide 플래그는 매퍼에서 노드 필터 생략 의미로만 쓴다.
            return new ScopeContext(true, false, Collections.emptyList());
        }
        // safe 단독 또는 노드관리자 후보 → 노드 스코프. 노드관리자 아니면(빈 집합) 차단(safe ⛔).
        List<String> scopedNodeCds =
                adminScopeMapper.selectScopedNodeCds(ScopedNodeQuery.of(cmpnyCd, siteCd, userCd));
        if (scopedNodeCds == null || scopedNodeCds.isEmpty()) {
            log.warn("근태 상세 진입 권한 없음(노드관리자 아님/safe 단독) - authCd={}, siteCd={}", authCd, siteCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
        return new ScopeContext(false, true, scopedNodeCds);
    }

    /**
     * 요청 nodeCd(있으면)를 토큰 스코프로 좁힌다(IDOR 재검증).
     * <ul>
     *   <li>master/hr(companyWide): nodeCd 지정 시 단일 노드로 좁힘(사업장 내 임의 노드 허용 — 사업장 권한 보유).</li>
     *   <li>노드관리자(useNodeScope): nodeCd 가 자기 스코프 밖이면 빈 결과(403 대신 스코프 외 노출 0).
     *       nodeCd 미지정이면 전체 스코프 노드를 사용한다.</li>
     * </ul>
     * @return 매퍼에 실을 노드 IN 집합(companyWide 이고 nodeCd 미지정이면 null=무필터).
     */
    private List<String> effectiveScopedNodes(ScopeContext s, String nodeCd) {
        if (s.companyWide()) {
            // master/hr: nodeCd 지정 시 그 노드만(사업장 내), 미지정이면 사업장 전부서(무필터).
            if (StringUtils.hasText(nodeCd)) {
                return List.of(nodeCd);
            }
            return null;
        }
        // 노드관리자: nodeCd 지정 시 스코프 교집합(밖이면 빈 집합 → 빈 결과), 미지정이면 전체 스코프.
        if (StringUtils.hasText(nodeCd)) {
            if (s.scopedNodeCds().contains(nodeCd)) {
                return List.of(nodeCd);
            }
            log.warn("근태 상세 노드 스코프 위반(IDOR 차단) - 요청 nodeCd={}", nodeCd);
            return Collections.emptyList();
        }
        return s.scopedNodeCds();
    }

    private record ScopeContext(boolean companyWide, boolean useNodeScope, List<String> scopedNodeCds) {
    }

    // ============================ 일자 근태 현황 ============================

    @Override
    public DailyAttdResponse selectDaily(AdminDailyAttdParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());
        List<String> nodes = effectiveScopedNodes(scope, param.nodeCd());

        // IDOR: 노드관리자가 스코프 밖 노드를 지정 → 빈 결과(노출 0).
        if (nodes != null && nodes.isEmpty()) {
            return DailyAttdResponse.builder()
                    .items(Collections.emptyList()).totalCount(0).hasMore(false).build();
        }

        boolean useNodeScope = nodes != null;   // null=무필터(master/hr 전부서), 아니면 IN 필터.
        AdminAttdScopeQuery query = new AdminAttdScopeQuery(
                param.gvCmpnyCd(), param.gvSiteCd(),
                !useNodeScope, useNodeScope, useNodeScope ? nodes : Collections.emptyList(),
                param.keyword(), param.workYmd(), null);

        List<DailyAttdRow> rows = mapper.selectDailyAttdRows(query);

        // NF-1: 그날 확정 반차 면제 구간(사용자·근무일 단위) — 지각·조퇴 판정 기준을 웹 Attd_08/Attd_11 과 맞춘다.
        Map<String, List<PartialLeaveWindowUtils.LeaveWindow>> leaveByUserYmd =
                groupHalfLeaveWindows(mapper.selectHalfLeaveWindows(query));

        // 사용자 단위 집계(등장 순서 유지 — 매퍼가 USER_CD, WORK_SEQ 순 정렬).
        Map<String, DailyAcc> accByUser = new LinkedHashMap<>();
        // PRAFTA-FIXEDOT-3: (사용자, 근무일=단일일) 고정연장 파생 컨텍스트 — 판정과 완전 분리(additive).
        Map<String, FixedOtDayCtx> fixedOtByUser = new LinkedHashMap<>();
        for (DailyAttdRow r : rows) {
            DailyAcc acc = accByUser.computeIfAbsent(r.userCd(), k -> new DailyAcc(r));

            if (r.workYmd() != null && (blankToNull(r.fixedOtStrTime()) != null
                    || blankToNull(r.preFixedOtStrTime()) != null)) {
                fixedOtByUser.computeIfAbsent(r.userCd(), k -> new FixedOtDayCtx(
                                r.userCd(), r.workYmd(), r.fstSchStrTime(), r.fstSchEndTime(),
                                r.secSchStrTime(), r.secSchEndTime(),
                                r.preFixedOtStrTime(), r.preFixedOtEndTime(),
                                r.fixedOtStrTime(), r.fixedOtEndTime(), r.fixedOtExemptYn()))
                        .addSlot(r.workSeq(), r.checkInDate(), blankToNull(r.checkInTime()),
                                r.checkOutDate(), blankToNull(r.checkOutTime()));
            }

            String inDate = blankToNull(r.checkInDate());
            String inTime = blankToNull(r.checkInTime());
            String outDate = blankToNull(r.checkOutDate());
            String outTime = blankToNull(r.checkOutTime());
            String workYmd = blankToNull(r.workYmd());

            // NF-1: 반차 반영 유효 소정 구간(그날 면제 구간 전부를 순차 적용 — 반차 2건이면 종일 면제).
            //   판정용 시각의 일자 프레임은 "원 스케줄"(rawStart/rawEnd)로 잡는다(야간 시작기준 반차 오판정 방지).
            String rawStart = blankToNull(r.planStart());
            String rawEnd = blankToNull(r.planEnd());
            String planStart = rawStart;
            String planEnd = rawEnd;
            List<PartialLeaveWindowUtils.LeaveWindow> leaves =
                    leaveByUserYmd.get(leaveKey(r.userCd(), workYmd));
            if (leaves != null && !leaves.isEmpty() && planStart != null && planEnd != null) {
                PartialLeaveWindowUtils.EffectiveWorkWindow eff =
                        PartialLeaveWindowUtils.resolveAll(planStart, planEnd, leaves);
                planStart = eff.fullyExempt() ? null : blankToNull(eff.planStart());
                planEnd = eff.fullyExempt() ? null : blankToNull(eff.planEnd());
            }

            acc.slotCount += 1;

            // 첫 차수(workSeq=1) 출근시각, 마지막 차수 퇴근시각 표시.
            if (r.workSeq() == 1) {
                if (inTime != null && acc.checkInTime == null) {
                    acc.checkInTime = inTime;
                }
            }
            // 퇴근은 가장 마지막(차수 큰 쪽) 값 우선 — 행이 WORK_SEQ 오름차순이므로 항상 덮어쓴다.
            if (outTime != null) {
                acc.checkOutTime = outTime;
            }

            if ("Y".equals(r.isOutsideYn())) {
                acc.isOffsite = true;
            }

            // 근무 분: 출근·퇴근 모두 존재 — (퇴근일시 - 출근일시), 음수 0. (일자 리스트는 휴게 미차감 — 표시 단순화)
            if (inTime != null && outTime != null && workYmd != null) {
                long inStamp = toMinuteStamp(inDate != null ? inDate : workYmd, inTime);
                long outStamp = toMinuteStamp(outDate != null ? outDate : workYmd, outTime);
                long worked = outStamp - inStamp;
                if (worked > 0) {
                    acc.workMinutes += worked;
                }
            }

            // 지각: 출근 + 유효 소정 시작 존재 시 실제출근일시 > 유효 소정 시작 일시.
            if (inTime != null && planStart != null && workYmd != null) {
                long schStartStamp = toMinuteStamp(
                        shiftYmd(workYmd, rawStart, rawEnd, planStart), planStart);
                long actInStamp = toMinuteStamp(inDate != null ? inDate : workYmd, inTime);
                if (actInStamp > schStartStamp) {
                    acc.isLate = true;
                }
            }

            // 조퇴: 퇴근 + 유효 소정 종료 존재 시 실제퇴근일시 < 유효 소정 종료 일시.
            //   야간(원 스케줄 종료 < 시작)이면 스케줄 시작보다 이른 시각은 근무일 익일로 본다.
            if (outTime != null && planEnd != null && workYmd != null) {
                long schEndStamp = toMinuteStamp(
                        shiftYmd(workYmd, rawStart, rawEnd, planEnd), planEnd);
                long actOutStamp = toMinuteStamp(outDate != null ? outDate : workYmd, outTime);
                if (actOutStamp < schEndStamp) {
                    acc.isEarly = true;
                }
            }
        }

        List<DailyAttdResponse.DailyItem> all = new ArrayList<>(accByUser.size());
        for (Map.Entry<String, DailyAcc> entry : accByUser.entrySet()) {
            DailyAcc acc = entry.getValue();
            // PRAFTA-FIXEDOT-3: 고정연장 실적(그날 실근태 ∩ 고정연장) + "연장 미이행" 배지(조퇴와 분리).
            //   고정연장 없는 타입은 ctx 미생성 → 0/false(기존 응답과 완전 동일 경로).
            FixedOtDayCtx fx = fixedOtByUser.get(entry.getKey());
            long fixedOtMinutes = 0L;
            boolean fixedOtUnmet = false;
            if (fx != null) {
                fixedOtMinutes = fx.actualMinutes();
                fixedOtUnmet = fx.rearUnfulfilled();
            }
            all.add(DailyAttdResponse.DailyItem.builder()
                    .userCd(acc.userCd)
                    .userNm(acc.userNm)
                    .nodeNm(acc.nodeNm)
                    .checkInTime(acc.checkInTime)
                    .checkOutTime(acc.checkOutTime)
                    .isLate(acc.isLate)
                    .isEarly(acc.isEarly)
                    .isOffsite(acc.isOffsite)
                    .workMinutes(acc.workMinutes)
                    .slotCount(acc.slotCount)
                    .fixedOtMinutes(fixedOtMinutes)
                    .isFixedOtUnmet(fixedOtUnmet)
                    .build());
        }

        int totalCount = all.size();
        int offset = (param.page() - 1) * param.pageSize();
        List<DailyAttdResponse.DailyItem> items = slice(all, offset, param.pageSize());
        boolean hasMore = offset + items.size() < totalCount;

        log.info("앱 관리자 일자 근태 현황 조회 - cmpnyCd={}, siteCd={}, workYmd={}, 사용자={}명",
                param.gvCmpnyCd(), param.gvSiteCd(), param.workYmd(), totalCount);

        return DailyAttdResponse.builder()
                .items(items).totalCount(totalCount).hasMore(hasMore).build();
    }

    // ============================ 월별 집계 ============================

    @Override
    public MonthlyAttdResponse selectMonthly(AdminMonthlyAttdParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());
        List<String> nodes = effectiveScopedNodes(scope, param.nodeCd());

        if (nodes != null && nodes.isEmpty()) {
            return MonthlyAttdResponse.builder()
                    .items(Collections.emptyList()).totalCount(0).hasMore(false).build();
        }

        boolean useNodeScope = nodes != null;
        AdminAttdScopeQuery query = new AdminAttdScopeQuery(
                param.gvCmpnyCd(), param.gvSiteCd(),
                !useNodeScope, useNodeScope, useNodeScope ? nodes : Collections.emptyList(),
                param.keyword(), null, param.workYm());

        List<MonthlyAttdRow> rows = mapper.selectMonthlyAttdRows(query);

        // NF-1: 그 달 확정 반차 면제 구간(사용자·근무일 단위) — 일자 조회와 동일 판정 기준.
        Map<String, List<PartialLeaveWindowUtils.LeaveWindow>> leaveByUserYmd =
                groupHalfLeaveWindows(mapper.selectHalfLeaveWindows(query));

        Map<String, MonthlyAcc> accByUser = new LinkedHashMap<>();
        // PRAFTA-FIXEDOT-3: (사용자, 근무일) 단위 고정연장 파생 컨텍스트(월 실적 합·미이행 카운트용).
        Map<String, FixedOtDayCtx> fixedOtDayByKey = new LinkedHashMap<>();
        for (MonthlyAttdRow r : rows) {
            MonthlyAcc acc = accByUser.computeIfAbsent(r.userCd(), k -> new MonthlyAcc(r));

            if (r.workYmd() != null && (blankToNull(r.fixedOtStrTime()) != null
                    || blankToNull(r.preFixedOtStrTime()) != null)) {
                fixedOtDayByKey.computeIfAbsent(leaveKey(r.userCd(), r.workYmd()), k -> new FixedOtDayCtx(
                                r.userCd(), r.workYmd(), r.fstSchStrTime(), r.fstSchEndTime(),
                                r.secSchStrTime(), r.secSchEndTime(),
                                r.preFixedOtStrTime(), r.preFixedOtEndTime(),
                                r.fixedOtStrTime(), r.fixedOtEndTime(), r.fixedOtExemptYn()))
                        .addSlot(r.workSeq(), r.actInDate(), blankToNull(r.actInTime()),
                                r.actOutDate(), blankToNull(r.actOutTime()));
            }

            String inDate = blankToNull(r.actInDate());
            String inTime = blankToNull(r.actInTime());
            String outDate = blankToNull(r.actOutDate());
            String outTime = blankToNull(r.actOutTime());
            String workYmd = blankToNull(r.workYmd());
            int breakMin = r.planBreakMin() == null ? 0 : r.planBreakMin();

            // NF-1: 반차 반영 유효 소정 구간(일자 조회와 동일 — PartialLeaveWindowUtils 단일 출처).
            String rawStart = blankToNull(r.planStart());
            String rawEnd = blankToNull(r.planEnd());
            String planStart = rawStart;
            String planEnd = rawEnd;
            List<PartialLeaveWindowUtils.LeaveWindow> leaves =
                    leaveByUserYmd.get(leaveKey(r.userCd(), workYmd));
            if (leaves != null && !leaves.isEmpty() && planStart != null && planEnd != null) {
                PartialLeaveWindowUtils.EffectiveWorkWindow eff =
                        PartialLeaveWindowUtils.resolveAll(planStart, planEnd, leaves);
                planStart = eff.fullyExempt() ? null : blankToNull(eff.planStart());
                planEnd = eff.fullyExempt() ? null : blankToNull(eff.planEnd());
            }

            // 근무일수: 출근 기록 존재 distinct WORK_YMD(차수 무관).
            if (inTime != null && workYmd != null) {
                acc.workDays.add(workYmd);
            }

            // 총 근무시간(분): 출근·퇴근 모두 존재 — (퇴근일시 - 출근일시) - 휴게, 음수 0.
            if (inTime != null && outTime != null && workYmd != null) {
                long inStamp = toMinuteStamp(inDate != null ? inDate : workYmd, inTime);
                long outStamp = toMinuteStamp(outDate != null ? outDate : workYmd, outTime);
                long worked = (outStamp - inStamp) - breakMin;
                if (worked > 0) {
                    acc.workMinutes += worked;
                }
            }

            // 지각(차수 단위 카운트): 실제출근일시 > 유효 소정 시작 일시.
            if (inTime != null && planStart != null && workYmd != null) {
                long schStartStamp = toMinuteStamp(
                        shiftYmd(workYmd, rawStart, rawEnd, planStart), planStart);
                long actInStamp = toMinuteStamp(inDate != null ? inDate : workYmd, inTime);
                if (actInStamp > schStartStamp) {
                    acc.lateCnt += 1;
                }
            }

            // 조퇴(차수 단위 카운트): 실제퇴근일시 < 유효 소정 종료 일시(야간 종료 익일 보정).
            if (outTime != null && planEnd != null && workYmd != null) {
                long schEndStamp = toMinuteStamp(
                        shiftYmd(workYmd, rawStart, rawEnd, planEnd), planEnd);
                long actOutStamp = toMinuteStamp(outDate != null ? outDate : workYmd, outTime);
                if (actOutStamp < schEndStamp) {
                    acc.earlyCnt += 1;
                }
            }
        }

        // PRAFTA-FIXEDOT-3: 일 단위 파생을 사용자 누적기에 반영(실적 합 + 미이행 일수 — 조퇴 카운트와 분리).
        for (FixedOtDayCtx fx : fixedOtDayByKey.values()) {
            MonthlyAcc acc = accByUser.get(fx.userCd);
            if (acc == null) {
                continue;
            }
            acc.fixedOtMinutes += fx.actualMinutes();
            if (fx.rearUnfulfilled()) {
                acc.fixedOtUnmetCnt += 1;
            }
        }

        List<MonthlyAttdResponse.MonthlyItem> all = new ArrayList<>(accByUser.size());
        for (MonthlyAcc acc : accByUser.values()) {
            all.add(MonthlyAttdResponse.MonthlyItem.builder()
                    .userCd(acc.userCd)
                    .userNm(acc.userNm)
                    .nodeNm(acc.nodeNm)
                    .workDays(acc.workDays.size())
                    .workMinutes(acc.workMinutes)
                    .lateCnt(acc.lateCnt)
                    .earlyCnt(acc.earlyCnt)
                    .fixedOtMinutes(acc.fixedOtMinutes)
                    .fixedOtUnmetCnt(acc.fixedOtUnmetCnt)
                    .build());
        }

        int totalCount = all.size();
        int offset = (param.page() - 1) * param.pageSize();
        List<MonthlyAttdResponse.MonthlyItem> items = slice(all, offset, param.pageSize());
        boolean hasMore = offset + items.size() < totalCount;

        log.info("앱 관리자 월별 근태 집계 조회 - cmpnyCd={}, siteCd={}, workYm={}, 사용자={}명",
                param.gvCmpnyCd(), param.gvSiteCd(), param.workYm(), totalCount);

        return MonthlyAttdResponse.builder()
                .items(items).totalCount(totalCount).hasMore(hasMore).build();
    }

    // ============================ 집계 누적기 ============================

    private static final class DailyAcc {
        final String userCd;
        final String userNm;
        final String nodeNm;
        String checkInTime;
        String checkOutTime;
        boolean isLate;
        boolean isEarly;
        boolean isOffsite;
        long workMinutes;
        int slotCount;

        DailyAcc(DailyAttdRow r) {
            this.userCd = r.userCd();
            this.userNm = r.userNm();
            this.nodeNm = r.nodeNm();
        }
    }

    private static final class MonthlyAcc {
        final String userCd;
        final String userNm;
        final String nodeNm;
        final java.util.Set<String> workDays = new java.util.HashSet<>();
        long workMinutes;
        int lateCnt;
        int earlyCnt;
        // PRAFTA-FIXEDOT-3: 고정연장 실적 합(분) + "연장 미이행" 일수 — 조퇴 카운트와 분리(additive).
        long fixedOtMinutes;
        int fixedOtUnmetCnt;

        MonthlyAcc(MonthlyAttdRow r) {
            this.userCd = r.userCd();
            this.userNm = r.userNm();
            this.nodeNm = r.nodeNm();
        }
    }

    /**
     * PRAFTA-FIXEDOT-3: (사용자, 근무일) 단위 고정연장 파생 계산 컨텍스트(web Attd11 동형).
     * 스케줄/고정연장/면제 값은 그날 전 슬롯 행에서 동일 — 첫 행 값 고정. 슬롯별 실근태 구간과
     * 마지막 스케줄 슬롯의 퇴근 스탬프만 행마다 수집한다. 판정(지각/조퇴)과 어떤 상태도 공유하지 않는다.
     */
    private static final class FixedOtDayCtx {
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

        FixedOtDayCtx(String userCd, String workYmd,
                      String fstSchStrTime, String fstSchEndTime,
                      String secSchStrTime, String secSchEndTime,
                      String preFixedOtStrTime, String preFixedOtEndTime,
                      String fixedOtStrTime, String fixedOtEndTime, String fixedOtExemptYn) {
            this.userCd = userCd;
            this.workYmd = workYmd;
            this.fstSchStrTime = fstSchStrTime;
            this.fstSchEndTime = fstSchEndTime;
            this.secSchStrTime = secSchStrTime;
            this.secSchEndTime = secSchEndTime;
            this.preFixedOtStrTime = preFixedOtStrTime;
            this.preFixedOtEndTime = preFixedOtEndTime;
            this.fixedOtStrTime = fixedOtStrTime;
            this.fixedOtEndTime = fixedOtEndTime;
            this.fixedOtExemptYn = fixedOtExemptYn;
        }

        void addSlot(int workSeq, String inDate, String inTime, String outDate, String outTime) {
            int[] seg = FixedOtMinutesUtils.actualSegment(
                    FixedOtMinutesUtils.dayAnchorMinutes(workYmd, inDate, inTime),
                    FixedOtMinutesUtils.dayAnchorMinutes(workYmd, outDate, outTime));
            if (seg != null) {
                actualSegs.add(seg);
            }
            // 마지막 스케줄 슬롯(2구간 스케줄이면 seq2, 아니면 seq1)의 퇴근 스탬프 — 미이행 판정 기준.
            int lastSlotSeq = (secSchStrTime != null && !secSchStrTime.isEmpty()) ? 2 : 1;
            if (workSeq == lastSlotSeq) {
                lastSlotOutAnchor = FixedOtMinutesUtils.dayAnchorMinutes(workYmd, outDate, outTime);
            }
        }

        /** 그날 고정연장 실적(분) — 실근태 ∩ 고정연장(정책 ① — 연차 사용일도 근무하면 계상). */
        long actualMinutes() {
            return FixedOtMinutesUtils.dayFixedOtActualMinutes(
                    fstSchStrTime, fstSchEndTime, secSchStrTime, secSchEndTime,
                    preFixedOtStrTime, preFixedOtEndTime, fixedOtStrTime, fixedOtEndTime,
                    actualSegs);
        }

        /** "연장 미이행"(후방) — 정책 ②③: 연차 계열 사용일·미퇴근이면 false. 전방 배지는 1차 제외(TODO). */
        boolean rearUnfulfilled() {
            return !"Y".equals(fixedOtExemptYn)
                    && FixedOtMinutesUtils.isRearUnfulfilled(
                            FixedOtMinutesUtils.rearFixedOtEndAnchor(
                                    fstSchStrTime, fstSchEndTime, secSchStrTime, secSchEndTime,
                                    fixedOtStrTime, fixedOtEndTime),
                            lastSlotOutAnchor);
        }
    }

    // ============================ 보조 ============================

    /** 일자(YYYYMMDD) + 시각(HHmm) 을 1970-01-01 기준 통합 분(minute) stamp 로 환산(attd11 동일). */
    private long toMinuteStamp(String ymd, String hhmm) {
        LocalDate d = LocalDate.parse(ymd, YMD);
        long epochDays = d.toEpochDay();
        int hh = Integer.parseInt(hhmm.substring(0, 2));
        int mm = Integer.parseInt(hhmm.substring(2, 4));
        return epochDays * 1440L + (long) hh * 60L + mm;
    }

    /**
     * NF-1: 확정 반차 면제 구간을 (USER_CD|WORK_YMD) 키로 모은다.
     *
     * <p>반차는 하루 2건(시작기준 0.5 + 종료기준 0.5 = 1.0)이 성립하므로 단건 가정 금지 —
     * 여러 건을 {@code resolveAll} 로 순차 적용해 합집합으로 판정한다(웹 Attd_08/Attd_11 동일).
     */
    private Map<String, List<PartialLeaveWindowUtils.LeaveWindow>> groupHalfLeaveWindows(
            List<HalfLeaveWindowRow> windows) {

        Map<String, List<PartialLeaveWindowUtils.LeaveWindow>> map = new HashMap<>();
        if (windows == null || windows.isEmpty()) {
            return map;
        }
        for (HalfLeaveWindowRow w : windows) {
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

    /**
     * 판정용 시각이 속한 일자(근무일 또는 익일) — 원 스케줄 프레임 기준.
     * 야간 스케줄에서 스케줄 시작보다 이른 시각은 익일이다(웹 Attd_08/Attd_11 shiftYmd 동일).
     */
    private String shiftYmd(String workYmd, String rawStart, String rawEnd, String hhmm) {
        int offset = PartialLeaveWindowUtils.dayOffsetOf(rawStart, rawEnd, hhmm);
        return (offset == 0) ? workYmd : ymdPlusDays(workYmd, offset);
    }

    private String ymdPlusDays(String ymd, int days) {
        return LocalDate.parse(ymd, YMD).plusDays(days).format(YMD);
    }

    private String blankToNull(String s) {
        return (s == null || s.isEmpty()) ? null : s;
    }

    private <T> List<T> slice(List<T> all, int offset, int size) {
        // offset 상한 가드(사용자 단위 집계 후라 OOM 위험은 낮지만 일관성 유지).
        if (offset > MAX_OFFSET || offset >= all.size()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(all.subList(offset, Math.min(offset + size, all.size())));
    }
}
