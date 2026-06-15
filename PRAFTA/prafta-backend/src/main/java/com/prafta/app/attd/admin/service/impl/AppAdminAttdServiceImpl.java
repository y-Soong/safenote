package com.prafta.app.attd.admin.service.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
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
import com.prafta.app.attd.admin.result.MonthlyAttdRow;
import com.prafta.app.attd.admin.service.AppAdminAttdService;
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

        // 사용자 단위 집계(등장 순서 유지 — 매퍼가 USER_CD, WORK_SEQ 순 정렬).
        Map<String, DailyAcc> accByUser = new LinkedHashMap<>();
        for (DailyAttdRow r : rows) {
            DailyAcc acc = accByUser.computeIfAbsent(r.userCd(), k -> new DailyAcc(r));

            String inDate = blankToNull(r.checkInDate());
            String inTime = blankToNull(r.checkInTime());
            String outDate = blankToNull(r.checkOutDate());
            String outTime = blankToNull(r.checkOutTime());
            String workYmd = blankToNull(r.workYmd());
            String planStart = blankToNull(r.planStart());
            String planEnd = blankToNull(r.planEnd());

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

            // 지각: 출근 + 스케줄 시작 존재 시 실제출근일시 > 스케줄시작일시(raw stamp).
            if (inTime != null && planStart != null && workYmd != null) {
                long schStartStamp = toMinuteStamp(workYmd, planStart);
                long actInStamp = toMinuteStamp(inDate != null ? inDate : workYmd, inTime);
                if (actInStamp > schStartStamp) {
                    acc.isLate = true;
                }
            }

            // 조퇴: 퇴근 + 스케줄 종료 존재 시 실제퇴근일시 < 스케줄종료일시(야간이면 종료 익일 보정).
            if (outTime != null && planEnd != null && workYmd != null) {
                String endYmd = workYmd;
                if (planStart != null && planEnd.compareTo(planStart) < 0) {
                    endYmd = ymdPlusDays(workYmd, 1);
                }
                long schEndStamp = toMinuteStamp(endYmd, planEnd);
                long actOutStamp = toMinuteStamp(outDate != null ? outDate : workYmd, outTime);
                if (actOutStamp < schEndStamp) {
                    acc.isEarly = true;
                }
            }
        }

        List<DailyAttdResponse.DailyItem> all = new ArrayList<>(accByUser.size());
        for (DailyAcc acc : accByUser.values()) {
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

        Map<String, MonthlyAcc> accByUser = new LinkedHashMap<>();
        for (MonthlyAttdRow r : rows) {
            MonthlyAcc acc = accByUser.computeIfAbsent(r.userCd(), k -> new MonthlyAcc(r));

            String inDate = blankToNull(r.actInDate());
            String inTime = blankToNull(r.actInTime());
            String outDate = blankToNull(r.actOutDate());
            String outTime = blankToNull(r.actOutTime());
            String workYmd = blankToNull(r.workYmd());
            String planStart = blankToNull(r.planStart());
            String planEnd = blankToNull(r.planEnd());
            int breakMin = r.planBreakMin() == null ? 0 : r.planBreakMin();

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

            // 지각(차수 단위 카운트): 실제출근일시 > 스케줄시작일시.
            if (inTime != null && planStart != null && workYmd != null) {
                long schStartStamp = toMinuteStamp(workYmd, planStart);
                long actInStamp = toMinuteStamp(inDate != null ? inDate : workYmd, inTime);
                if (actInStamp > schStartStamp) {
                    acc.lateCnt += 1;
                }
            }

            // 조퇴(차수 단위 카운트): 실제퇴근일시 < 스케줄종료일시(야간 종료 익일 보정).
            if (outTime != null && planEnd != null && workYmd != null) {
                String endYmd = workYmd;
                if (planStart != null && planEnd.compareTo(planStart) < 0) {
                    endYmd = ymdPlusDays(workYmd, 1);
                }
                long schEndStamp = toMinuteStamp(endYmd, planEnd);
                long actOutStamp = toMinuteStamp(outDate != null ? outDate : workYmd, outTime);
                if (actOutStamp < schEndStamp) {
                    acc.earlyCnt += 1;
                }
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

        MonthlyAcc(MonthlyAttdRow r) {
            this.userCd = r.userCd();
            this.userNm = r.userNm();
            this.nodeNm = r.nodeNm();
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
