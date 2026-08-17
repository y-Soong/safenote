package com.prafta.app.approval.admin.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.app.admin.common.scope.application.query.ScopedNodeQuery;
import com.prafta.app.leave.leaveflow.service.MultiDayLeavePlanner;
import com.prafta.app.admin.common.scope.mapper.AdminScopeMapper;
import com.prafta.app.approval.admin.application.param.ApprovalDetailParam;
import com.prafta.app.approval.admin.application.param.ApprovalHistoryParam;
import com.prafta.app.approval.admin.application.param.ApprovalPendingParam;
import com.prafta.app.approval.admin.application.param.ApprovalProcessParam;
import com.prafta.app.approval.admin.application.query.ApprovalScopeQuery;
import com.prafta.app.approval.admin.dto.response.ApprovalDetailResponse;
import com.prafta.app.approval.admin.dto.response.ApprovalHistoryResponse;
import com.prafta.app.approval.admin.dto.response.ApprovalPendingResponse;
import com.prafta.app.approval.admin.dto.response.ApprovalProcessResponse;
import com.prafta.app.approval.admin.mapper.AppAdminApprovalMapper;
import com.prafta.app.approval.admin.result.AttdSnapshotRow;
import com.prafta.app.approval.admin.result.DayScheduleRow;
import com.prafta.app.approval.admin.result.HistoryRow;
import com.prafta.app.approval.admin.result.LeaveBalanceRow;
import com.prafta.app.approval.admin.result.LeaveBodyRow;
import com.prafta.app.approval.admin.result.NeighborAttdSegmentRow;
import com.prafta.app.approval.admin.result.PendingCorrOtRow;
import com.prafta.app.approval.admin.result.PendingLeaveRow;
import com.prafta.app.approval.admin.result.ReqMetaRow;
import com.prafta.app.approval.admin.result.SchedBodyRow;
import com.prafta.app.approval.admin.service.AppAdminApprovalService;
import com.prafta.common.cmm.approval.mapper.ApprovalLineMapper;
import com.prafta.common.cmm.approval.vo.ApprovalStepVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AttdOverlapMessages;
import com.prafta.common.util.AttdOverlapUtils;
import com.prafta.common.util.AuthRoleUtils;
import com.prafta.common.util.DateTimeUtils;
import com.prafta.web.attd.attd07.application.model.OvertimeItemModel;
import com.prafta.web.attd.attd07.application.param.ApproveSchedModifyRequestParam;
import com.prafta.web.attd.attd07.application.param.RejectUserAttdRequestParam;
import com.prafta.web.attd.attd07.application.param.RejectUserOvertimeRequestParam;
import com.prafta.web.attd.attd07.application.param.UpdateUserAttdRequestParam;
import com.prafta.web.attd.attd07.application.param.UpdateUserOvertimeRequestParam;
import com.prafta.web.attd.attd07.service.Attd07Service;
import com.prafta.web.attd.leaveflow.application.param.LeaveApprovalActionParam;
import com.prafta.web.attd.leaveflow.service.LeaveFlowService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 001-P2: 앱 관리자 승인 관리 서비스 구현(읽기/게이트 — A-1/A-2/A-5).
 *
 * <p>web reqinbox/leaveflow/attd07 조회 SQL 을 app mapper 로 포팅하고, 식별자/스코프를 토큰·노드(Phase 1 CTE)로
 * 치환했다(comApi/webApi 직접 호출 없음). 연차 대기/이력은 web 동일하게 결재선(결재자 본인) 기반이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppAdminApprovalServiceImpl implements AppAdminApprovalService {

    private final AppAdminApprovalMapper mapper;
    private final AdminScopeMapper adminScopeMapper;       // 노드 자손 전개(재귀 CTE) — Phase 1 재사용
    private final ApprovalLineMapper approvalLineMapper;   // 연차 결재선(공통) — steps/IDOR 보조

    // A-3 처리(B4): web 처리 자산 재사용(SQL 엔진 복제 금지). comApi/webApi HTTP 호출이 아니라
    //   동일 JVM 의 @Service 빈을 같은 트랜잭션으로 호출한다(앱 leaveflow/근태 모듈이 이미 쓰는 패턴).
    private final Attd07Service attd07Service;             // 근태보정/초과 승인·반려(MGMT 반영+HIST)
    private final LeaveFlowService leaveFlowService;       // 연차 다단 결재 승인·반려(단계 진행+차감)

    // SYS032 요청유형
    private static final List<String> CORRECTION_TYPES = List.of("01", "02");
    private static final List<String> OVERTIME_TYPES = List.of("03", "04");
    private static final List<String> SCHEDULE_TYPES = List.of("10");   // PRAFTA-APP-029: 스케줄 수정 요청
    private static final List<String> HISTORY_STATUSES = List.of("02", "03", "04");

    private static final String WHOLE_SITE = "*";
    private static final String STEP_APPLIED = "01";

    // Fix1: ALL 그룹 메모리 병합 DoS 방어 — offset 절대 상한(초과 페이지는 빈 페이지로 응답).
    //   단일 그룹은 DB OFFSET/LIMIT 라 OOM 위험이 없으나, 일관성을 위해 동일 상한을 적용한다.
    private static final int MAX_OFFSET = 10000;

    private static final String G_ALL = "ALL";
    private static final String G_CORRECTION = "CORRECTION";
    private static final String G_OVERTIME = "OVERTIME";
    private static final String G_LEAVE = "LEAVE";
    private static final String G_SCHEDULE = "SCHEDULE";   // PRAFTA-APP-029: 스케줄 수정(10) 그룹

    // ============================ 스코프 산출 ============================

    /** 승인 관리 스코프: master=전사 / hr=사업장 / 노드관리자=자기노드+자손. safe 단독 ⛔. */
    private ScopeContext resolveScope(String cmpnyCd, String userCd, String siteCd, String authCd) {
        if (AuthRoleUtils.isAccessDenied(authCd)) {
            log.warn("승인 관리 접근 차단(권한 없음) - authCd={}", authCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
        // master = 전사(무필터). hr 은 전사가 아니라 사업장 스코프이므로 isCompanyWide 를 쓰지 않는다.
        if (AuthRoleUtils.AUTH_MASTER.equals(authCd)) {
            return new ScopeContext(true, null, false, null, Collections.emptyList());
        }
        if (AuthRoleUtils.AUTH_HR_MANAGER.equals(authCd)) {
            if (!StringUtils.hasText(siteCd)) {
                log.warn("승인 관리 hr 사업장 미지정 - userCd={}", userCd);
                throw new ApiException(AttdErrorCode.ATTD_403_002);
            }
            return new ScopeContext(false, siteCd, false, null, Collections.emptyList());
        }
        // safe 단독 또는 노드관리자 후보 → 노드 스코프. 노드관리자 아니면(빈 집합) 차단(safe ⛔).
        List<String> scopedNodeCds = Collections.emptyList();
        if (StringUtils.hasText(siteCd)) {
            scopedNodeCds = adminScopeMapper.selectScopedNodeCds(ScopedNodeQuery.of(cmpnyCd, siteCd, userCd));
        }
        if (scopedNodeCds == null || scopedNodeCds.isEmpty()) {
            log.warn("승인 관리 진입 권한 없음(노드관리자 아님/safe 단독) - authCd={}, siteCd={}", authCd, siteCd);
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
        return new ScopeContext(false, null, true, siteCd, scopedNodeCds);
    }

    private record ScopeContext(boolean master, String hrSiteCd,
            boolean useNodeScope, String nodeSiteCd, List<String> scopedNodeCds) {
    }

    private ApprovalScopeQuery scopeQuery(ScopeContext s, String cmpnyCd, List<String> reqTypes,
            List<String> statuses, String keyword, String startYmd, String endYmd, int offset, int limit) {
        return new ApprovalScopeQuery(cmpnyCd, reqTypes, statuses,
                s.master(), s.hrSiteCd(), s.useNodeScope(), s.nodeSiteCd(), s.scopedNodeCds(),
                keyword, startYmd, endYmd, offset, limit);
    }

    // ============================ A-1 대기 리스트 ============================

    @Override
    public ApprovalPendingResponse selectPending(ApprovalPendingParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        int offset = (param.page() - 1) * param.pageSize();
        int pageSize = param.pageSize();

        // 그룹별 counts(항상 산출).
        int corrCount = mapper.countPendingCorrOt(scopeQuery(scope, param.gvCmpnyCd(),
                CORRECTION_TYPES, List.of("01"), param.keyword(), null, null, 0, 0));
        int otCount = mapper.countPendingCorrOt(scopeQuery(scope, param.gvCmpnyCd(),
                OVERTIME_TYPES, List.of("01"), param.keyword(), null, null, 0, 0));
        // prafta-leavemulti: groupLeave=Y 면 연차 건수를 "카드 수(묶음 수)"로 산출한다.
        //   목록 아이템도 접힌 카드 1건이므로 counts/totalCount/hasMore 가 서로 정합해진다.
        int leaveCount = param.groupLeave()
                ? mapper.countPendingLeaveGroups(param.gvCmpnyCd(), param.gvUserCd(), param.keyword())
                : mapper.countPendingLeave(param.gvCmpnyCd(), param.gvUserCd(), param.keyword());
        int schedCount = mapper.countPendingCorrOt(scopeQuery(scope, param.gvCmpnyCd(),
                SCHEDULE_TYPES, List.of("01"), param.keyword(), null, null, 0, 0));
        int allCount = corrCount + otCount + leaveCount + schedCount;

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put(G_ALL, allCount);
        counts.put(G_CORRECTION, corrCount);
        counts.put(G_OVERTIME, otCount);
        counts.put(G_LEAVE, leaveCount);
        counts.put(G_SCHEDULE, schedCount);

        // Fix1: offset 상한 초과 시 빈 페이지(counts/총건수는 유지) — ALL 메모리 병합 폭주/OOM 차단.
        if (offset > MAX_OFFSET) {
            log.warn("승인 대기 offset 상한 초과 — 빈 페이지 응답. cmpnyCd={}, group={}, page={}, offset={}",
                    param.gvCmpnyCd(), param.group(), param.page(), offset);
            return ApprovalPendingResponse.builder()
                    .items(Collections.emptyList())
                    .counts(counts)
                    .totalCount(counts.getOrDefault(param.group(), allCount))
                    .hasMore(false)
                    .build();
        }

        List<ApprovalPendingResponse.PendingItem> items;
        int totalCount;
        boolean hasMore;

        if (G_CORRECTION.equals(param.group())) {
            List<PendingCorrOtRow> rows = mapper.selectPendingCorrOt(scopeQuery(scope, param.gvCmpnyCd(),
                    CORRECTION_TYPES, List.of("01"), param.keyword(), null, null, offset, pageSize));
            items = mapCorrOt(rows, param.gvUserCd());
            totalCount = corrCount;
            hasMore = offset + items.size() < totalCount;
        } else if (G_OVERTIME.equals(param.group())) {
            List<PendingCorrOtRow> rows = mapper.selectPendingCorrOt(scopeQuery(scope, param.gvCmpnyCd(),
                    OVERTIME_TYPES, List.of("01"), param.keyword(), null, null, offset, pageSize));
            items = mapCorrOt(rows, param.gvUserCd());
            totalCount = otCount;
            hasMore = offset + items.size() < totalCount;
        } else if (G_LEAVE.equals(param.group())) {
            items = selectLeaveItems(param, offset, pageSize);
            totalCount = leaveCount;
            hasMore = offset + items.size() < totalCount;
        } else if (G_SCHEDULE.equals(param.group())) {
            // PRAFTA-APP-029: 스케줄 수정(10) 대기 — corrOt 스코프/매핑 재사용(groupOf 가 SCHEDULE 산출).
            List<PendingCorrOtRow> rows = mapper.selectPendingCorrOt(scopeQuery(scope, param.gvCmpnyCd(),
                    SCHEDULE_TYPES, List.of("01"), param.keyword(), null, null, offset, pageSize));
            items = mapCorrOt(rows, param.gvUserCd());
            totalCount = schedCount;
            hasMore = offset + items.size() < totalCount;
        } else {
            // ALL: 3개 소스를 offset+pageSize+1 까지 모아 reqDate DESC 병합 후 슬라이스.
            //   Fix1: fetch 를 절대 상한으로 캡(소스당 최대 ~MAX_OFFSET+pageSize 행) → 메모리 적재 폭주 차단.
            int fetch = Math.min(offset + pageSize + 1, MAX_OFFSET + pageSize + 1);
            List<ApprovalPendingResponse.PendingItem> merged = new ArrayList<>();
            merged.addAll(mapCorrOt(mapper.selectPendingCorrOt(scopeQuery(scope, param.gvCmpnyCd(),
                    CORRECTION_TYPES, List.of("01"), param.keyword(), null, null, 0, fetch)), param.gvUserCd()));
            merged.addAll(mapCorrOt(mapper.selectPendingCorrOt(scopeQuery(scope, param.gvCmpnyCd(),
                    OVERTIME_TYPES, List.of("01"), param.keyword(), null, null, 0, fetch)), param.gvUserCd()));
            merged.addAll(mapCorrOt(mapper.selectPendingCorrOt(scopeQuery(scope, param.gvCmpnyCd(),
                    SCHEDULE_TYPES, List.of("01"), param.keyword(), null, null, 0, fetch)), param.gvUserCd()));
            // prafta-leavemulti: groupLeave=Y 면 접힌 아이템 1건으로 병합에 참여한다 →
            //   ALL 슬라이스 경계에서 묶음이 갈리는 경로가 함께 소멸한다.
            //   ★단 그룹 모드의 limit 은 "묶음 수"라 적재 행 수가 묶음 크기만큼 곱해진다(security M-1).
            //     Fix1 의 fetch 캡은 "행 수" 상한을 노린 값이므로, 그대로 넘기면 한 요청이
            //     fetch × 최대 묶음 크기(MAX_RANGE_DAYS=62) 행까지 적재하고 행마다 복호화 함수와
            //     상관 서브쿼리가 돈다. 연차 소스에만 묶음 상한으로 나눈 fetch 를 준다.
            //     (하한 pageSize+1 은 첫 페이지 슬라이스와 hasMore 판정에 필요한 최소치다.)
            int leaveFetch = param.groupLeave()
                    ? Math.max(pageSize + 1, fetch / MultiDayLeavePlanner.MAX_RANGE_DAYS)
                    : fetch;
            merged.addAll(selectLeaveItems(param, 0, leaveFetch));
            merged.sort(Comparator.comparing(
                    ApprovalPendingResponse.PendingItem::getReqDate,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            items = slice(merged, offset, pageSize);
            totalCount = allCount;
            hasMore = merged.size() > offset + pageSize;
        }

        log.info("앱 관리자 승인 대기 리스트 조회 - cmpnyCd={}, group={}, count={}, totalCount={}",
                param.gvCmpnyCd(), param.group(), items.size(), totalCount);

        return ApprovalPendingResponse.builder()
                .items(items)
                .counts(counts)
                .totalCount(totalCount)
                .hasMore(hasMore)
                .build();
    }

    private List<ApprovalPendingResponse.PendingItem> mapCorrOt(List<PendingCorrOtRow> rows, String tokenUserCd) {
        List<ApprovalPendingResponse.PendingItem> list = new ArrayList<>();
        if (rows == null) {
            return list;
        }
        for (PendingCorrOtRow r : rows) {
            String group = groupOf(r.reqType());
            // PRAFTA-APP-029 후속: 스케줄 수정 카드 요약을 SCH_CD 코드(예: 00011)가 아닌 실제 시각 range 로 표기한다.
            //   요청 스케줄 시각은 대기 쿼리(selectPendingCorrOt)가 근무일 기준 유효버전으로 미리 조인해 내려준다(N+1 제거).
            //   비스케줄(근태보정/초과) 행은 sch*Time 이 전부 null → schedRange=null 로 기존 표기 그대로.
            String schedRange = null;
            if (G_SCHEDULE.equals(group)) {
                // PRAFTA-FIXEDOT-2(표기): 고정연장(전방·후방) 구분 suffix — 없으면 빈 문자열(기존 표기 불변).
                schedRange = appendFixedOtText(
                        schedRangeText(
                                r.schFstStrTime(), r.schFstEndTime(), r.schSecStrTime(), r.schSecEndTime()),
                        r.schPreFixedOtStrTime(), r.schPreFixedOtEndTime(),
                        r.schFixedOtStrTime(), r.schFixedOtEndTime());
            }
            list.add(ApprovalPendingResponse.PendingItem.builder()
                    .reqId(r.reqId())
                    .group(group)
                    .reqType(r.reqType())
                    .reqTypeNm(reqTypeNm(r.reqType()))
                    .requesterUserNm(r.userNm())
                    .requesterUserCd(r.userCd())
                    .nodeNm(r.nodeNm())
                    .targetYmd(r.workYmd())
                    .summaryLines(summaryCorrOt(group, r, schedRange))
                    .reqDate(r.reqDate())
                    .deadlineDday(null)         // A1(마감 기준일) 미확정 → 보류
                    .deadlineLevel(null)
                    .selfYn(r.userCd() != null && r.userCd().equals(tokenUserCd) ? "Y" : "N")
                    .lockedYn(false)
                    .lockedByNm(null)
                    .approvalStep(null)
                    .build());
        }
        return list;
    }

    private List<ApprovalPendingResponse.PendingItem> mapLeave(List<PendingLeaveRow> rows) {
        List<ApprovalPendingResponse.PendingItem> list = new ArrayList<>();
        if (rows == null) {
            return list;
        }
        for (PendingLeaveRow r : rows) {
            list.add(ApprovalPendingResponse.PendingItem.builder()
                    .reqId(r.reqId())
                    .group(G_LEAVE)
                    .reqType(r.reqType())
                    .reqTypeNm(reqTypeNm(r.reqType()))
                    .requesterUserNm(r.requesterUserNm())
                    .requesterUserCd(r.requesterUserCd())
                    .nodeNm(r.nodeNm())
                    .targetYmd(r.workYmd())
                    .summaryLines(summaryLeave(r))
                    .reqDate(r.reqDate())
                    .deadlineDday(null)
                    .deadlineLevel(null)
                    .selfYn(r.selfYn())
                    .lockedYn(false)
                    .lockedByNm(null)
                    .approvalStep(r.approvalStep())
                    // 가불표시-02: 구조화 필드로 전달(summaryLines 문자열 오염 금지 — FE 가 칩 렌더).
                    .borrowDays(r.borrowDays())
                    .build());
        }
        return list;
    }

    /**
     * prafta-leavemulti: 연차 대기 아이템 산출(단건 페이징 / 묶음 접기 분기).
     *
     * <p>{@code groupLeave=false} 면 종전 경로(selectPendingLeave + mapLeave)를 그대로 탄다 — 구버전 앱 무회귀.
     *
     * @param offset 그룹 모드에서는 "묶음 기준" offset, 아니면 행 기준 offset.
     * @param limit  그룹 모드에서는 "묶음 기준" 개수, 아니면 행 기준 개수.
     */
    private List<ApprovalPendingResponse.PendingItem> selectLeaveItems(
            ApprovalPendingParam param, int offset, int limit) {
        if (param.groupLeave()) {
            return mapLeaveGrouped(mapper.selectPendingLeaveGrouped(
                    param.gvCmpnyCd(), param.gvUserCd(), param.keyword(), offset, limit));
        }
        return mapLeave(mapper.selectPendingLeave(
                param.gvCmpnyCd(), param.gvUserCd(), param.keyword(), offset, limit));
    }

    /**
     * prafta-leavemulti: 연차 기간(From-To) 신청 묶음을 카드 1건으로 접는다.
     *
     * <p>묶음키 = {@code COALESCE(LEAVE_GROUP_ID, REQ_ID)}. 단일일 신청(LEAVE_GROUP_ID=null)은
     * 종전 {@code mapLeave} 와 완전히 동일한 아이템을 만들고 묶음 필드는 전부 null 로 둔다.
     *
     * <p>대표행은 근무일 최소(첫날) — 카드 본문을 눌러 상세로 가면 첫날 건이 열린다.
     * ★{@code groupDays} 는 LEAVE_DAYS 만 합산한다. LEAVE_MINUTES 는 분할차감 시 첫 행에만 총량을 싣는
     * 불변식이라 합산하면 틀린 값이 된다.
     */
    private List<ApprovalPendingResponse.PendingItem> mapLeaveGrouped(List<PendingLeaveRow> rows) {
        List<ApprovalPendingResponse.PendingItem> list = new ArrayList<>();
        if (rows == null || rows.isEmpty()) {
            return list;
        }
        // 쿼리 정렬(그룹 → 근무일 오름차순)을 그대로 보존하기 위해 LinkedHashMap 사용.
        Map<String, List<PendingLeaveRow>> grouped = new LinkedHashMap<>();
        for (PendingLeaveRow r : rows) {
            String key = StringUtils.hasText(r.leaveGroupId()) ? r.leaveGroupId() : r.reqId();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        for (Map.Entry<String, List<PendingLeaveRow>> e : grouped.entrySet()) {
            List<PendingLeaveRow> members = e.getValue();
            PendingLeaveRow head = members.get(0);

            // 단일일 신청(묶음 아님) — 종전 단건 아이템과 동일(묶음 필드 전부 null).
            if (!StringUtils.hasText(head.leaveGroupId())) {
                list.addAll(mapLeave(members));
                continue;
            }

            // 대표행 = 근무일 최소(첫날). 쿼리가 이미 WORK_YMD ASC 지만 방어적으로 재산출한다.
            PendingLeaveRow rep = head;
            String fromYmd = head.workYmd();
            String toYmd = head.workYmd();
            BigDecimal daysSum = BigDecimal.ZERO;
            BigDecimal borrowSum = BigDecimal.ZERO;
            boolean anySelf = false;
            List<ApprovalPendingResponse.PendingItem.GroupItem> children = new ArrayList<>(members.size());

            for (PendingLeaveRow m : members) {
                if (m.workYmd() != null) {
                    if (fromYmd == null || m.workYmd().compareTo(fromYmd) < 0) {
                        fromYmd = m.workYmd();
                        rep = m;
                    }
                    if (toYmd == null || m.workYmd().compareTo(toYmd) > 0) {
                        toYmd = m.workYmd();
                    }
                }
                if (m.leaveDays() != null) {
                    daysSum = daysSum.add(m.leaveDays());
                }
                if (m.borrowDays() != null) {
                    borrowSum = borrowSum.add(m.borrowDays());
                }
                if ("Y".equals(m.selfYn())) {
                    anySelf = true;
                }
                children.add(ApprovalPendingResponse.PendingItem.GroupItem.builder()
                        .reqId(m.reqId())
                        .approvalStep(m.approvalStep())
                        .targetYmd(m.workYmd())
                        .leaveDays(m.leaveDays())
                        .unitNm(m.unitNm())
                        .selfYn(m.selfYn())
                        .borrowDays(m.borrowDays())
                        .summaryLines(summaryLeave(m))
                        .build());
            }

            list.add(ApprovalPendingResponse.PendingItem.builder()
                    .reqId(rep.reqId())
                    .group(G_LEAVE)
                    .reqType(rep.reqType())
                    .reqTypeNm(reqTypeNm(rep.reqType()))
                    .requesterUserNm(rep.requesterUserNm())
                    .requesterUserCd(rep.requesterUserCd())
                    .nodeNm(rep.nodeNm())
                    .targetYmd(rep.workYmd())
                    .summaryLines(summaryLeave(rep))
                    .reqDate(rep.reqDate())
                    .deadlineDday(null)
                    .deadlineLevel(null)
                    .selfYn(anySelf ? "Y" : "N")
                    .lockedYn(false)
                    .lockedByNm(null)
                    .approvalStep(rep.approvalStep())
                    .borrowDays(borrowSum)
                    .leaveGroupId(e.getKey())
                    .groupCount(members.size())
                    .groupFromYmd(fromYmd)
                    .groupToYmd(toYmd)
                    .groupDays(daysSum)
                    .groupItems(children)
                    .build());
        }
        return list;
    }

    // ============================ A-2 상세 ============================

    @Override
    public ApprovalDetailResponse selectDetail(ApprovalDetailParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        ReqMetaRow meta = mapper.selectReqMeta(param.gvCmpnyCd(), param.reqId());
        if (meta == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_001);
        }
        String group = groupOf(meta.reqType());
        if (group == null) {
            // 07/08/09/임의 등 미지원 유형은 상세를 열지 않는다(SCHEDULE='10' 은 PRAFTA-APP-029 로 지원).
            throw new ApiException(AttdErrorCode.ATTD_400_006);
        }

        // IDOR: 토큰 스코프 내인지 서버 재검증(불일치 403). 연차는 결재선 참여(결재자 본인)도 허용.
        if (!isInScope(scope, meta) && !(G_LEAVE.equals(group) && isApproverOf(param.gvCmpnyCd(), param.reqId(), param.gvUserCd()))) {
            log.warn("승인 상세 스코프 위반(IDOR 차단) - userCd={}, reqId={}, reqSite={}, reqNode={}",
                    param.gvUserCd(), param.reqId(), meta.siteCd(), meta.nodeCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        // 결재선(연차 다단) — 현재 단계/steps.
        List<ApprovalStepVO> steps = approvalLineMapper.selectApprovalLineByReqId(param.gvCmpnyCd(), param.reqId());
        Integer currentStep = currentAppliedStep(steps);

        // prafta-leavemulti(정책 변경 2026-08-16, 사용자 확정): 관리자 본인이 낸 요청을 스스로 승인해도 무방하다.
        //   종전에는 앱만 본인결재를 무조건 차단(gate.selfBlockedYn=true → 버튼 비활성)했고 웹은 차단하지 않아
        //   앱이 더 엄격했다. 화면에 "본인 결재 불가" 배너가 남지 않도록 표시 플래그도 함께 내린다.
        //   근태보정/초과/스케줄은 위임 대상 web Attd07Service 가 노드 정책(SELF_ATTD_APPRV_YN)으로 여전히
        //   판정하므로(403_001/403_003), 여기서는 막지 않고 서버 처리 시점 메시지로 안내한다
        //   (:disabled 로 감추면 "눌러도 아무 일이 없다"로 오인된다).
        boolean selfRequested = meta.userCd() != null && meta.userCd().equals(param.gvUserCd());
        boolean closed = isClosed(param.gvCmpnyCd(), meta);
        boolean conflict = !"01".equals(meta.reqStatus());
        boolean canProcess = !closed && !conflict;

        ApprovalDetailResponse.Gate gate = ApprovalDetailResponse.Gate.builder()
                .canProcess(canProcess)
                // 본인결재 차단 해제 — 항상 false(응답 계약은 유지, 구버전 앱도 배너 미표시).
                .selfBlockedYn(false)
                .closedYn(closed)
                .lockedYn(false)
                .lockedByNm(null)
                .conflictYn(conflict)
                .conflictMsg(conflict ? "이미 처리된 요청입니다." : null)
                .build();

        ApprovalDetailResponse.Meta metaOut = ApprovalDetailResponse.Meta.builder()
                .reqId(meta.reqId())
                .group(group)
                .reqType(meta.reqType())
                .reqTypeNm(reqTypeNm(meta.reqType()))
                .reqStatus(meta.reqStatus())
                .requesterUserNm(meta.requesterUserNm())
                .requesterUserCd(meta.userCd())
                .nodeNm(meta.nodeNm())
                .reqDate(meta.reqDate())
                .targetYmd(meta.workYmd())
                .deadlineDday(null)
                .deadlineLevel(null)
                .approvalStep(currentStep)
                .build();

        Map<String, Object> body = buildBody(param.gvCmpnyCd(), group, meta, steps);

        log.info("앱 관리자 승인 상세 조회 - reqId={}, group={}, canProcess={}, self={}, closed={}, conflict={}",
                meta.reqId(), group, canProcess, selfRequested, closed, conflict);

        return ApprovalDetailResponse.builder()
                .meta(metaOut)
                .gate(gate)
                .body(body)
                .reason(meta.reqReason())
                .attachments(Collections.emptyList())   // 현행 스키마에 요청 첨부 테이블 없음
                .build();
    }

    private Map<String, Object> buildBody(String cmpnyCd, String group, ReqMetaRow meta, List<ApprovalStepVO> steps) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (G_CORRECTION.equals(group)) {
            Map<String, Object> before = new LinkedHashMap<>();
            if (StringUtils.hasText(meta.targetId())) {
                AttdSnapshotRow snap = mapper.selectAttdSnapshot(cmpnyCd, meta.targetId());
                if (snap != null) {
                    before.put("checkIn", joinDt(snap.checkInDate(), snap.checkInTime()));
                    before.put("checkOut", joinDt(snap.checkOutDate(), snap.checkOutTime()));
                }
            }
            Map<String, Object> after = new LinkedHashMap<>();
            after.put("checkIn", joinDt(meta.startDate(), meta.startTime()));
            after.put("checkOut", joinDt(meta.endDate(), meta.endTime()));
            body.put("correctionTypeNm", null);   // 보정유형 코드 미저장(요청테이블) — 후속
            body.put("before", before);
            body.put("after", after);
            // 겹침가드 개선(2026-08-06): 앞뒤 근무일(D-1/D+1) 근태 구간. 이웃 근무일의 미마감 근태가
            //   이 보정 승인을 막는 원인일 때 관리자가 화면에서 특정할 수 있게 한다(정책서 attd §7.6).
            //   표시 문자열·status 는 서버 완성값(프론트 재판정 금지). 0건이면 빈 리스트.
            body.put("neighborSegments",
                    buildNeighborSegments(cmpnyCd, meta.siteCd(), meta.userCd(), meta.workYmd()));
        } else if (G_OVERTIME.equals(group)) {
            Map<String, Object> claimed = new LinkedHashMap<>();
            claimed.put("startAt", joinDt(meta.startDate(), meta.startTime()));
            claimed.put("endAt", joinDt(meta.endDate(), meta.endTime()));
            body.put("claimMode", null);           // 사후/사전 구분은 마감/일자 기준(A1) — 후속
            body.put("claimed", claimed);
            body.put("systemCalc", null);          // 시스템 계산값은 OT 윈도우 엔진(A-3/R3) 포팅 후 제공
            body.put("approved", null);
            // 2026-08-17: 승인 판단 컨텍스트 — 당일 스케줄/고정연장/실근태(표시 전용, 서버 완성 문자열).
            //   상신 구간만으로는 "이 초과가 스케줄·실근태와 정합인지" 판단이 어렵다는 현장 피드백 반영.
            //   상세 EP 진입부의 scope/node 권한 가드를 승계하고 쿼리 WHERE 로 스코프를 이중 차단한다.
            body.put("dayContext", buildOvertimeDayContext(cmpnyCd, meta.siteCd(), meta.userCd(), meta.workYmd()));
        } else if (G_LEAVE.equals(group)) {
            LeaveBodyRow lb = mapper.selectLeaveBody(cmpnyCd, meta.reqId(), meta.targetId());
            if (lb != null) {
                body.put("leaveCd", lb.leaveCd());
                body.put("leaveNm", lb.leaveNm());
                body.put("paidYn", null);          // 유급여부 컬럼 미확정(DDL) — 후속
                body.put("unitNm", lb.unitNm());
                // 가불표시-02: 가불 충당 일수(0 이상). FE 는 0 초과일 때만 "가불 N일" 행 표시.
                body.put("borrowDays", lb.borrowDays());
                Map<String, Object> applied = new LinkedHashMap<>();
                applied.put("startDate", lb.startDate());
                applied.put("startTime", lb.startTime());
                applied.put("endDate", lb.endDate());
                applied.put("endTime", lb.endTime());
                applied.put("leaveDays", lb.leaveDays());
                body.put("appliedRange", applied);
                LeaveBalanceRow bal = mapper.selectLeaveBalance(cmpnyCd, meta.userCd(), lb.leaveCd());
                Map<String, Object> balance = new LinkedHashMap<>();
                balance.put("granted", bal != null ? bal.granted() : BigDecimal.ZERO);
                balance.put("used", bal != null ? bal.used() : BigDecimal.ZERO);
                balance.put("remain", bal != null ? bal.remain() : BigDecimal.ZERO);
                body.put("balance", balance);
            }
            body.put("steps", buildSteps(steps));
            body.put("hrFinalYn", null);
        } else if (G_SCHEDULE.equals(group)) {
            // PRAFTA-APP-029(D6): 현재(WORK_PLAN_CD) → 요청(REQ.SCH_CD) 스케줄 비교. CORRECTION before/after 패턴과 동형.
            //   미처리 대기 건이므로 before=현재(승인 전), after=요청(승인 시 반영). 현재 work_plan 없으면 before=null.
            SchedBodyRow sb = mapper.selectSchedBody(
                    cmpnyCd, meta.siteCd(), meta.userCd(), meta.workYmd(), meta.schCd());
            Map<String, Object> before = null;
            Map<String, Object> after = null;
            if (sb != null) {
                if (StringUtils.hasText(sb.curSchCd())) {
                    before = new LinkedHashMap<>();
                    before.put("schCd", sb.curSchCd());
                    before.put("schNm", sb.curSchNo());
                    // PRAFTA-FIXEDOT-2(표기): 현재 스케줄 고정연장 구분 suffix(없으면 기존 표기 그대로).
                    before.put("rangeText",
                            appendFixedOtText(
                                    schedRangeText(sb.curFstStrTime(), sb.curFstEndTime(), sb.curSecStrTime(), sb.curSecEndTime()),
                                    sb.curPreFixedOtStrTime(), sb.curPreFixedOtEndTime(),
                                    sb.curFixedOtStrTime(), sb.curFixedOtEndTime()));
                }
                after = new LinkedHashMap<>();
                after.put("schCd", sb.reqSchCd());
                after.put("schNm", sb.reqSchNo());
                // PRAFTA-FIXEDOT-2(표기): 요청 스케줄 고정연장 구분 suffix — 고정연장만 다른 변경도
                //   승인자가 before/after 차이를 볼 수 있다.
                after.put("rangeText",
                        appendFixedOtText(
                                schedRangeText(sb.reqFstStrTime(), sb.reqFstEndTime(), sb.reqSecStrTime(), sb.reqSecEndTime()),
                                sb.reqPreFixedOtStrTime(), sb.reqPreFixedOtEndTime(),
                                sb.reqFixedOtStrTime(), sb.reqFixedOtEndTime()));
            } else if (StringUtils.hasText(meta.schCd())) {
                // 요청 SCH_CD 가 마스터에 미존재(데이터 이상) — 코드만 노출(시각 미상).
                after = new LinkedHashMap<>();
                after.put("schCd", meta.schCd());
                after.put("schNm", null);
                after.put("rangeText", null);
            }
            body.put("before", before);
            body.put("after", after);
        }
        return body;
    }

    /**
     * 앞뒤 근무일(D-1 / D+1) 근태 구간 목록(표시 전용, 겹침가드 개선 2026-08-06).
     *
     * <p>웹 일자상세(daily-attd-details)의 {@code neighborAttdSegmentList} 와 동일한 형상으로 내려준다.
     *   상태(status)는 각 행 자기 근무일 기준 판정(CLOSED/OPEN/CORRUPT)이며 서버 단일 출처다.
     *   상세 EP 진입부의 scope/node 권한 가드를 승계하고, 쿼리 WHERE 로 회사/사업장/사용자 스코프를 이중 차단한다.
     *   ATTD_ID 는 화면에서 쓰지 않으므로 내려보내지 않는다(IDOR 표면 축소).
     */
    private List<Map<String, Object>> buildNeighborSegments(String cmpnyCd, String siteCd,
                                                            String userCd, String workYmd) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!StringUtils.hasText(siteCd) || !StringUtils.hasText(userCd) || !StringUtils.hasText(workYmd)) {
            return out;
        }
        String fromYmd = DateTimeUtils.plusDays(workYmd, -1);
        String toYmd = DateTimeUtils.plusDays(workYmd, 1);
        if (fromYmd == null || toYmd == null) {
            return out; // 근무일 형식 오류 fail-safe(표시 생략)
        }
        List<NeighborAttdSegmentRow> rows =
                mapper.selectNeighborAttdSegments(cmpnyCd, siteCd, userCd, fromYmd, toYmd, workYmd);
        if (rows == null || rows.isEmpty()) {
            return out;
        }
        boolean corruptFound = false;
        for (NeighborAttdSegmentRow row : rows) {
            AttdOverlapUtils.SegmentKind kind = AttdOverlapUtils.classify(
                    row.workYmd(), row.checkInDate(), row.checkInTime(), row.checkOutDate(), row.checkOutTime());
            if (kind == null) {
                kind = AttdOverlapUtils.SegmentKind.CORRUPT; // 출근 stamp 산출 불가(스키마상 NOT NULL — 이론상 미도달)
            }
            if (kind == AttdOverlapUtils.SegmentKind.CORRUPT) {
                corruptFound = true;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("workYmd", row.workYmd());
            item.put("dayLabel", AttdOverlapMessages.dayLabel(workYmd, row.workYmd()));
            item.put("workSeq", row.workSeq());
            item.put("seqLabel", AttdOverlapMessages.seqLabel(row.workSeq()));
            item.put("checkInText", AttdOverlapMessages.stampText(row.workYmd(), row.checkInDate(), row.checkInTime()));
            item.put("checkOutText", AttdOverlapMessages.checkOutTextFor(kind, row.workYmd(), row.checkOutDate(), row.checkOutTime()));
            item.put("status", kind.name());
            out.add(item);
        }
        if (corruptFound) {
            log.warn("[겹침가드] 앞뒤 근무일 구간에 퇴근시각 미성립(CORRUPT) 행 포함. siteCd={}, workYmd={}", siteCd, workYmd);
        }
        return out;
    }

    /**
     * PRAFTA-APP-029(D6): 스케줄 시각 range 표기. 1구간 "07:00~15:00", 2구간 있으면 " / 16:00~20:00" 부착.
     * fmtHm(HHMM→HH:MM) 재사용. 1구간 시각이 모두 비면 null.
     */
    /**
     * PRAFTA-FIXEDOT-2(표기): 스케줄 range 문자열 뒤에 고정연장(전방·후방) 구분 suffix 를 붙인다.
     * 예) "09:00~18:00" → "09:00~18:00 + 고정연장 18:00~20:00" (전방+후방이면 " · " 로 나열).
     * 고정연장 미설정이면 base 그대로 반환(기존 표기 완전 불변 — 무회귀). base 가 null 이면 null 유지.
     */
    private String appendFixedOtText(String base, String preStr, String preEnd,
                                     String rearStr, String rearEnd) {
        if (base == null) {
            return null;
        }
        String parts = fixedOtPartsText(preStr, preEnd, rearStr, rearEnd);
        if (parts == null) {
            return base;
        }
        return base + " + 고정연장 " + parts;
    }

    /**
     * 고정연장(전방·후방) 구간 문자열 — "07:00~08:00 · 18:00~20:00" 형태(한쪽만 있으면 그 구간만).
     * 미설정이면 null. appendFixedOtText(suffix 표기)와 초과근무 dayContext(독립 행 표기)가 공용한다.
     */
    private String fixedOtPartsText(String preStr, String preEnd, String rearStr, String rearEnd) {
        StringBuilder parts = new StringBuilder();
        if (StringUtils.hasText(preStr) && StringUtils.hasText(preEnd)) {
            parts.append(fmtHm(preStr)).append("~").append(fmtHm(preEnd));
        }
        if (StringUtils.hasText(rearStr) && StringUtils.hasText(rearEnd)) {
            if (parts.length() > 0) {
                parts.append(" · ");
            }
            parts.append(fmtHm(rearStr)).append("~").append(fmtHm(rearEnd));
        }
        return parts.length() == 0 ? null : parts.toString();
    }

    /**
     * 초과근무 승인 상세 — 당일 컨텍스트(스케줄/고정연장/실근태) 표시 문자열 묶음 (2026-08-17 요청).
     *
     * <p>표시 전용이며 승인 판정(gate)·검증에는 관여하지 않는다. 값이 없는 항목은 null 로 내려
     * FE 가 '-' 로 표기한다(스케줄 미배정·근태 미기록도 그 자체가 판단 정보다 — 행을 숨기지 않는다).
     * <ul>
     *   <li>scheduleText — 당일 배정 스케줄 소정 구간("08:00~17:00", 2구간이면 " / " 나열)</li>
     *   <li>schNm — 스케줄 번호(SCH_NO)</li>
     *   <li>fixedOtText — 스케줄의 고정연장(전방·후방) 구간</li>
     *   <li>actualText — 당일 실근태 구간("08:01 ~ 19:32", 2건이면 " / " 나열, 퇴근 미기록은 "미퇴근",
     *       교차일 시각은 stampText 규칙("MM-DD HH:MM")을 따른다)</li>
     * </ul>
     */
    private Map<String, Object> buildOvertimeDayContext(String cmpnyCd, String siteCd,
                                                        String userCd, String workYmd) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        String scheduleText = null;
        String schNm = null;
        String fixedOtText = null;
        if (StringUtils.hasText(siteCd) && StringUtils.hasText(userCd) && StringUtils.hasText(workYmd)) {
            DayScheduleRow ds = mapper.selectDayScheduleBody(cmpnyCd, siteCd, userCd, workYmd);
            if (ds != null && StringUtils.hasText(ds.schCd())) {
                schNm = ds.schNo();
                scheduleText = schedRangeText(ds.fstStrTime(), ds.fstEndTime(), ds.secStrTime(), ds.secEndTime());
                fixedOtText = fixedOtPartsText(ds.preFixedOtStrTime(), ds.preFixedOtEndTime(),
                        ds.fixedOtStrTime(), ds.fixedOtEndTime());
            }
        }
        ctx.put("schNm", schNm);
        ctx.put("scheduleText", scheduleText);
        ctx.put("fixedOtText", fixedOtText);
        ctx.put("actualText", buildDayActualText(cmpnyCd, siteCd, userCd, workYmd));
        return ctx;
    }

    /** 당일 실근태 구간 표시 문자열. 근태 행이 없으면 null(FE '-' 표기). */
    private String buildDayActualText(String cmpnyCd, String siteCd, String userCd, String workYmd) {
        if (!StringUtils.hasText(siteCd) || !StringUtils.hasText(userCd) || !StringUtils.hasText(workYmd)) {
            return null;
        }
        List<NeighborAttdSegmentRow> rows = mapper.selectDayAttdSegments(cmpnyCd, siteCd, userCd, workYmd);
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (NeighborAttdSegmentRow row : rows) {
            String in = AttdOverlapMessages.stampText(workYmd, row.checkInDate(), row.checkInTime());
            String out = AttdOverlapMessages.stampText(workYmd, row.checkOutDate(), row.checkOutTime());
            if (in == null && out == null) {
                continue; // 시각 미성립 행(이론상 미도달) — 표시 생략
            }
            parts.add((in == null ? "-" : in) + " ~ " + (out == null ? "미퇴근" : out));
        }
        return parts.isEmpty() ? null : String.join(" / ", parts);
    }

    private String schedRangeText(String fstStr, String fstEnd, String secStr, String secEnd) {
        String fs = fmtHm(fstStr);
        String fe = fmtHm(fstEnd);
        if (fs.isEmpty() && fe.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(fs).append("~").append(fe);
        String ss = fmtHm(secStr);
        String se = fmtHm(secEnd);
        if (!ss.isEmpty() || !se.isEmpty()) {
            sb.append(" / ").append(ss).append("~").append(se);
        }
        return sb.toString();
    }

    private List<Map<String, Object>> buildSteps(List<ApprovalStepVO> steps) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (steps == null) {
            return out;
        }
        for (ApprovalStepVO s : steps) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("approvalStep", s.getApprovalStep());
            m.put("approverUserNm", s.getApproverUserNm());
            m.put("approvalStatus", s.getApprovalStatus());
            m.put("approvalStatusNm", s.getApprovalStatusNm());
            m.put("approvalComment", s.getApprovalComment());
            m.put("approvalDate", s.getApprovalDate());
            out.add(m);
        }
        return out;
    }

    // ============================ A-5 이력 ============================

    @Override
    public ApprovalHistoryResponse selectHistory(ApprovalHistoryParam param) {
        ScopeContext scope = resolveScope(param.gvCmpnyCd(), param.gvUserCd(), param.gvSiteCd(), param.gvAuthCd());

        int offset = (param.page() - 1) * param.pageSize();
        int pageSize = param.pageSize();

        // Fix1: offset 상한 초과 시 빈 페이지(총건수는 COUNT 로만 산출) — ALL 메모리 병합 폭주/OOM 차단.
        if (offset > MAX_OFFSET) {
            log.warn("승인 이력 offset 상한 초과 — 빈 페이지 응답. cmpnyCd={}, group={}, page={}, offset={}",
                    param.gvCmpnyCd(), param.group(), param.page(), offset);
            return ApprovalHistoryResponse.builder()
                    .items(Collections.emptyList())
                    .totalCount(historyTotalCount(scope, param))
                    .hasMore(false)
                    .build();
        }

        List<ApprovalHistoryResponse.HistoryItem> items;
        int totalCount;
        boolean hasMore;

        if (G_CORRECTION.equals(param.group())) {
            ApprovalScopeQuery q = scopeQuery(scope, param.gvCmpnyCd(), CORRECTION_TYPES, HISTORY_STATUSES,
                    param.keyword(), param.startDate(), param.endDate(), offset, pageSize);
            items = mapHistory(mapper.selectHistoryCorrOt(q));
            totalCount = mapper.countHistoryCorrOt(q);
            hasMore = offset + items.size() < totalCount;
        } else if (G_OVERTIME.equals(param.group())) {
            ApprovalScopeQuery q = scopeQuery(scope, param.gvCmpnyCd(), OVERTIME_TYPES, HISTORY_STATUSES,
                    param.keyword(), param.startDate(), param.endDate(), offset, pageSize);
            items = mapHistory(mapper.selectHistoryCorrOt(q));
            totalCount = mapper.countHistoryCorrOt(q);
            hasMore = offset + items.size() < totalCount;
        } else if (G_SCHEDULE.equals(param.group())) {
            // PRAFTA-APP-029: 스케줄 수정(10) 이력 — corrOt 스코프/매핑 재사용(groupOf 가 SCHEDULE 산출).
            ApprovalScopeQuery q = scopeQuery(scope, param.gvCmpnyCd(), SCHEDULE_TYPES, HISTORY_STATUSES,
                    param.keyword(), param.startDate(), param.endDate(), offset, pageSize);
            items = mapHistory(mapper.selectHistoryCorrOt(q));
            totalCount = mapper.countHistoryCorrOt(q);
            hasMore = offset + items.size() < totalCount;
        } else if (G_LEAVE.equals(param.group())) {
            items = mapHistory(mapper.selectHistoryLeave(param.gvCmpnyCd(), param.gvUserCd(),
                    param.keyword(), param.startDate(), param.endDate(), offset, pageSize));
            totalCount = mapper.countHistoryLeave(param.gvCmpnyCd(), param.gvUserCd(),
                    param.keyword(), param.startDate(), param.endDate());
            hasMore = offset + items.size() < totalCount;
        } else {
            // Fix1: ALL 이력도 fetch 를 절대 상한으로 캡 → 2소스 메모리 병합 적재 폭주 차단.
            int fetch = Math.min(offset + pageSize + 1, MAX_OFFSET + pageSize + 1);
            // PRAFTA-APP-029: corrOt 합집합에 SCHEDULE_TYPES('10') 포함(근태보정/초과/스케줄 단일 SQL 소스).
            ApprovalScopeQuery corrOtQ = scopeQuery(scope, param.gvCmpnyCd(),
                    concat(concat(CORRECTION_TYPES, OVERTIME_TYPES), SCHEDULE_TYPES), HISTORY_STATUSES,
                    param.keyword(), param.startDate(), param.endDate(), 0, fetch);
            int corrOtCount = mapper.countHistoryCorrOt(corrOtQ);
            int leaveCount = mapper.countHistoryLeave(param.gvCmpnyCd(), param.gvUserCd(),
                    param.keyword(), param.startDate(), param.endDate());
            List<ApprovalHistoryResponse.HistoryItem> merged = new ArrayList<>();
            merged.addAll(mapHistory(mapper.selectHistoryCorrOt(corrOtQ)));
            merged.addAll(mapHistory(mapper.selectHistoryLeave(param.gvCmpnyCd(), param.gvUserCd(),
                    param.keyword(), param.startDate(), param.endDate(), 0, fetch)));
            merged.sort(Comparator.comparing(
                    ApprovalHistoryResponse.HistoryItem::getProcessDate,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            items = sliceHistory(merged, offset, pageSize);
            totalCount = corrOtCount + leaveCount;
            hasMore = merged.size() > offset + pageSize;
        }

        log.info("앱 관리자 승인 이력 조회 - cmpnyCd={}, group={}, count={}, totalCount={}",
                param.gvCmpnyCd(), param.group(), items.size(), totalCount);

        return ApprovalHistoryResponse.builder()
                .items(items)
                .totalCount(totalCount)
                .hasMore(hasMore)
                .build();
    }

    /** Fix1 가드용: 이력 그룹별 총건수만 산출(COUNT 집계 — 행 미적재라 DoS 무관). */
    private int historyTotalCount(ScopeContext scope, ApprovalHistoryParam param) {
        if (G_CORRECTION.equals(param.group())) {
            return mapper.countHistoryCorrOt(scopeQuery(scope, param.gvCmpnyCd(), CORRECTION_TYPES,
                    HISTORY_STATUSES, param.keyword(), param.startDate(), param.endDate(), 0, 0));
        }
        if (G_OVERTIME.equals(param.group())) {
            return mapper.countHistoryCorrOt(scopeQuery(scope, param.gvCmpnyCd(), OVERTIME_TYPES,
                    HISTORY_STATUSES, param.keyword(), param.startDate(), param.endDate(), 0, 0));
        }
        if (G_SCHEDULE.equals(param.group())) {
            return mapper.countHistoryCorrOt(scopeQuery(scope, param.gvCmpnyCd(), SCHEDULE_TYPES,
                    HISTORY_STATUSES, param.keyword(), param.startDate(), param.endDate(), 0, 0));
        }
        if (G_LEAVE.equals(param.group())) {
            return mapper.countHistoryLeave(param.gvCmpnyCd(), param.gvUserCd(),
                    param.keyword(), param.startDate(), param.endDate());
        }
        int corrOtCount = mapper.countHistoryCorrOt(scopeQuery(scope, param.gvCmpnyCd(),
                concat(concat(CORRECTION_TYPES, OVERTIME_TYPES), SCHEDULE_TYPES), HISTORY_STATUSES,
                param.keyword(), param.startDate(), param.endDate(), 0, 0));
        int leaveCount = mapper.countHistoryLeave(param.gvCmpnyCd(), param.gvUserCd(),
                param.keyword(), param.startDate(), param.endDate());
        return corrOtCount + leaveCount;
    }

    private List<ApprovalHistoryResponse.HistoryItem> mapHistory(List<HistoryRow> rows) {
        List<ApprovalHistoryResponse.HistoryItem> list = new ArrayList<>();
        if (rows == null) {
            return list;
        }
        for (HistoryRow r : rows) {
            list.add(ApprovalHistoryResponse.HistoryItem.builder()
                    .reqId(r.reqId())
                    .group(groupOf(r.reqType()))
                    .reqType(r.reqType())
                    .reqTypeNm(reqTypeNm(r.reqType()))
                    .requesterUserNm(r.requesterUserNm())
                    .nodeNm(r.nodeNm())
                    .targetYmd(r.workYmd())
                    .reqDate(r.reqDate())
                    .processDate(r.processDate())
                    .reqStatus(r.reqStatus())
                    .reqStatusNm(reqStatusNm(r.reqStatus()))
                    .processUserNm(r.processUserNm())
                    .rejectReason("03".equals(r.reqStatus()) ? r.rejectReason() : null)
                    .build());
        }
        return list;
    }

    // ============================ A-3 처리(승인/조정후승인/반려) ============================

    /**
     * group·decision 디스패치. 처리 전 IDOR 스코프 + 멱등(409) + ④마감을 서버에서 재검증한 뒤,
     * 실제 기록 반영은 web 처리 자산(attd07/leaveflow)에 위임한다(같은 트랜잭션, 원자적 전이).
     *
     * <p>위임 대상 web 서비스도 자체 가드(매니저 게이트 canManageNode / REQ_STATUS '01' / 마감 / 연차 결재자 본인·단계)를
     * 재수행하므로 방어가 중첩된다(defense-in-depth). REQ 의 권위 필드(메타)로 web param 을 구성해 변조검증을 통과시킨다.
     */
    @Override
    @Transactional
    public ApprovalProcessResponse process(ApprovalProcessParam p) {
        ScopeContext scope = resolveScope(p.gvCmpnyCd(), p.gvUserCd(), p.gvSiteCd(), p.gvAuthCd());

        ReqMetaRow meta = mapper.selectReqMeta(p.gvCmpnyCd(), p.reqId());
        if (meta == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_001);
        }
        String group = groupOf(meta.reqType());
        if (group == null) {
            // 07/08/09/임의 등 미지원 유형은 처리하지 않는다(SCHEDULE='10' 은 PRAFTA-APP-029 로 지원).
            throw new ApiException(AttdErrorCode.ATTD_400_006);
        }
        if (p.group() != null && !p.group().equals(group)) {
            log.warn("승인 처리 group 불일치 - paramGroup={}, actualGroup={}, reqId={}", p.group(), group, meta.reqId());
            throw new ApiException(AttdErrorCode.ATTD_400_006);
        }

        // ④ + IDOR + 멱등 재검증(서버 단일 출처). 위반 시 적절한 코드로 거부.
        revalidateGate(scope, meta, group, p);

        // Fix3(F1): 근태보정/초과/스케줄 위임 시 web canManageNode/변조검증이 app 과 동일 결론을 내도록
        //   유효 NODE_CD 를 확정한다(노드관리자 + REQ.NODE_CD NULL 인 경우 요청자 노드로 백필).
        //   PRAFTA-APP-029(D3): SCHEDULE 도 web approveSchedModifyRequest 가 canManageNode + nodeCd 변조검증을 하므로 포함.
        //   연차(LEAVE)는 nodeCd 게이트를 쓰지 않으므로 대상이 아니다(meta.nodeCd() 그대로).
        String effectiveNodeCd = (G_CORRECTION.equals(group) || G_OVERTIME.equals(group) || G_SCHEDULE.equals(group))
                ? resolveDelegationNodeCd(scope, meta, p)
                : meta.nodeCd();

        switch (p.decision()) {
            case ApprovalProcessParam.DECISION_REJECT:
                doReject(group, meta, effectiveNodeCd, p);
                break;
            case ApprovalProcessParam.DECISION_APPROVE_ASIS:
                doApproveAsis(group, meta, effectiveNodeCd, p);
                break;
            case ApprovalProcessParam.DECISION_APPROVE_ADJUST:
                // 보류(R3): 근태보정/초과 조정후승인 미구현 + 연차는 계약상 조정 불가(plan §5.8.4).
                //   메인 세션이 프론트 '조정 후 승인' 라디오를 비활성 처리한다(보류 플래그).
                log.warn("승인 처리 APPROVE_ADJUST 보류(미구현) - group={}, reqId={}, actor={}",
                        group, meta.reqId(), p.gvUserCd());
                throw new ApiException(AttdErrorCode.ATTD_400_006);
            default:
                throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        log.info("앱 관리자 승인 처리 완료 - reqId={}, group={}, decision={}, actor={}",
                meta.reqId(), group, p.decision(), p.gvUserCd());

        return ApprovalProcessResponse.builder()
                .reqId(meta.reqId())
                .group(group)
                .decision(p.decision())
                .processed(true)
                .build();
    }

    /** 처리 직전 게이트 재검증: IDOR 스코프 → 멱등(이미 처리) → ④마감. (본인결재차단은 2026-08-16 제거) */
    private void revalidateGate(ScopeContext scope, ReqMetaRow meta, String group, ApprovalProcessParam p) {
        // IDOR: 토큰 스코프 내인지 재검증. 연차는 결재선 참여(결재자 본인)도 허용(상세와 동일 규칙).
        boolean inScope = isInScope(scope, meta);
        boolean leaveApprover = G_LEAVE.equals(group)
                && isApproverOf(p.gvCmpnyCd(), meta.reqId(), p.gvUserCd());
        if (!inScope && !leaveApprover) {
            log.warn("승인 처리 스코프 위반(IDOR 차단) - userCd={}, reqId={}, reqSite={}, reqNode={}",
                    p.gvUserCd(), meta.reqId(), meta.siteCd(), meta.nodeCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
        // ② 본인 결재 차단은 제거됐다(2026-08-16 사용자 확정: "관리자는 본인이 낸 요청을 스스로 승인해도 무방하다").
        //   - 연차뿐 아니라 근태보정/초과/스케줄 전 유형에 적용한다.
        //   - 웹은 원래 이 무조건 차단이 없었다(앱만 더 엄격했다). 웹 일괄 승인 경로도 self-block 이 없어
        //     앱 단건에서만 막히는 비대칭이 있었다.
        //   - 같은 메서드의 IDOR 검사가 이미 "연차는 결재선 참여자면 통과"라는 예외를 두고 있어,
        //     결재선에 본인이 들어간 연차를 본인이 못 여는 것 자체가 앞뒤가 맞지 않았다.
        //   ★나머지 가드는 그대로 유지한다: IDOR 스코프(403_002) / 멱등 409 / 마감(400_042).
        //   ★근태보정/초과/스케줄은 위임 대상 web Attd07Service 가 노드 정책(SELF_ATTD_APPRV_YN)으로
        //     자기처리 허용 여부를 계속 판정한다(403_001/403_003) — 웹과 동일한 판정으로 수렴한다.

        // 멱등 충돌: REQ 가 이미 처리됨(대기 '01' 아님) → 409("다른 관리자가 이미 처리").
        //   연차 다단은 REQ 가 최종 승인 전까지 '01' 을 유지하므로, 단계 차례/중복은 위임 web 서비스가 추가 차단한다.
        if (!"01".equals(meta.reqStatus())) {
            log.warn("승인 처리 멱등 충돌(이미 처리됨) - reqId={}, status={}", meta.reqId(), meta.reqStatus());
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }
        // ④ 마감 차단: 대상월 근태 마감이면 거부.
        if (isClosed(p.gvCmpnyCd(), meta)) {
            log.warn("승인 처리 마감 차단 - reqId={}, siteCd={}, workYmd={}", meta.reqId(), meta.siteCd(), meta.workYmd());
            throw new ApiException(AttdErrorCode.ATTD_400_042);
        }
    }

    /** 반려(REQ_STATUS='03' + 처리자/사유/일시). 근태보정/초과=attd07, 연차=leaveflow 단계 반려. */
    private void doReject(String group, ReqMetaRow meta, String nodeCd, ApprovalProcessParam p) {
        switch (group) {
            case G_CORRECTION:
                // nodeCd = 유효 노드(Fix3): web canManageNode(param.nodeCd) + 변조검증(param.nodeCd vs REQ.NODE_CD) 동시 통과.
                attd07Service.rejectUserAttdRequest(new RejectUserAttdRequestParam(
                        meta.reqId(), meta.siteCd(), meta.userCd(), meta.workYmd(), workSeqStr(meta), nodeCd,
                        p.comment(),
                        p.gvCmpnyCd(), p.gvUserCd(), p.gvAuthCd(), p.gvSiteCd()));
                break;
            case G_OVERTIME:
                // OT 반려 param 은 nodeCd 를 싣지 않고 web 이 REQ.NODE_CD(권위행)로 canManageNode 를 판정한다.
                //   Fix3: NULL 노드건은 위임 전 resolveDelegationNodeCd 의 백필로 REQ.NODE_CD 가 채워져 게이트를 통과한다.
                attd07Service.rejectUserOvertimeRequest(new RejectUserOvertimeRequestParam(
                        meta.reqId(), meta.siteCd(), meta.userCd(), p.comment(),
                        p.gvCmpnyCd(), p.gvUserCd(), p.gvAuthCd(), p.gvSiteCd()));
                break;
            case G_SCHEDULE:
                // PRAFTA-APP-029(D1/D2): 스케줄 수정 반려는 근태 반려와 공유하는 web rejectSchedModifyRequest 위임.
                //   web from() 의 site==token 검증을 우회하기 위해 canonical 생성자로 직접 구성(hr/master 정상 처리).
                //   SCH_CD 는 운반하지 않음(web 이 REQ 행 권위 조회). nodeCd = effectiveNodeCd(백필 정합).
                attd07Service.rejectSchedModifyRequest(new RejectUserAttdRequestParam(
                        meta.reqId(), meta.siteCd(), meta.userCd(), meta.workYmd(), workSeqStr(meta), nodeCd,
                        p.comment(),
                        p.gvCmpnyCd(), p.gvUserCd(), p.gvAuthCd(), p.gvSiteCd()));
                break;
            case G_LEAVE:
                requireApprovalStep(p);
                leaveFlowService.rejectStep(new LeaveApprovalActionParam(
                        meta.reqId(), p.approvalStep(), p.comment(), p.gvCmpnyCd(), p.gvUserCd()));
                break;
            default:
                throw new ApiException(AttdErrorCode.ATTD_400_006);
        }
    }

    /**
     * 요청대로 승인(APPROVE_ASIS). 근태보정/초과는 REQ 의 신청값(START/END)을 그대로 반영,
     * 연차는 다단 결재 단계 승인. MGMT/HIST/차감은 위임 web 서비스가 수행한다.
     */
    private void doApproveAsis(String group, ReqMetaRow meta, String nodeCd, ApprovalProcessParam p) {
        switch (group) {
            case G_CORRECTION:
                // 근태보정 승인: 신청 출퇴근(START=출근, END=퇴근)을 MGMT 에 반영. method 는 REQ 미보관 → null(단순화, 보고).
                //   nodeCd = 유효 노드(Fix3): web canManageNode(param.nodeCd) + 변조검증(param.nodeCd vs REQ.NODE_CD) 동시 통과.
                attd07Service.updateUserAttdRequest(new UpdateUserAttdRequestParam(
                        meta.reqId(), meta.targetId(), meta.siteCd(), meta.userCd(),
                        meta.workYmd(), workSeqStr(meta), nodeCd,
                        meta.startDate(), meta.startTime(), null,
                        meta.endDate(), meta.endTime(), null,
                        null, null, null, null,
                        p.comment(),
                        p.gvCmpnyCd(), p.gvUserCd(), p.gvAuthCd(), p.gvSiteCd()));
                break;
            case G_OVERTIME:
                // 초과근무 승인: 신청 구간 1건(START~END)을 OT 등록 엔진(web)에 투입해 등록. attdId 는 일자 기준 서버 도출 위임(null).
                //   nodeCd = 유효 노드(Fix3): web canManageNode(param.nodeCd) 통과(OT 승인은 nodeCd 변조검증 없음).
                List<OvertimeItemModel> overtimes = new ArrayList<>(1);
                overtimes.add(new OvertimeItemModel(
                        // 앱 승인은 신청 구간을 신규 등록(INSERT)하는 경로이므로 in-place 수정 식별자(otId)는 없다.
                        null, meta.startDate(), meta.startTime(), meta.endDate(), meta.endTime()));
                attd07Service.updateUserOvertimeRequests(new UpdateUserOvertimeRequestParam(
                        meta.userCd(), meta.siteCd(), nodeCd, meta.workYmd(),
                        null, meta.reqId(), meta.reqReason(), overtimes,
                        p.gvCmpnyCd(), p.gvUserCd(), p.gvAuthCd(), p.gvSiteCd()));
                break;
            case G_SCHEDULE:
                // PRAFTA-APP-029(D1/D2/D8): 스케줄 수정 승인은 web approveSchedModifyRequest 위임.
                //   web 이 tb_user_work_plan.WORK_PLAN_CD ← REQ.SCH_CD upsert + REQ_STATUS='02' + 승인마커 +
                //   교대잠금(ATTD_400_160)/마감/변조검증을 모두 수행한다(앱은 처리 로직 미작성).
                //   canonical 생성자로 직접 구성(web from() 의 site==token 검증 우회 — hr/master 정상 처리, D2-a).
                //   SCH_CD 미운반(web 이 REQ 행 권위 조회 — IDOR/변조 방지). nodeCd = effectiveNodeCd(백필 정합).
                attd07Service.approveSchedModifyRequest(new ApproveSchedModifyRequestParam(
                        meta.reqId(), meta.siteCd(), meta.userCd(), meta.workYmd(),
                        workSeqStr(meta), nodeCd,
                        p.gvCmpnyCd(), p.gvUserCd(), p.gvAuthCd(), p.gvSiteCd()));
                break;
            case G_LEAVE:
                requireApprovalStep(p);
                leaveFlowService.approveStep(new LeaveApprovalActionParam(
                        meta.reqId(), p.approvalStep(), p.comment(), p.gvCmpnyCd(), p.gvUserCd()));
                break;
            default:
                throw new ApiException(AttdErrorCode.ATTD_400_006);
        }
    }

    /** 연차(다단 결재) 처리는 결재 단계 번호가 필수다. */
    private void requireApprovalStep(ApprovalProcessParam p) {
        if (p.approvalStep() == null) {
            log.warn("연차 승인/반려 결재 단계(approvalStep) 누락 - reqId={}", p.reqId());
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }
    }

    /** ReqMetaRow.workSeq(Integer) → 문자열(web 변조검증은 문자열 비교, null 은 null 유지). */
    private String workSeqStr(ReqMetaRow meta) {
        return meta.workSeq() == null ? null : String.valueOf(meta.workSeq());
    }

    // ============================ 스코프/IDOR 보조 ============================

    /** 근태보정/초과 스코프 판정(연차 외). master/hr-site/node 축. */
    private boolean isInScope(ScopeContext s, ReqMetaRow meta) {
        if (s.master()) {
            return true;
        }
        if (s.hrSiteCd() != null) {
            return s.hrSiteCd().equals(meta.siteCd());
        }
        if (s.useNodeScope()) {
            if (!s.nodeSiteCd().equals(meta.siteCd())) {
                return false;
            }
            String node = StringUtils.hasText(meta.nodeCd()) ? meta.nodeCd() : meta.requesterNodeCd();
            return node != null && s.scopedNodeCds().contains(node);
        }
        return false;
    }

    /**
     * Fix3(F1): 근태보정/초과 위임 시 web 게이트가 app 과 동일 결론을 내도록 유효 NODE_CD 를 확정한다.
     *
     * <p>대기 리스트·isInScope 는 COALESCE(REQ.NODE_CD, 요청자 U.NODE_CD) 로 노드 스코프를 판정해 노드관리자에게
     * NODE_CD NULL 건도 노출/허용하지만, 위임 시 web {@code canManageNode(meta.nodeCd()=NULL)} 가 노드역할에서 false →
     * ATTD_403_002 회귀가 발생한다. app 이 이미 스코프를 권위 검증했으므로, 위임 직전 REQ.NODE_CD 를 요청자 실제
     * 노드로 백필(데이터품질 교정, 같은 트랜잭션)하고 그 값을 web param 으로 전달한다.
     *
     * <ul>
     *   <li>master/hr(canManageAllNodes): useNodeScope=false → 백필하지 않고 meta.nodeCd() 그대로 → 기존 동작 불변.</li>
     *   <li>REQ.NODE_CD 가 이미 있으면: 그대로 사용(백필 없음).</li>
     *   <li>노드관리자 + REQ.NODE_CD NULL + 요청자 노드 보유: 요청자 노드로 백필 후 그 값 반환.</li>
     * </ul>
     *
     * @return web param 에 실을 유효 NODE_CD.
     */
    private String resolveDelegationNodeCd(ScopeContext scope, ReqMetaRow meta, ApprovalProcessParam p) {
        String reqNode = meta.nodeCd();
        // master/hr 또는 이미 NODE_CD 보유 → 변경 없음(기존 동작 불변).
        if (!scope.useNodeScope() || StringUtils.hasText(reqNode)) {
            return reqNode;
        }
        String requesterNode = meta.requesterNodeCd();
        // 요청자 노드도 없으면 백필 불가(노드 스코프 검증을 통과했다면 이 경로는 도달하지 않음).
        if (!StringUtils.hasText(requesterNode)) {
            return reqNode;
        }
        int updated = mapper.backfillReqNodeCd(p.gvCmpnyCd(), meta.reqId(), requesterNode, p.gvUserCd());
        log.info("승인 처리 전 REQ.NODE_CD 백필(노드관리자 NULL건 교정) - reqId={}, requesterNode={}, updated={}",
                meta.reqId(), requesterNode, updated);
        return requesterNode;
    }

    private boolean isApproverOf(String cmpnyCd, String reqId, String userCd) {
        List<ApprovalStepVO> steps = approvalLineMapper.selectApprovalLineByReqId(cmpnyCd, reqId);
        if (steps == null) {
            return false;
        }
        for (ApprovalStepVO s : steps) {
            if (userCd.equals(s.getApproverUserCd())) {
                return true;
            }
        }
        return false;
    }

    /** ④ 마감 차단: 대상 NODE_CD(요청행 우선, NULL 이면 요청자 노드, 그래도 없으면 전체 '*') 의 마감 여부. */
    private boolean isClosed(String cmpnyCd, ReqMetaRow meta) {
        if (!StringUtils.hasText(meta.siteCd()) || !StringUtils.hasText(meta.workYmd())
                || meta.workYmd().length() < 6) {
            return false;
        }
        String closeYm = meta.workYmd().substring(0, 6);
        String node = StringUtils.hasText(meta.nodeCd()) ? meta.nodeCd()
                : (StringUtils.hasText(meta.requesterNodeCd()) ? meta.requesterNodeCd() : WHOLE_SITE);
        return mapper.countCloseCovering(cmpnyCd, meta.siteCd(), node, closeYm) > 0;
    }

    private Integer currentAppliedStep(List<ApprovalStepVO> steps) {
        if (steps == null) {
            return null;
        }
        for (ApprovalStepVO s : steps) {
            if (STEP_APPLIED.equals(s.getApprovalStatus())) {
                return s.getApprovalStep();
            }
        }
        return null;
    }

    // ============================ 코드/표시 헬퍼 ============================

    private String groupOf(String reqType) {
        if (reqType == null) {
            return null;
        }
        switch (reqType) {
            case "01":
            case "02":
                return G_CORRECTION;
            case "03":
            case "04":
                return G_OVERTIME;
            case "05":
            case "06":
                return G_LEAVE;
            case "10":
                return G_SCHEDULE;   // PRAFTA-APP-029: 스케줄 수정 요청
            default:
                return null;   // 07/08/09/임의 미지원(ATTD_400_006)
        }
    }

    private String reqTypeNm(String reqType) {
        if (reqType == null) {
            return null;
        }
        switch (reqType) {
            case "01": return "근태생성";
            case "02": return "근태수정";
            case "03": return "초과생성";
            case "04": return "초과수정";
            case "05": return "연차사용";
            case "06": return "연차수정";
            case "10": return "스케줄수정";
            default: return null;
        }
    }

    private String reqStatusNm(String reqStatus) {
        if (reqStatus == null) {
            return null;
        }
        switch (reqStatus) {
            case "01": return "신청";
            case "02": return "승인";
            case "03": return "반려";
            case "04": return "취소";
            default: return null;
        }
    }

    private List<String> summaryCorrOt(String group, PendingCorrOtRow r, String schedRange) {
        // 대상일자(WORK_YMD)는 카드의 targetYmdDisplay(포맷된 별도 표시)와 동일 데이터이므로
        // summary 라인에서는 중복 표기하지 않는다(요약은 변경 내용만).
        List<String> lines = new ArrayList<>();
        if (G_SCHEDULE.equals(group)) {
            // PRAFTA-APP-029 후속: 요청 스케줄(REQ.SCH_CD)의 실제 시각 range 로 표기한다(예: "스케줄 변경 → 09:00~18:00").
            if (StringUtils.hasText(schedRange)) {
                lines.add("스케줄 변경 → " + schedRange);
            } else {
                // 폴백: 요청 SCH_CD 가 마스터에 미존재(데이터 이상)하거나 시각 미상 — 코드만/문구만 노출.
                String sch = (r.schCd() == null || r.schCd().isBlank()) ? "" : r.schCd();
                lines.add(sch.isEmpty() ? "스케줄 변경 요청" : "스케줄 변경 → " + sch);
            }
            return lines;
        }
        String from = joinDt(r.startDate(), r.startTime());
        String to = joinDt(r.endDate(), r.endTime());
        if (G_OVERTIME.equals(group)) {
            lines.add("초과 " + nz(from) + " ~ " + nz(to));
        } else {
            lines.add("출근 " + nz(from) + " / 퇴근 " + nz(to));
        }
        return lines;
    }

    private List<String> summaryLeave(PendingLeaveRow r) {
        List<String> lines = new ArrayList<>();
        StringBuilder l1 = new StringBuilder();
        if (StringUtils.hasText(r.leaveNm())) {
            l1.append(r.leaveNm());
        }
        if (StringUtils.hasText(r.unitNm())) {
            l1.append(l1.length() > 0 ? " " : "").append(r.unitNm());
        }
        if (l1.length() > 0) {
            lines.add(l1.toString());
        }
        // 대상일자(WORK_YMD)는 카드의 targetYmdDisplay(포맷된 별도 표시)와 동일 데이터이므로
        // summary 라인에서는 중복 표기하지 않는다.
        return lines;
    }

    /** YYYYMMDD + HHMM → 'YYYY-MM-DD HH:MM'. 값이 비면 빈 문자열/부분 표기. */
    private String joinDt(String ymd, String hm) {
        String d = fmtYmd(ymd);
        String t = fmtHm(hm);
        if (d.isEmpty() && t.isEmpty()) {
            return "";
        }
        if (t.isEmpty()) {
            return d;
        }
        return (d + " " + t).trim();
    }

    private String fmtYmd(String ymd) {
        if (ymd != null && ymd.length() == 8) {
            return ymd.substring(0, 4) + "-" + ymd.substring(4, 6) + "-" + ymd.substring(6, 8);
        }
        return ymd == null ? "" : ymd;
    }

    private String fmtHm(String hm) {
        if (hm != null && hm.length() == 4) {
            return hm.substring(0, 2) + ":" + hm.substring(2, 4);
        }
        return hm == null ? "" : hm;
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }

    private List<String> concat(List<String> a, List<String> b) {
        List<String> out = new ArrayList<>(a);
        out.addAll(b);
        return out;
    }

    private List<ApprovalPendingResponse.PendingItem> slice(
            List<ApprovalPendingResponse.PendingItem> all, int offset, int size) {
        if (offset >= all.size()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(all.subList(offset, Math.min(offset + size, all.size())));
    }

    private List<ApprovalHistoryResponse.HistoryItem> sliceHistory(
            List<ApprovalHistoryResponse.HistoryItem> all, int offset, int size) {
        if (offset >= all.size()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(all.subList(offset, Math.min(offset + size, all.size())));
    }
}
