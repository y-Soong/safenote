package com.prafta.app.leave.approval.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.prafta.app.leave.approval.application.param.LeaveApprovalDetailParam;
import com.prafta.app.leave.approval.application.param.LeaveApprovalHistoryParam;
import com.prafta.app.leave.approval.application.param.LeaveApprovalPendingParam;
import com.prafta.app.leave.approval.application.param.LeaveApprovalProcessParam;
import com.prafta.app.leave.approval.dto.response.LeaveApprovalDetailResponse;
import com.prafta.app.leave.approval.dto.response.LeaveApprovalHistoryResponse;
import com.prafta.app.leave.approval.dto.response.LeaveApprovalPendingResponse;
import com.prafta.app.leave.approval.dto.response.LeaveApprovalProcessResponse;
import com.prafta.app.leave.approval.mapper.AppLeaveApprovalMapper;
import com.prafta.app.leave.approval.result.LeaveBalanceRow;
import com.prafta.app.leave.approval.result.LeaveDetailBodyRow;
import com.prafta.app.leave.approval.result.LeaveProcessedHistoryRow;
import com.prafta.app.leave.approval.result.LeaveReqMetaRow;
import com.prafta.app.leave.approval.service.AppLeaveApprovalService;
import com.prafta.common.cmm.approval.mapper.ApprovalLineMapper;
import com.prafta.common.cmm.approval.vo.ApprovalStepVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.attd.leaveflow.application.param.LeaveApprovalActionParam;
import com.prafta.web.attd.leaveflow.service.LeaveFlowService;
import com.prafta.web.attd.leaveflow.vo.MyLeaveApprovalVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자연차결재-01: 사용자 모드 연차 결재 관리 서비스 구현(결재자 본인 스코프).
 *
 * <p>대기·처리는 동일 JVM @Service 빈 {@link LeaveFlowService} 에 @Transactional 위임한다(SQL/엔진 복제 금지).
 * 엔진이 멱등(409)/단계차례(400_053)/본인결재(403_030)/마감(400_042)/반려사유(400_057)를 재검증하므로 방어가 중첩된다.
 * 상세는 읽기 SQL 포팅(F-DETAIL) + 게이트 산출, 이력은 "내 단계 행동 기준" 신규 쿼리(F-H1)다.
 * 관리자 승인 관리(/admin/approval/*) 무관 — 권한(authCd) 게이트를 쓰지 않고 결재선 실존(isApproverOf)으로만 인가한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppLeaveApprovalServiceImpl implements AppLeaveApprovalService {

    private final AppLeaveApprovalMapper mapper;
    private final LeaveFlowService leaveFlowService;       // 연차 표준 엔진(대기/승인/반려 위임)
    private final ApprovalLineMapper approvalLineMapper;   // 결재선(steps/isApproverOf)
    private final AttdCloseService attdCloseService;       // 마감 차단 판정(부서 단위, PRAFTA-028)

    private static final String GROUP_LEAVE = "LEAVE";
    private static final String STEP_APPLIED = "01";       // SYS044 신청(차례)
    private static final String STEP_APPROVED = "02";
    private static final String STEP_REJECTED = "03";

    /** 이력 기본 조회 범위(최근 N일). */
    private static final int HISTORY_DEFAULT_DAYS = 30;
    /** 이력 페이지 크기(v1 — 30일 범위로 자연 소량, 1페이지 고정 반환). */
    private static final int HISTORY_PAGE_SIZE = 50;

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ============================ 3-A 대기 ============================

    @Override
    public LeaveApprovalPendingResponse selectPending(LeaveApprovalPendingParam param) {
        // 엔진 위임: 내가 현재 단계('01') 결재자인 05/06 요청 전체(결재자 본인 스코프).
        List<MyLeaveApprovalVO> vos =
                leaveFlowService.getMyPendingLeaveApprovals(param.gvCmpnyCd(), param.gvUserCd());

        String keyword = param.keyword();
        List<LeaveApprovalPendingResponse.PendingItem> items = new ArrayList<>();
        if (vos != null) {
            for (MyLeaveApprovalVO v : vos) {
                if (!matchesKeyword(v, keyword)) {
                    continue;
                }
                items.add(LeaveApprovalPendingResponse.PendingItem.builder()
                        .reqId(v.reqId())
                        .approvalStep(v.approvalStep())
                        .group(GROUP_LEAVE)
                        .reqType(null)             // MyLeaveApprovalVO 는 요청유형코드를 싣지 않음 — 표시는 leaveNm 사용
                        .reqTypeNm(null)
                        .requesterUserNm(v.requesterUserNm())
                        .requesterUserCd(v.requesterUserCd())
                        .nodeNm(v.nodeNm())
                        .targetYmd(v.workYmd())
                        .leaveNm(v.leaveNm())
                        .unitNm(v.unitNm())
                        .leaveDays(v.leaveDays())
                        .leaveMinutes(v.leaveMinutes())
                        .startTime(v.startTime())
                        .endTime(v.endTime())
                        .summaryLines(summaryLeave(v))
                        .reqDate(v.reqDate())
                        .selfYn(v.selfYn())
                        // 가불표시-02: 구조화 필드로 전달(summaryLines 문자열 오염 금지 — FE 가 칩 렌더).
                        .borrowDays(v.borrowDays())
                        .build());
            }
        }

        log.info("앱 사용자 연차 결재 대기 조회 - cmpnyCd={}, approver={}, count={}",
                param.gvCmpnyCd(), param.gvUserCd(), items.size());

        return LeaveApprovalPendingResponse.builder()
                .items(items)
                .totalCount(items.size())
                .hasMore(false)
                .build();
    }

    private boolean matchesKeyword(MyLeaveApprovalVO v, String keyword) {
        if (keyword == null) {
            return true;
        }
        String kw = keyword.toLowerCase();
        String nm = v.requesterUserNm() == null ? "" : v.requesterUserNm().toLowerCase();
        String cd = v.requesterUserCd() == null ? "" : v.requesterUserCd().toLowerCase();
        return nm.contains(kw) || cd.contains(kw);
    }

    private List<String> summaryLeave(MyLeaveApprovalVO v) {
        List<String> lines = new ArrayList<>();
        StringBuilder l1 = new StringBuilder();
        if (StringUtils.hasText(v.leaveNm())) {
            l1.append(v.leaveNm());
        }
        if (StringUtils.hasText(v.unitNm())) {
            l1.append(l1.length() > 0 ? " " : "").append(v.unitNm());
        }
        if (l1.length() > 0) {
            lines.add(l1.toString());
        }
        lines.add("대상일자 " + fmtYmd(v.workYmd()));
        return lines;
    }

    // ============================ 3-B 상세 ============================

    @Override
    public LeaveApprovalDetailResponse selectDetail(LeaveApprovalDetailParam param) {
        LeaveReqMetaRow meta = mapper.selectLeaveReqMeta(param.gvCmpnyCd(), param.reqId());
        if (meta == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_001);
        }
        // 연차(05/06)만 지원. 그 외 유형은 본 엔드포인트 범위 외.
        if (!isLeaveType(meta.reqType())) {
            throw new ApiException(AttdErrorCode.ATTD_400_006);
        }

        // 결재선 + IDOR: 결재선에 내가 결재자로 실존하지 않으면 403(식별자는 토큰 전용).
        List<ApprovalStepVO> steps = approvalLineMapper.selectApprovalLineByReqId(param.gvCmpnyCd(), param.reqId());
        Integer myStep = findMyStep(steps, param.gvUserCd());
        if (myStep == null) {
            log.warn("연차 결재 상세 IDOR 차단(결재선 미존재) - userCd={}, reqId={}", param.gvUserCd(), param.reqId());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
        String myStepStatus = statusOfStep(steps, myStep);
        Integer currentStep = currentAppliedStep(steps);

        // 게이트 산출(서버 단일 출처).
        boolean selfBlocked = meta.userCd() != null && meta.userCd().equals(param.gvUserCd());
        boolean closed = isClosed(param.gvCmpnyCd(), meta);
        boolean reqConflict = !STEP_APPLIED.equals(meta.reqStatus());
        boolean stepConflict = !STEP_APPLIED.equals(myStepStatus);
        boolean conflict = reqConflict || stepConflict;
        boolean canProcess = !selfBlocked && !closed && !conflict;

        String conflictMsg = null;
        if (conflict) {
            conflictMsg = reqConflict ? "이미 처리된 요청입니다." : "아직 결재 차례가 아닙니다.";
        }

        LeaveApprovalDetailResponse.Gate gate = LeaveApprovalDetailResponse.Gate.builder()
                .canProcess(canProcess)
                .selfBlockedYn(selfBlocked)
                .closedYn(closed)
                .conflictYn(conflict)
                .conflictMsg(conflictMsg)
                .myStep(myStep)
                .myStepStatus(myStepStatus)
                .build();

        LeaveApprovalDetailResponse.Meta metaOut = LeaveApprovalDetailResponse.Meta.builder()
                .reqId(meta.reqId())
                .group(GROUP_LEAVE)
                .reqType(meta.reqType())
                .reqTypeNm(reqTypeNm(meta.reqType()))
                .reqStatus(meta.reqStatus())
                .reqStatusNm(reqStatusNm(meta.reqStatus()))
                .requesterUserNm(meta.requesterUserNm())
                .requesterUserCd(meta.userCd())
                .nodeNm(meta.nodeNm())
                .reqDate(meta.reqDate())
                .targetYmd(meta.workYmd())
                .approvalStep(currentStep)
                .build();

        Map<String, Object> body = buildBody(param.gvCmpnyCd(), meta, steps);

        log.info("앱 사용자 연차 결재 상세 조회 - reqId={}, myStep={}, canProcess={}, self={}, closed={}, conflict={}",
                meta.reqId(), myStep, canProcess, selfBlocked, closed, conflict);

        return LeaveApprovalDetailResponse.builder()
                .meta(metaOut)
                .gate(gate)
                .body(body)
                .reason(meta.reqReason())
                .build();
    }

    private Map<String, Object> buildBody(String cmpnyCd, LeaveReqMetaRow meta, List<ApprovalStepVO> steps) {
        Map<String, Object> body = new LinkedHashMap<>();
        LeaveDetailBodyRow lb = mapper.selectLeaveBody(cmpnyCd, meta.reqId(), meta.targetId());
        if (lb != null) {
            body.put("leaveCd", lb.leaveCd());
            body.put("leaveNm", lb.leaveNm());
            body.put("paidYn", lb.paidYn());
            body.put("unitNm", lb.unitNm());
            // 가불표시-02: 가불 충당 일수(0 이상). FE 는 0 초과일 때만 "가불 N일" 행 표시.
            body.put("borrowDays", lb.borrowDays());

            Map<String, Object> applied = new LinkedHashMap<>();
            applied.put("startDate", lb.startDate());
            applied.put("startTime", lb.startTime());
            applied.put("endDate", lb.endDate());
            applied.put("endTime", lb.endTime());
            applied.put("leaveDays", lb.leaveDays());
            applied.put("leaveMinutes", lb.leaveMinutes());
            body.put("appliedRange", applied);

            LeaveBalanceRow bal = mapper.selectLeaveBalance(cmpnyCd, meta.userCd(), lb.leaveCd());
            Map<String, Object> balance = new LinkedHashMap<>();
            balance.put("granted", bal != null ? bal.granted() : BigDecimal.ZERO);
            balance.put("used", bal != null ? bal.used() : BigDecimal.ZERO);
            balance.put("remain", bal != null ? bal.remain() : BigDecimal.ZERO);
            body.put("balance", balance);
        }
        body.put("steps", buildSteps(steps));
        return body;
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

    // ============================ 3-C 이력 ============================

    @Override
    public LeaveApprovalHistoryResponse selectHistory(LeaveApprovalHistoryParam param) {
        LocalDate today = LocalDate.now();
        String endYmd = StringUtils.hasText(param.endDate()) ? param.endDate() : today.format(YMD);
        String startYmd = StringUtils.hasText(param.startDate())
                ? param.startDate() : today.minusDays(HISTORY_DEFAULT_DAYS).format(YMD);

        int total = mapper.countMyProcessedLeaveHistory(
                param.gvCmpnyCd(), param.gvUserCd(), param.keyword(), startYmd, endYmd);

        List<LeaveProcessedHistoryRow> rows = mapper.selectMyProcessedLeaveHistory(
                param.gvCmpnyCd(), param.gvUserCd(), param.keyword(), startYmd, endYmd, 0, HISTORY_PAGE_SIZE);

        List<LeaveApprovalHistoryResponse.HistoryItem> items = new ArrayList<>();
        if (rows != null) {
            for (LeaveProcessedHistoryRow r : rows) {
                items.add(LeaveApprovalHistoryResponse.HistoryItem.builder()
                        .reqId(r.reqId())
                        .group(GROUP_LEAVE)
                        .reqType(r.reqType())
                        .reqTypeNm(reqTypeNm(r.reqType()))
                        .requesterUserNm(r.requesterUserNm())
                        .nodeNm(r.nodeNm())
                        .targetYmd(r.workYmd())
                        .leaveNm(r.leaveNm())
                        .unitNm(r.unitNm())
                        .leaveDays(r.leaveDays())
                        .reqDate(r.reqDate())
                        .myDecision(r.myDecision())
                        .myDecisionNm(decisionNm(r.myDecision()))
                        .myProcessDate(r.myProcessDate())
                        .myComment(r.myComment())
                        .reqStatus(r.reqStatus())
                        .reqStatusNm(reqStatusNm(r.reqStatus()))
                        .build());
            }
        }

        log.info("앱 사용자 연차 결재 이력 조회 - cmpnyCd={}, approver={}, range={}~{}, count={}, total={}",
                param.gvCmpnyCd(), param.gvUserCd(), startYmd, endYmd, items.size(), total);

        return LeaveApprovalHistoryResponse.builder()
                .items(items)
                .totalCount(total)
                .hasMore(total > items.size())
                .build();
    }

    // ============================ 3-D 처리 ============================

    @Override
    @Transactional
    public LeaveApprovalProcessResponse process(LeaveApprovalProcessParam param) {
        // 진입 게이트: 결재선에 내가 결재자로 실존하는지 재검증(IDOR). 식별자는 토큰 전용.
        List<ApprovalStepVO> steps = approvalLineMapper.selectApprovalLineByReqId(param.gvCmpnyCd(), param.reqId());
        if (findMyStep(steps, param.gvUserCd()) == null) {
            log.warn("연차 결재 처리 IDOR 차단(결재선 미존재) - userCd={}, reqId={}", param.gvUserCd(), param.reqId());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }

        // 표준 엔진 위임(@Transactional). 멱등(409)/단계차례(400_053)/본인결재(403_030)/마감(400_042)/반려사유(400_057)는 엔진 재검증.
        LeaveApprovalActionParam action = new LeaveApprovalActionParam(
                param.reqId(), param.approvalStep(), param.comment(), param.gvCmpnyCd(), param.gvUserCd());

        if (LeaveApprovalProcessParam.DECISION_APPROVE.equals(param.decision())) {
            leaveFlowService.approveStep(action);
        } else {
            leaveFlowService.rejectStep(action);
        }

        log.info("앱 사용자 연차 결재 처리 완료 - reqId={}, step={}, decision={}, approver={}",
                param.reqId(), param.approvalStep(), param.decision(), param.gvUserCd());

        return LeaveApprovalProcessResponse.builder()
                .reqId(param.reqId())
                .approvalStep(param.approvalStep())
                .decision(param.decision())
                .processed(true)
                .build();
    }

    // ============================ 보조 ============================

    private boolean isLeaveType(String reqType) {
        return "05".equals(reqType) || "06".equals(reqType);
    }

    /** 결재선에서 내가 결재자인 단계 번호(없으면 null = 결재선 미참여). */
    private Integer findMyStep(List<ApprovalStepVO> steps, String userCd) {
        if (steps == null || userCd == null) {
            return null;
        }
        for (ApprovalStepVO s : steps) {
            if (userCd.equals(s.getApproverUserCd())) {
                return s.getApprovalStep();
            }
        }
        return null;
    }

    private String statusOfStep(List<ApprovalStepVO> steps, Integer step) {
        if (steps == null || step == null) {
            return null;
        }
        for (ApprovalStepVO s : steps) {
            if (step.equals(s.getApprovalStep())) {
                return s.getApprovalStatus();
            }
        }
        return null;
    }

    /** 현재 차례('01' 신청) 단계 번호. 없으면 null. */
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

    /** 마감 차단: 신청자 부서 기준 대상월 마감 여부(PRAFTA-028). cmpnyCd 는 토큰 스코프값을 전달받는다. */
    private boolean isClosed(String cmpnyCd, LeaveReqMetaRow meta) {
        if (!StringUtils.hasText(meta.siteCd()) || !StringUtils.hasText(meta.workYmd())
                || meta.workYmd().length() < 6) {
            return false;
        }
        String closeYm = meta.workYmd().substring(0, 6);
        return attdCloseService.isClosedForUser(cmpnyCd, meta.siteCd(), meta.userCd(), closeYm);
    }

    private String reqTypeNm(String reqType) {
        if (reqType == null) {
            return null;
        }
        switch (reqType) {
            case "05": return "연차사용";
            case "06": return "연차수정";
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

    private String decisionNm(String status) {
        if (STEP_APPROVED.equals(status)) {
            return "승인";
        }
        if (STEP_REJECTED.equals(status)) {
            return "반려";
        }
        return null;
    }

    private String fmtYmd(String ymd) {
        if (ymd != null && ymd.length() == 8) {
            return ymd.substring(0, 4) + "-" + ymd.substring(4, 6) + "-" + ymd.substring(6, 8);
        }
        return ymd == null ? "" : ymd;
    }
}
