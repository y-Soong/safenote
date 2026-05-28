package com.prafta.web.attd.leaveflow.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.approval.mapper.ApprovalLineMapper;
import com.prafta.common.cmm.approval.vo.ApprovalStepVO;
import com.prafta.common.cmm.leave.service.LeaveDeductionService;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.DateTimeUtils;
import com.prafta.web.attd.attd07.service.AttdCloseService;
import com.prafta.web.attd.leaveflow.application.command.LeaveReqInsertCommand;
import com.prafta.web.attd.leaveflow.application.param.LeaveApplyParam;
import com.prafta.web.attd.leaveflow.application.param.LeaveApprovalActionParam;
import com.prafta.web.attd.leaveflow.mapper.LeaveFlowMapper;
import com.prafta.web.attd.leaveflow.service.LeaveFlowService;
import com.prafta.web.attd.leaveflow.vo.DeductibleGrantVO;
import com.prafta.web.attd.leaveflow.vo.DirectLeaveResult;
import com.prafta.web.attd.leaveflow.vo.LeaveModifyTargetVO;
import com.prafta.web.attd.leaveflow.vo.LeaveReqRowVO;
import com.prafta.web.attd.leaveflow.vo.LeaveUseDetailVO;
import com.prafta.web.attd.leaveflow.vo.LeaveTypeInfoVO;
import com.prafta.web.attd.leaveflow.vo.LeaveUseVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link LeaveFlowService} 구현 (prafta-019-E).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveFlowServiceImpl implements LeaveFlowService {

    // 요청 유형 [SYS032]
    private static final String REQ_TYPE_LEAVE_USE = "05";
    private static final String REQ_TYPE_LEAVE_MODIFY = "06";
    // 요청 상태 [SYS033]
    private static final String REQ_APPLIED = "01";
    private static final String REQ_APPROVED = "02";
    private static final String REQ_REJECTED = "03";
    // 결재 단계 상태 [SYS044]
    private static final String STEP_WAIT = "00";
    private static final String STEP_APPLIED = "01";
    private static final String STEP_APPROVED = "02";
    private static final String STEP_REJECTED = "03";
    // 사용 단위 [SYS025]
    private static final String UNIT_FULL = "00";
    private static final String UNIT_HALF = "01";
    private static final String UNIT_HOUR2 = "02";
    private static final String UNIT_HOUR1 = "03";
    private static final String UNIT_MIN30 = "04";

    private static final String USE_CONFIRMED = "CONFIRMED";

    private final LeaveFlowMapper leaveFlowMapper;
    private final ApprovalLineMapper approvalLineMapper;
    private final LeaveDeductionService leaveDeductionService;
    private final AttdCloseService attdCloseService;

    @Override
    @Transactional
    public void submitLeave(LeaveApplyParam p) {
        final String cmpny = p.gvCmpnyCd();
        final String site = p.gvSiteCd();
        final String user = p.gvUserCd();
        final String workYmd = p.workYmd();
        final String leaveCd = p.leaveCd();
        final String unit = p.useUnitType();

        // 1) 타입 메타 + 결재 여부 (회사정의=타입 APRV_USE_YN, 법정 시드=정책 APRV_USE_YN)
        LeaveTypeInfoVO type = leaveFlowMapper.selectLeaveTypeInfo(cmpny, leaveCd);
        if (type == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_030);
        }
        boolean aprvRequired;
        if ("Y".equals(type.systemYn())) {
            aprvRequired = "Y".equals(leaveFlowMapper.selectPolicyAprvUseYn(cmpny));
        } else {
            aprvRequired = "Y".equals(type.aprvUseYn());
        }

        // 2) 사용 단위 검증 + 차감 계산
        BigDecimal leaveDays;
        Integer leaveMinutes = null;
        String startTime = null;
        String endTime = null;

        if (UNIT_FULL.equals(unit)) {
            leaveDays = new BigDecimal("1.00000");
        } else if (UNIT_HALF.equals(unit)) {
            leaveDays = new BigDecimal("0.50000");
            Integer daily = leaveDeductionService.getDailyStdWorkMinutes(cmpny, site, user, workYmd);
            if (daily != null) {
                leaveMinutes = daily / 2;
            }
        } else if (UNIT_HOUR2.equals(unit) || UNIT_HOUR1.equals(unit) || UNIT_MIN30.equals(unit)) {
            startTime = p.startTime();
            endTime = p.endTime();
            Integer sMin = DateTimeUtils.hhmmToMinutes(startTime);
            Integer eMin = DateTimeUtils.hhmmToMinutes(endTime);
            if (sMin == null || eMin == null || eMin <= sMin) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            int minutes = eMin - sMin;
            int unitMin = unitMinutes(unit);
            if (minutes % unitMin != 0) {
                throw new ApiException(AttdErrorCode.ATTD_400_054);
            }
            Integer daily = leaveDeductionService.getDailyStdWorkMinutes(cmpny, site, user, workYmd);
            if (daily == null || minutes > daily) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            // 휴게 가로지름 거부 (§8.5.9) — 휴게시각 미설정이면 skip
            if (leaveDeductionService.crossesBreak(cmpny, site, user, workYmd, sMin, eMin)) {
                throw new ApiException(AttdErrorCode.ATTD_400_055);
            }
            leaveDays = leaveDeductionService.calcDeductionDays(minutes, daily);
            if (leaveDays == null) {
                throw new ApiException(AttdErrorCode.ATTD_400_052);
            }
            leaveMinutes = minutes;
        } else {
            throw new ApiException(AttdErrorCode.ATTD_400_054);
        }

        // 3) 사후 신청은 근태 마감 전까지만 (결정 #4)
        //    PRAFTA-028: 부서 단위 마감 도입 → 신청자 소속부서 기준으로 정밀 판정한다
        //    (다른 부서가 마감돼도 본 신청자 부서가 열려 있으면 사후 신청 허용).
        String today = todayYmd();
        if (workYmd.compareTo(today) < 0) {
            String closeYm = workYmd.substring(0, 6);
            if (attdCloseService.isClosedForUser(cmpny, site, user, closeYm)) {
                throw new ApiException(AttdErrorCode.ATTD_400_050);
            }
        }

        // 4) 차감 대상 부여 (잔여 충분, 만료 임박 우선)
        DeductibleGrantVO grant = leaveFlowMapper.selectDeductibleGrant(cmpny, user, leaveCd, workYmd, leaveDays);
        if (grant == null) {
            throw new ApiException(AttdErrorCode.ATTD_400_051);
        }

        // 5) 요청 생성 (REQ_TYPE='05'). 결재 Y면 신청('01'), N이면 즉시 승인('02').
        String reqId = leaveFlowMapper.selectNextReqId(cmpny);
        String reqStatus = aprvRequired ? REQ_APPLIED : REQ_APPROVED;
        leaveFlowMapper.insertLeaveReq(new LeaveReqInsertCommand(
                reqId, cmpny, site, user, reqStatus, p.reason(), workYmd, p.nodeCd(),
                workYmd, startTime, workYmd, endTime, p.leaveType(), leaveDays, user));

        // 6) 결재 Y → 라인 일괄 생성. 자기 승인 원칙(§9.5): 본인이 결재자인 단계는
        //    소속 노드 자체근태승인(SELF_ATTD_APPRV_YN) ON + 본인이 그 노드 담당 정/부일 때만
        //    자동승인('02'), 자격 미달이면 본인 지정 불가(ATTD_400_056).
        boolean fullyAutoApproved = false;
        if (aprvRequired) {
            List<String> approvers = p.approverUserCds();
            if (approvers == null || approvers.isEmpty()) {
                throw new ApiException(CommonErrorCode.COMMON_400_001);
            }
            boolean selfAllowed = "Y".equals(leaveFlowMapper.selectUserNodeSelfApproveYn(cmpny, user));

            int currentIdx = -1; // 첫 수동(비-자동승인) 단계 인덱스
            for (int i = 0; i < approvers.size(); i++) {
                boolean isSelf = user.equals(approvers.get(i));
                if (isSelf && !selfAllowed) {
                    throw new ApiException(AttdErrorCode.ATTD_400_056);
                }
                if (!isSelf && currentIdx < 0) {
                    currentIdx = i;
                }
            }
            for (int i = 0; i < approvers.size(); i++) {
                String approver = approvers.get(i);
                boolean isSelf = user.equals(approver);
                ApprovalStepVO s = new ApprovalStepVO();
                s.setReqId(reqId);
                s.setApprovalStep(i + 1);
                s.setCmpnyCd(cmpny);
                s.setApproverUserCd(approver);
                if (isSelf) {
                    s.setApprovalStatus(STEP_APPROVED); // 본인 + 자체근태승인 ON → 자동 승인
                    s.setApprovalComment("자체근태승인 자동 승인");
                } else {
                    s.setApprovalStatus(i == currentIdx ? STEP_APPLIED : STEP_WAIT);
                }
                s.setInsertNo(user);
                approvalLineMapper.insertApprovalStep(s);
            }
            fullyAutoApproved = (currentIdx < 0); // 전 단계가 본인 자동승인 → 즉시 확정
        }

        // 7) 차감 예약 (CONFIRMED) + 부여 USED_DAYS 동기화
        String leaveId = leaveFlowMapper.selectNextLeaveId(cmpny);
        LeaveUseVO use = LeaveUseVO.builder()
                .leaveId(leaveId).cmpnyCd(cmpny).siteCd(site).userCd(user).leaveCd(leaveCd)
                .reqId(reqId).grantId(grant.grantId())
                .startDate(workYmd).startTime(startTime).endDate(workYmd).endTime(endTime)
                .useUnitType(unit).leaveDays(leaveDays).leaveMinutes(leaveMinutes)
                .leaveReason(p.reason()).leaveStatus(USE_CONFIRMED).insertNo(user)
                .build();
        leaveFlowMapper.insertLeaveUse(use);
        leaveFlowMapper.recomputeGrantUsedDays(cmpny, grant.grantId(), user);

        // 결재 Y인데 전 단계가 본인 자동승인이면 요청 즉시 확정(§9.5 자기 승인 원칙)
        if (aprvRequired && fullyAutoApproved) {
            leaveFlowMapper.updateReqStatus(cmpny, reqId, REQ_APPROVED, user, "자체근태승인 자동 승인");
        }
        // 출근 차단(§8.3): 확정(결재 N 또는 전건 자동승인) + 일 단위(00)면 근무계획을 연차로 덮어 출근 차단.
        // 반차/시간차는 leave_use 기록 기반으로 출퇴근 단계에서 해당 구간 차단(check-in 연계).
        boolean confirmedNow = !aprvRequired || fullyAutoApproved;
        if (confirmedNow && UNIT_FULL.equals(unit)) {
            leaveFlowMapper.upsertWorkPlanLeave(cmpny, site, user, workYmd, leaveCd, user);
        }

        log.info("연차 신청 완료. reqId={}, user={}, leaveCd={}, unit={}, days={}, aprv={}",
                reqId, user, leaveCd, unit, leaveDays, aprvRequired);
    }

    /** PRAFTA-028 - 마감된 기간(신청자 부서)의 연차 결재(승인/반려)를 차단한다. */
    private void ensureLeaveNotClosed(String cmpnyCd, LeaveReqRowVO req) {
        String ym = (req.workYmd() != null && req.workYmd().length() >= 6)
                ? req.workYmd().substring(0, 6) : req.workYmd();
        if (attdCloseService.isClosedForUser(cmpnyCd, req.siteCd(), req.userCd(), ym)) {
            throw new ApiException(AttdErrorCode.ATTD_400_042);
        }
    }

    @Override
    @Transactional
    public void approveStep(LeaveApprovalActionParam p) {
        LeaveReqRowVO req = loadProcessableReq(p.gvCmpnyCd(), p.reqId());
        ApprovalStepVO step = loadCurrentStep(p, req);

        // PRAFTA-028 - 마감된 기간(신청자 부서)의 연차 결재 승인 차단
        ensureLeaveNotClosed(p.gvCmpnyCd(), req);

        approvalLineMapper.updateStepStatus(p.gvCmpnyCd(), p.reqId(), p.approvalStep(), STEP_APPROVED, p.comment(), p.gvUserCd());

        // 다음 대기('00') 단계로 진행(자기승인 자동승인 '02' 단계는 자동 skip). 없으면 최종 승인.
        Integer nextStep = approvalLineMapper.selectFirstWaitingStep(p.gvCmpnyCd(), p.reqId());
        if (nextStep != null) {
            approvalLineMapper.updateStepStatus(p.gvCmpnyCd(), p.reqId(), nextStep, STEP_APPLIED, null, p.gvUserCd());
        } else {
            leaveFlowMapper.updateReqStatus(p.gvCmpnyCd(), p.reqId(), REQ_APPROVED, p.gvUserCd(), p.comment());
            // PRAFTA-025: 06(연차수정)은 최종 승인 시 기존 사용기록(TARGET_ID)을 새 값으로 in-place 갱신,
            //   05(연차사용)는 기존대로 일 단위 출근 차단을 적용한다.
            if (REQ_TYPE_LEAVE_MODIFY.equals(req.reqType())) {
                applyLeaveModify(p.gvCmpnyCd(), req, p.gvUserCd());
                log.info("연차 수정 최종 승인. reqId={}, leaveId={}", p.reqId(), req.targetId());
            } else {
                applyFullDayAttendanceBlock(p.gvCmpnyCd(), p.reqId(), p.gvUserCd());
                log.info("연차 최종 승인. reqId={}, approver={}", p.reqId(), p.gvUserCd());
            }
        }
    }

    @Override
    @Transactional
    public void rejectStep(LeaveApprovalActionParam p) {
        LeaveReqRowVO req = loadProcessableReq(p.gvCmpnyCd(), p.reqId());
        loadCurrentStep(p, req);

        // PRAFTA-028 - 마감된 기간(신청자 부서)의 연차 결재 반려 차단
        ensureLeaveNotClosed(p.gvCmpnyCd(), req);

        // 반려 사유 필수(이력 보존 §9.5). 프론트 최소길이와 별개로 서버에서도 비어있음을 차단.
        if (p.comment() == null || p.comment().trim().isEmpty()) {
            throw new ApiException(AttdErrorCode.ATTD_400_057);
        }

        approvalLineMapper.updateStepStatus(p.gvCmpnyCd(), p.reqId(), p.approvalStep(), STEP_REJECTED, p.comment(), p.gvUserCd());
        leaveFlowMapper.updateReqStatus(p.gvCmpnyCd(), p.reqId(), REQ_REJECTED, p.gvUserCd(), p.comment());

        // PRAFTA-025: 06(연차수정) 반려는 기존 사용기록을 절대 건드리지 않는다.
        //   수정은 '최종 승인 시에만' 반영되므로, 반려 시점엔 되돌릴 변경이 없다.
        //   (05 반려 로직의 차감 해제/부여 동기화/출근차단 해제를 06에 적용하면 기존 휴가가 잘못 취소됨)
        if (REQ_TYPE_LEAVE_MODIFY.equals(req.reqType())) {
            log.info("연차 수정 반려(기존 휴가 유지). reqId={}, approver={}", p.reqId(), p.gvUserCd());
            return;
        }

        // 출근 차단 해제용 detail은 취소(LEAVE_STATUS 변경) 전에 확보
        LeaveUseDetailVO detail = leaveFlowMapper.selectLeaveUseDetailByReqId(p.gvCmpnyCd(), p.reqId());

        // 차감 해제 + 부여 동기화
        leaveFlowMapper.cancelLeaveUseByReqId(p.gvCmpnyCd(), p.reqId(), "결재 반려", p.gvUserCd());
        String grantId = leaveFlowMapper.selectGrantIdByReqId(p.gvCmpnyCd(), p.reqId());
        if (grantId != null) {
            leaveFlowMapper.recomputeGrantUsedDays(p.gvCmpnyCd(), grantId, p.gvUserCd());
        }
        // 출근 차단(일 단위) 해제 — 연차 블록(WORK_PLAN_CD=leaveCd)만 삭제한다.
        // 한계: 덮기 전 원 SCH_CD를 보관하지 않으므로 명시 근무계획이 있던 일자는 복원되지 않는다.
        //       (요청서 prafta-019-E §10.1 — 원 스케줄 복원은 별도 후속 작업)
        if (detail != null && UNIT_FULL.equals(detail.useUnitType())) {
            leaveFlowMapper.deleteWorkPlanLeave(p.gvCmpnyCd(), detail.siteCd(), detail.userCd(),
                    detail.startDate(), detail.leaveCd());
        }
        log.info("연차 반려. reqId={}, approver={}", p.reqId(), p.gvUserCd());
    }

    @Override
    public java.util.List<com.prafta.web.attd.leaveflow.vo.MyLeaveApprovalVO> getMyPendingLeaveApprovals(
            String cmpnyCd, String approverUserCd) {
        return leaveFlowMapper.selectMyPendingLeaveApprovals(cmpnyCd, approverUserCd);
    }

    @Override
    @Transactional
    public DirectLeaveResult recordDirectLeaveUsage(String cmpnyCd, String siteCd, String userCd,
                                                    String workYmd, String leaveCd, String operatorUserCd) {
        // 멱등: 동일 직원·일자·연차코드 직접 사용기록이 있으면 중복 차감 방지
        if (leaveFlowMapper.countDirectLeaveUse(cmpnyCd, userCd, workYmd, leaveCd) > 0) {
            return DirectLeaveResult.SKIPPED_DUP;
        }
        // 차감 대상 부여 1일 (동일 LEAVE_CD, 만료 임박순, FOR UPDATE). 없으면 잔여 부족.
        BigDecimal oneDay = BigDecimal.ONE;
        DeductibleGrantVO grant = leaveFlowMapper.selectDeductibleGrant(cmpnyCd, userCd, leaveCd, workYmd, oneDay);
        if (grant == null) {
            return DirectLeaveResult.INSUFFICIENT;
        }
        // 사용 기록(CONFIRMED, REQ_ID 없음 = 결재 없는 직접 차감) + 부여 USED_DAYS 동기화
        String leaveId = leaveFlowMapper.selectNextLeaveId(cmpnyCd);
        LeaveUseVO use = LeaveUseVO.builder()
                .leaveId(leaveId).cmpnyCd(cmpnyCd).siteCd(siteCd).userCd(userCd).leaveCd(leaveCd)
                .reqId(null).grantId(grant.grantId())
                .startDate(workYmd).startTime(null).endDate(workYmd).endTime(null)
                .useUnitType(UNIT_FULL).leaveDays(oneDay).leaveMinutes(null)
                .leaveReason("근무계획 연차 적용").leaveStatus(USE_CONFIRMED).insertNo(operatorUserCd)
                .build();
        try {
            leaveFlowMapper.insertLeaveUse(use);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 동시 저장 경합(TOCTOU): UK_LEAVE_USE_DIRECT(회사+직원+일자+연차코드) 유니크가 최종 차단.
            // 이미 기록된 것으로 보고 건너뜀(이중 차감 방지).
            log.info("근무계획 직접 연차 사용 - 멱등키 경합으로 건너뜀. cmpnyCd={}, userCd={}, workYmd={}, leaveCd={}",
                    cmpnyCd, userCd, workYmd, leaveCd);
            return DirectLeaveResult.SKIPPED_DUP;
        }
        leaveFlowMapper.recomputeGrantUsedDays(cmpnyCd, grant.grantId(), operatorUserCd);

        log.info("근무계획 직접 연차 사용 기록. cmpnyCd={}, userCd={}, workYmd={}, leaveCd={}, grantId={}",
                cmpnyCd, userCd, workYmd, leaveCd, grant.grantId());
        return DirectLeaveResult.RECORDED;
    }

    /** 요청 소유권(회사) + 처리 가능 상태('01' 신청) 검증. (D 인계: 회사 소유권 상위 검증) */
    private LeaveReqRowVO loadProcessableReq(String cmpnyCd, String reqId) {
        LeaveReqRowVO req = leaveFlowMapper.selectLeaveReq(cmpnyCd, reqId);
        if (req == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_030);
        }
        if (!REQ_APPLIED.equals(req.reqStatus())) {
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }
        return req;
    }

    /** 현재 단계 로드 + 결재자 본인·단계 순서('01' 신청 상태) 검증. (D 인계: 본인·순서 검증) */
    private ApprovalStepVO loadCurrentStep(LeaveApprovalActionParam p, LeaveReqRowVO req) {
        ApprovalStepVO step = approvalLineMapper.selectApprovalStep(p.gvCmpnyCd(), p.reqId(), p.approvalStep());
        if (step == null) {
            throw new ApiException(AttdErrorCode.ATTD_404_030);
        }
        if (!p.gvUserCd().equals(step.getApproverUserCd())) {
            log.warn("연차 결재 권한 없음 - reqId={}, step={}, actor={}, approver={}",
                    p.reqId(), p.approvalStep(), p.gvUserCd(), step.getApproverUserCd());
            throw new ApiException(AttdErrorCode.ATTD_403_030);
        }
        if (!STEP_APPLIED.equals(step.getApprovalStatus())) {
            // 아직 차례가 아니거나(대기 '00') 이미 처리된 단계
            throw new ApiException(AttdErrorCode.ATTD_400_053);
        }
        return step;
    }

    /** 일 단위(00) 휴가 확정 시 근무계획을 연차코드로 덮어 출근을 차단한다. 반차/시간차는 대상 아님. */
    private void applyFullDayAttendanceBlock(String cmpnyCd, String reqId, String actorUserCd) {
        LeaveUseDetailVO d = leaveFlowMapper.selectLeaveUseDetailByReqId(cmpnyCd, reqId);
        if (d != null && UNIT_FULL.equals(d.useUnitType())) {
            leaveFlowMapper.upsertWorkPlanLeave(cmpnyCd, d.siteCd(), d.userCd(), d.startDate(), d.leaveCd(), actorUserCd);
        }
    }

    /**
     * PRAFTA-025 - 연차 수정('06') 최종 승인 반영.
     *
     * <p>기존 사용기록(TARGET_ID=LEAVE_ID)을 요청이 담은 새 값(일자/시각/일수)으로 in-place
     * 갱신하고, 부여 USED_DAYS 를 재동기화하며, 일 단위 출근 차단 블록을 새 일자로 이동한다.
     *
     * <p>범위: 연차코드(LEAVE_CD)/차감 부여(GRANT_ID)/사용단위(USE_UNIT_TYPE)는 보존한다.
     * 일 단위↔반차/시간차 전환이나 연차 종류 변경은 요청 테이블이 사용단위/분을 담지 않아
     * 별도 신청(취소 후 재신청)으로 처리한다(스키마 확장 시 후속).
     */
    private void applyLeaveModify(String cmpnyCd, LeaveReqRowVO req, String actorUserCd) {
        String leaveId = req.targetId();
        if (leaveId == null || leaveId.isEmpty()) {
            throw new ApiException(AttdErrorCode.ATTD_404_030);
        }
        LeaveModifyTargetVO tgt = leaveFlowMapper.selectLeaveModifyTarget(cmpnyCd, leaveId);
        if (tgt == null) {
            // 대상 사용기록이 없거나 이미 취소됨 → 수정 대상 부재
            throw new ApiException(AttdErrorCode.ATTD_404_030);
        }

        // 새 차감 일수: 요청값 우선, 없으면 기존 유지.
        BigDecimal newDays = (req.leaveDays() != null) ? req.leaveDays() : tgt.leaveDays();

        // 잔여 가드: '본 건의 기존 차감을 되돌린 가용분' 안에서만 증액 허용.
        //   availableExclSelf = GRANT_DAYS - USED_DAYS + (본 건 기존 차감)
        if (tgt.grantDays() != null && tgt.usedDays() != null && tgt.leaveDays() != null) {
            BigDecimal availableExclSelf = tgt.grantDays()
                    .subtract(tgt.usedDays())
                    .add(tgt.leaveDays());
            if (newDays.compareTo(availableExclSelf) > 0) {
                throw new ApiException(AttdErrorCode.ATTD_400_051);
            }
        }

        // 기존 사용기록 in-place 갱신(연차코드/부여/사용단위 보존). 일 단위 기준이라 minutes는 null.
        int updated = leaveFlowMapper.updateLeaveUseModify(
                cmpnyCd, leaveId,
                req.startDate(), req.startTime(), req.endDate(), req.endTime(),
                newDays, null, actorUserCd);
        if (updated == 0) {
            throw new ApiException(AttdErrorCode.ATTD_404_030);
        }

        // 부여 USED_DAYS 재동기화(차감 일수 변동 반영).
        if (tgt.grantId() != null && !tgt.grantId().isEmpty()) {
            leaveFlowMapper.recomputeGrantUsedDays(cmpnyCd, tgt.grantId(), actorUserCd);
        }

        // 출근 차단(일 단위) 이동: 기존 일자 블록 삭제 후 새 일자 블록 설정.
        // (반차/시간차는 출근 단계에서 leave_use 기반으로 차단하므로 근무계획 미조정)
        if (UNIT_FULL.equals(tgt.useUnitType())) {
            leaveFlowMapper.deleteWorkPlanLeave(cmpnyCd, tgt.siteCd(), tgt.userCd(), tgt.startDate(), tgt.leaveCd());
            leaveFlowMapper.upsertWorkPlanLeave(cmpnyCd, tgt.siteCd(), tgt.userCd(), req.startDate(), tgt.leaveCd(), actorUserCd);
        }
    }

    private int unitMinutes(String unit) {
        if (UNIT_HOUR2.equals(unit)) return 120;
        if (UNIT_HOUR1.equals(unit)) return 60;
        return 30; // UNIT_MIN30
    }

    private String todayYmd() {
        LocalDate d = LocalDate.now();
        return String.format("%04d%02d%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }
}
