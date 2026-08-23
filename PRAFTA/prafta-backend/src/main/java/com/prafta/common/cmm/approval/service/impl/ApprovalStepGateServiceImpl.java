package com.prafta.common.cmm.approval.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.prafta.common.cmm.approval.mapper.ApprovalLineMapper;
import com.prafta.common.cmm.approval.service.ApprovalStepGateService;
import com.prafta.common.cmm.approval.vo.ApprovalStepVO;
import com.prafta.common.error.attd.AttdErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.common.util.AuthRoleUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link ApprovalStepGateService} 구현 (근태결재선통합 P1-1).
 *
 * <p>연차 {@code LeaveFlowServiceImpl.loadCurrentStep/approveStep}(827~940, 1157~1172행) 패턴 이식.
 * {@code @Transactional} 은 부여하지 않는다 — 호출부(attd07 승인/반려 메서드)의 트랜잭션에 참여한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalStepGateServiceImpl implements ApprovalStepGateService {

    // 결재 단계 상태 [SYS044] — leave 도메인과 동일 코드값(공유 테이블 TB_USER_ATTD_REQ_APPROVAL).
    private static final String STEP_WAIT = "00";
    private static final String STEP_APPLIED = "01";
    private static final String STEP_APPROVED = "02";

    private final ApprovalLineMapper approvalLineMapper;

    @Override
    public ApprovalStepVO resolveProcessableStep(String cmpnyCd, String reqId, String siteCd, String nodeCd,
                                                 String callerUserCd, String authCd, String insertNo) {
        List<ApprovalStepVO> steps = approvalLineMapper.selectApprovalLineByReqId(cmpnyCd, reqId);

        if (steps == null || steps.isEmpty()) {
            // §4 lazy 폴백: 결재선이 아직 없는(과거 조직도 위임 모델 잔재) 대기 REQ를 만나면
            //   그 자리에서 1단계 결재선을 합성해 INSERT 한다(idempotent — 이후 조회부터는 정상 흐름).
            //   기본 결재자 산출은 AttdApprovalLineServiceImpl.resolveApprovers 3번째 폴백과 동일
            //   형식(정 관리자 우선, 없으면 부 관리자)이다 — 로직 이원화 방지(§0-3).
            String defaultApprover = approvalLineMapper.selectDefaultApproverOfNode(cmpnyCd, siteCd, nodeCd);
            if (defaultApprover == null || defaultApprover.isBlank()) {
                log.warn("[approvalGate] lazy 결재선 생성 실패 - 승인 가능한 부서 관리자 없음. reqId={}, siteCd={}, nodeCd={}",
                        reqId, siteCd, nodeCd);
                throw new ApiException(AttdErrorCode.ATTD_400_105);
            }
            ApprovalStepVO lazyStep = new ApprovalStepVO();
            lazyStep.setReqId(reqId);
            lazyStep.setApprovalStep(1);
            lazyStep.setCmpnyCd(cmpnyCd);
            lazyStep.setApproverUserCd(defaultApprover);
            lazyStep.setApprovalStatus(STEP_APPLIED);
            lazyStep.setInsertNo(insertNo);
            approvalLineMapper.insertApprovalStep(lazyStep);
            log.info("[approvalGate] 레거시 orphan REQ에 lazy 결재선 생성(§4). reqId={}, defaultApprover={}",
                    reqId, defaultApprover);
            steps = List.of(lazyStep);
        }

        ApprovalStepVO current = null;
        for (ApprovalStepVO s : steps) {
            if (STEP_APPLIED.equals(s.getApprovalStatus())) {
                current = s;
                break;
            }
        }
        if (current == null) {
            // 전 단계 처리 완료(이미 승인/반려됨) 또는 이상 데이터.
            log.warn("[approvalGate] 처리 가능한 결재 단계 없음(이미 처리됨/이상 데이터). reqId={}", reqId);
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }

        // 마스터관리자 예외(공통정책서 §8.5): 전사 역할은 결재선 소유권과 무관하게 현재 단계 처리 허용.
        //   lazy 폴백으로 방금 생성한 단계라도 이 검증은 그대로 적용된다(결재선 생성≠처리 권한 부여).
        if (!AuthRoleUtils.canManageAllNodes(authCd) && !callerUserCd.equals(current.getApproverUserCd())) {
            log.warn("[approvalGate] 결재 권한 없음. reqId={}, step={}, caller={}, approver={}",
                    reqId, current.getApprovalStep(), callerUserCd, current.getApproverUserCd());
            throw new ApiException(AttdErrorCode.ATTD_403_002);
        }
        return current;
    }

    @Override
    public boolean advanceOrFinalize(String cmpnyCd, String reqId, ApprovalStepVO currentStep,
                                     String comment, String actorUserCd) {
        // P1 Medium 수정(security 지적·사용자 승인 08-23): 갱신 전 상태(APPLIED)를 WHERE 조건으로
        //   명시하고 영향행수를 검사한다. 지금까지는 MySQL 격리수준 + 호출부의 REQ_STATUS 낙관적
        //   락이 동시 처리를 우연히 막고 있었을 뿐, 이 UPDATE 자체는 조건 없이 항상 성공했다.
        int currentUpdated = approvalLineMapper.updateStepStatusGuarded(cmpnyCd, reqId, currentStep.getApprovalStep(),
                STEP_APPLIED, STEP_APPROVED, comment, actorUserCd);
        if (currentUpdated == 0) {
            log.warn("[approvalGate] 현재 단계 승인 갱신 실패(동시 처리 충돌/이미 처리됨). reqId={}, step={}",
                    reqId, currentStep.getApprovalStep());
            throw new ApiException(AttdErrorCode.ATTD_409_001);
        }

        Integer nextStep = approvalLineMapper.selectFirstWaitingStep(cmpnyCd, reqId);
        if (nextStep != null) {
            int nextUpdated = approvalLineMapper.updateStepStatusGuarded(cmpnyCd, reqId, nextStep,
                    STEP_WAIT, STEP_APPLIED, null, actorUserCd);
            if (nextUpdated == 0) {
                log.warn("[approvalGate] 다음 단계 신청 전환 실패(동시 처리 충돌/이미 처리됨). reqId={}, nextStep={}",
                        reqId, nextStep);
                throw new ApiException(AttdErrorCode.ATTD_409_001);
            }
            // TODO(developer): 차례 도래 PUSH — 본 서비스는 common 계층이라 app.req.req09 의 PUSH
            //   생산자(AttdApprovalNotiService)에 의존할 수 없다(계층 역행 방지). P1 은 결재선 진행
            //   정확성만 보장 범위이며, PUSH 연결은 P2 이후 이관 대상(요청서 §0-3 보정①).
            log.info("[approvalGate] 결재선 다음 단계로 진행(중간 단계 승인). reqId={}, nextStep={}",
                    reqId, nextStep);
            return false;
        }
        log.info("[approvalGate] 결재선 최종 단계 승인 완료. reqId={}, finalStep={}",
                reqId, currentStep.getApprovalStep());
        return true;
    }
}
