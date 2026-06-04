package com.prafta.app.req.req09.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.app.mypage.mypage01.mapper.AppMypage01Mapper;
import com.prafta.app.mypage.mypage01.result.PresetStepResult;
import com.prafta.app.req.req09.mapper.AppReq09Mapper;
import com.prafta.app.req.req09.service.AttdApprovalLineService;
import com.prafta.app.req.req09.service.AttdApprovalNotiService;
import com.prafta.common.cmm.approval.mapper.ApprovalLineMapper;
import com.prafta.common.cmm.approval.vo.ApprovalStepVO;
import com.prafta.common.cmm.leave.mapper.LeaveApprovalNotiMapper;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.error.common.CommonErrorCode;
import com.prafta.common.exception.ApiException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 근태 요청 결재 분기/라인 INSERT 공용 서비스 구현 (PRAFTA-APP-009-3).
 *
 * <p>req07 등록 트랜잭션 안에서 호출된다({@code @Transactional} 미부여 — 호출부 트랜잭션 참여).
 * 결재라인 INSERT 는 요청 INSERT 와 원자적으로 커밋/롤백되고, PUSH 적재만 예외 격리한다.
 *
 * <p>연차 {@code AppLeaveFlowServiceImpl#submitLeave}(336~392) 의 'N' 결재라인 생성 패턴을 미러한다
 * (신규 발명 최소). 'Y' 분기는 근태 전용(D3/D4/D5).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttdApprovalLineServiceImpl implements AttdApprovalLineService {

    private final AppReq09Mapper appReq09Mapper;
    private final ApprovalLineMapper approvalLineMapper;
    private final AppMypage01Mapper appMypage01Mapper;
    private final LeaveApprovalNotiMapper leaveApprovalNotiMapper;
    private final AttdApprovalNotiService attdApprovalNotiService;

    // 요청 상태 [SYS033]
    private static final String REQ_APPROVED = "02";
    // 결재 단계 상태 [SYS044]
    private static final String STEP_WAIT = "00";
    private static final String STEP_APPLIED = "01";
    private static final String STEP_APPROVED = "02";

    private static final String SELF_APPROVE_COMMENT = "자체근태승인 자동 승인";

    @Override
    public void applyApprovalFlow(String cmpnyCd, String siteCd, String userCd, String reqId,
                                  List<String> approverUserCds, String presetId, String insertNo) {

        // 신청자 소속 노드의 자체근태승인 여부(D2). null → 'N' 취급(결재라인 다단계).
        String selfApprvYn = appReq09Mapper.selectAttdSelfApprvYn(cmpnyCd, siteCd, userCd);

        if (isYes(selfApprvYn)) {
            applyAttdSelfApprove(cmpnyCd, siteCd, userCd, reqId, insertNo);
        } else {
            applyApprovalLine(cmpnyCd, siteCd, userCd, reqId, approverUserCds, presetId, insertNo);
        }
    }

    /**
     * 'Y' 자체근태승인 분기(D3/D4/D5). 결재라인은 INSERT 하지 않는다.
     * <ul>
     *   <li>신청자=노드 정/부 관리자 → 즉시 자동승인(REQ_STATUS='02'), PUSH 미발송 (D4).</li>
     *   <li>일반 근로자 → 노드 관리자 0명이면 ATTD_400_105(D5), 1명 이상이면 승인 요망 PUSH(D3).</li>
     * </ul>
     */
    private void applyAttdSelfApprove(String cmpnyCd, String siteCd, String userCd, String reqId,
                                      String insertNo) {
        boolean isNodeAdmin = appReq09Mapper.selectIsNodeAdmin(cmpnyCd, siteCd, userCd) > 0;
        if (isNodeAdmin) {
            // D4: 신청자가 그 노드 관리자 → 즉시 자동승인. PUSH 미발송.
            appReq09Mapper.updateReqStatus(cmpnyCd, reqId, REQ_APPROVED, userCd, SELF_APPROVE_COMMENT);
            log.info("[attdAprvLine] 'Y' 자체근태승인 — 신청자=노드관리자 즉시 자동승인 (reqId={}, userCd={})",
                    reqId, userCd);
            return;
        }

        // D5: 일반 근로자 — 승인 가능한 노드 Main/Sub 관리자가 0명이면 설정오류(fail-closed).
        //   정상 흐름에서는 발생하지 않는다(prafta-046 구조 차단). 최소 방어.
        List<String> admins = leaveApprovalNotiMapper.selectNodeAdmins(cmpnyCd, siteCd, userCd);
        if (admins == null || admins.isEmpty()) {
            log.info("[attdAprvLine] 'Y' 자체근태승인 — 노드 관리자 0명 설정오류 (reqId={}, userCd={})",
                    reqId, userCd);
            throw new ApiException(AttdErrorCode.ATTD_400_105);
        }

        // D3: 승인 요망 PUSH(노드 관리자 전원). 결재라인 미INSERT — 현행 단일승인 경로 유지.
        attdApprovalNotiService.notifyAttdApprovalRequest(cmpnyCd, siteCd, userCd, reqId, insertNo);
        log.info("[attdAprvLine] 'Y' 자체근태승인 — 일반 근로자 승인 요망 PUSH 적재 (reqId={}, 관리자수={})",
                reqId, admins.size());
    }

    /**
     * 'N' 결재라인 다단계 생성(D6/D7/D8). 연차 submitLeave 336~392 미러.
     */
    private void applyApprovalLine(String cmpnyCd, String siteCd, String userCd, String reqId,
                                   List<String> approverUserCds, String presetId, String insertNo) {

        List<String> approvers = resolveApprovers(cmpnyCd, userCd, approverUserCds, presetId);
        if (approvers == null || approvers.isEmpty()) {
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // D8 결재자 스코프 가드: 본문 결재자가 동일 회사+사업장+재직 사용자인지 서버 검증.
        //   타 사업장/회사/존재없는 USER_CD 주입 → 그 결재함에 신청 노출되는 cross-tenant 침해 차단.
        List<String> distinctApprovers = new ArrayList<>(new LinkedHashSet<>(approvers));
        int validCount = appReq09Mapper.countValidApprovers(cmpnyCd, siteCd, distinctApprovers);
        if (validCount != distinctApprovers.size()) {
            log.info("[attdAprvLine] 결재선 거부: 유효하지 않은 결재자 포함 (reqId={}, 요청={}, 유효={})",
                    reqId, distinctApprovers.size(), validCount);
            throw new ApiException(CommonErrorCode.COMMON_400_001);
        }

        // D7 자기승인 자격: 본인 지정은 노드 자체근태승인 + 노드관리자일 때만.
        boolean selfAllowed = isYes(appReq09Mapper.selectUserNodeSelfApproveYn(cmpnyCd, userCd));

        int currentIdx = -1; // 첫 수동(비-자동승인) 단계 인덱스
        for (int i = 0; i < approvers.size(); i++) {
            boolean isSelf = userCd.equals(approvers.get(i));
            if (isSelf && !selfAllowed) {
                throw new ApiException(AttdErrorCode.ATTD_400_056);
            }
            if (!isSelf && currentIdx < 0) {
                currentIdx = i;
            }
        }

        for (int i = 0; i < approvers.size(); i++) {
            String approver = approvers.get(i);
            boolean isSelf = userCd.equals(approver);
            ApprovalStepVO s = new ApprovalStepVO();
            s.setReqId(reqId);
            s.setApprovalStep(i + 1);
            s.setCmpnyCd(cmpnyCd);
            s.setApproverUserCd(approver);
            if (isSelf) {
                s.setApprovalStatus(STEP_APPROVED); // 본인 + 자체근태승인 ON → 자동 승인
                s.setApprovalComment(SELF_APPROVE_COMMENT);
            } else {
                s.setApprovalStatus(i == currentIdx ? STEP_APPLIED : STEP_WAIT);
            }
            s.setInsertNo(insertNo);
            approvalLineMapper.insertApprovalStep(s);
        }

        boolean fullyAutoApproved = (currentIdx < 0); // 전 단계가 본인 자동승인 → 즉시 확정
        if (fullyAutoApproved) {
            appReq09Mapper.updateReqStatus(cmpnyCd, reqId, REQ_APPROVED, userCd, SELF_APPROVE_COMMENT);
            log.info("[attdAprvLine] 'N' 결재라인 전 단계 자동승인 → 즉시 확정 (reqId={}, 단계수={})",
                    reqId, approvers.size());
            return;
        }

        // 차례가 도래한 첫 수동 단계 결재자에게 차례 도래 PUSH(예외 격리는 noti 내부).
        String turnApprover = approvers.get(currentIdx);
        int turnStep = currentIdx + 1; // approvalStep 은 1-based
        attdApprovalNotiService.notifyAttdApprovalTurn(cmpnyCd, siteCd, userCd, reqId, turnStep, turnApprover, insertNo);
        log.info("[attdAprvLine] 'N' 결재라인 생성 완료 (reqId={}, 단계수={}, 차례단계={})",
                reqId, approvers.size(), turnStep);
    }

    /**
     * 결재자 목록 결정: approverUserCds 1차, 비어있고 presetId 가 있으면 본인 소유 프리셋을 전개.
     * 프리셋 전개는 mypage01 {@code selectPresetStepsById}(소유자 스코프) 재사용(타인 프리셋 차단).
     */
    private List<String> resolveApprovers(String cmpnyCd, String userCd,
                                          List<String> approverUserCds, String presetId) {
        if (approverUserCds != null && !approverUserCds.isEmpty()) {
            return approverUserCds;
        }
        if (presetId == null || presetId.isBlank()) {
            return approverUserCds;
        }
        List<PresetStepResult> steps =
                appMypage01Mapper.selectPresetStepsById(cmpnyCd, userCd, presetId);
        List<String> expanded = new ArrayList<>(steps.size());
        for (PresetStepResult s : steps) {
            if (s.approverUserCd() != null && !s.approverUserCd().isBlank()) {
                expanded.add(s.approverUserCd());
            }
        }
        return expanded;
    }

    /** 'Y'(대소문자 무관) → true. null/그외 → false. */
    private boolean isYes(String yn) {
        return "Y".equalsIgnoreCase(yn);
    }
}
