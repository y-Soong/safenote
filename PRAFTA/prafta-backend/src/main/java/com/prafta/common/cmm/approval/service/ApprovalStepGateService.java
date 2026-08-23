package com.prafta.common.cmm.approval.service;

import com.prafta.common.cmm.approval.vo.ApprovalStepVO;

/**
 * 결재선(TB_USER_ATTD_REQ_APPROVAL) 처리 공용 게이트 (근태결재선통합 P1-1).
 *
 * <p>연차 {@code LeaveFlowServiceImpl.loadCurrentStep/approveStep/rejectStep} 의 "현재 단계 소유권
 * 검증 + 단계 전진" 패턴을 근태보정/스케줄수정/초과근무(attd07) 도메인이 공유할 수 있도록 이식한
 * 공용 헬퍼다. leave 전용 필드(연차 차감 등)는 담지 않는다.
 *
 * <p>leave 의 loadCurrentStep 과 다른 점 2가지:
 * <ol>
 *   <li>마스터관리자 예외(공통정책서 §8.5) — 처리자가 master/hr/safe({@code AuthRoleUtils.canManageAllNodes})
 *       면 결재선 소유권과 무관하게 현재 단계 처리를 허용한다(연차는 이 예외가 없다).</li>
 *   <li>레거시 orphan REQ 의 lazy 결재선 생성 — 결재선이 아직 없는(과거 조직도 위임 모델로 생성된) 대기
 *       REQ 를 만나면 그 자리에서 1단계 결재선을 합성해 INSERT 한 뒤 정상 흐름을 태운다.</li>
 * </ol>
 *
 * <p>정책서: {@code common/08-permissions.md} §8.5, {@code common/07-organization.md} §7.2/§7.3.
 */
public interface ApprovalStepGateService {

    /**
     * 처리 가능한 현재 결재 단계를 조회한다. 결재선이 없으면(orphan) 그 노드의 기본 결재자로
     * 1단계를 lazy 생성한다. 처리자가 master/hr/safe 가 아니면 그 단계의 지정 결재자 본인만 통과한다.
     *
     * @param cmpnyCd      회사 코드
     * @param reqId        요청 ID(tb_user_attd_req)
     * @param siteCd       사업장 코드(REQ 의 권위값 — lazy 폴백 기본 결재자 산출에 사용)
     * @param nodeCd       노드 코드(REQ 의 권위값 — lazy 폴백 기본 결재자 산출에 사용)
     * @param callerUserCd 처리자(현재 로그인 사용자)
     * @param authCd       처리자 권한코드(마스터관리자 예외 판정)
     * @param insertNo     lazy 결재선 생성 시 적재자로 남길 사용자 코드(보통 처리자 본인)
     * @return 처리 가능한(신청 상태 '01') 현재 단계 행
     * @throws com.prafta.common.exception.ApiException 결재선 생성 실패(관리자 0명)/이미 처리됨/권한 없음
     */
    ApprovalStepVO resolveProcessableStep(String cmpnyCd, String reqId, String siteCd, String nodeCd,
                                          String callerUserCd, String authCd, String insertNo);

    /**
     * 현재 단계를 승인 처리하고 결재선을 전진시킨다.
     *
     * <p>다음 대기('00') 단계가 있으면 그 단계를 신청('01')으로 전환하고 {@code false}(중간 단계)를
     * 반환한다 — 이 경우 호출부는 REQ_STATUS 를 확정하지 않고 실제 데이터(MGMT/OT/work_plan) 반영도
     * 하지 않는다. 다음 대기 단계가 없으면(전 단계 처리 완료) {@code true}(최종 단계)를 반환한다 —
     * 이 경우에만 호출부가 REQ_STATUS='02' 확정 + 실제 데이터 반영을 수행해야 한다.
     *
     * @param cmpnyCd     회사 코드
     * @param reqId       요청 ID
     * @param currentStep {@link #resolveProcessableStep} 이 반환한 현재 단계
     * @param comment     결재 코멘트(처리자 입력, nullable)
     * @param actorUserCd 처리자
     * @return 최종 단계(전 단계 처리 완료) 여부
     */
    boolean advanceOrFinalize(String cmpnyCd, String reqId, ApprovalStepVO currentStep,
                              String comment, String actorUserCd);
}
