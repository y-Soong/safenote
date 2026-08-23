package com.prafta.common.cmm.approval.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.common.cmm.approval.vo.ApprovalStepVO;

/**
 * 연차 요청 결재라인 저장/조회 Mapper (prafta-019-D 제공, 작업 E 소비).
 *
 * <p>정책서: 재기획서 §9.1/§9.2, 근태 §9.5.
 */
@Mapper
public interface ApprovalLineMapper {

    /** 결재 단계 1행 INSERT (라인 일괄 생성 시 단계별 호출 — 작업 E). */
    int insertApprovalStep(ApprovalStepVO step);

    /** 요청별 결재라인 조회 (단계 오름차순, 결재자명·상태명 포함). */
    List<ApprovalStepVO> selectApprovalLineByReqId(@Param("cmpnyCd") String cmpnyCd,
                                                   @Param("reqId") String reqId);

    /**
     * 단계 상태 갱신 (승인/반려/신청 전환 — 작업 E).
     *
     * <p>leave 도메인({@code LeaveFlowServiceImpl}) 전용. 갱신 전 상태 조건이 없어 동시 처리
     * 방지를 스스로 보장하지 않는다(P1 Medium 이슈 — 근태결재선통합 §0-3). attd07 계열은
     * {@link #updateStepStatusGuarded} 를 사용한다.
     */
    int updateStepStatus(@Param("cmpnyCd") String cmpnyCd,
                         @Param("reqId") String reqId,
                         @Param("approvalStep") Integer approvalStep,
                         @Param("approvalStatus") String approvalStatus,
                         @Param("approvalComment") String approvalComment,
                         @Param("processUserCd") String processUserCd);

    /**
     * 단계 상태 갱신(가드 버전, 근태결재선통합 P1 Medium 수정 — security 지적·사용자 승인 08-23).
     * 갱신 전 상태가 {@code fromStatus} 일 때만 UPDATE 한다(WHERE 절에 APPROVAL_STATUS 조건 추가).
     * 영향행수가 0이면(동시 처리로 이미 다른 상태로 전이됨) 호출부가 {@code ApiException
     * (AttdErrorCode.ATTD_409_001)} 을 던져야 한다 — 같은 파일 다른 UPDATE 들의 "영향행수 가드"
     * 패턴과 동일. attd07/{@code ApprovalStepGateServiceImpl} 전용, leave 도메인은 여전히
     * {@link #updateStepStatus} 를 사용한다(이번 작업 범위 밖).
     */
    int updateStepStatusGuarded(@Param("cmpnyCd") String cmpnyCd,
                                @Param("reqId") String reqId,
                                @Param("approvalStep") Integer approvalStep,
                                @Param("fromStatus") String fromStatus,
                                @Param("approvalStatus") String approvalStatus,
                                @Param("approvalComment") String approvalComment,
                                @Param("processUserCd") String processUserCd);

    /**
     * 요청의 결재라인 전체 삭제 (재구성 시 — 선택).
     *
     * <p>★테넌트 격리: REQ_ID 는 회사별 채번이라 전역 유일하지 않다. 반드시 CMPNY_CD 를 동반해야
     *   다른 회사의 같은 REQ_ID 결재선을 지우지 않는다(updateStepStatus 와 동일 규약).
     */
    int deleteApprovalLineByReqId(@Param("cmpnyCd") String cmpnyCd,
                                  @Param("reqId") String reqId);

    /** 특정 단계의 결재자 본인 여부 검증 등에 사용할 단건 조회. */
    ApprovalStepVO selectApprovalStep(@Param("cmpnyCd") String cmpnyCd,
                                      @Param("reqId") String reqId,
                                      @Param("approvalStep") Integer approvalStep);

    /** 요청의 첫 대기('00') 단계 번호. 없으면 null(=모든 단계 처리됨). 다음 활성 단계 판정용. */
    Integer selectFirstWaitingStep(@Param("cmpnyCd") String cmpnyCd, @Param("reqId") String reqId);

    /**
     * 근태결재선통합 P1(§0-5/§4): 노드의 기본 결재자 — 정 관리자(MAIN_ADMIN_CD) 우선, 없으면
     * 부 관리자(SUB_ADMIN_CD). 조상 노드 확장 없음(그 노드 1건만 — canManageNode 의 cascade 와
     * 다른 개념).
     *
     * <p>결재자 미지정 신규 신청의 기본 결재자(P1-2, {@code AttdApprovalLineServiceImpl
     * .resolveApprovers} 3번째 폴백)와 레거시 orphan REQ 의 lazy 결재선 생성(P1-1
     * {@code ApprovalStepGateService.resolveProcessableStep})이 동일 형식(MAIN→SUB)을 공유해야
     * 한다 — 두 로직이 갈리면 lazy 폴백과 신규 신청의 기본 결재자가 달라지는 버그가 난다.
     * {@code AppReq09Mapper.selectDefaultApproverForUser} 와 동일 형식(모듈 독립을 위해 복제).
     */
    String selectDefaultApproverOfNode(@Param("cmpnyCd") String cmpnyCd,
                                       @Param("siteCd") String siteCd,
                                       @Param("nodeCd") String nodeCd);
}
