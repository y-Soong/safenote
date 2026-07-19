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

    /** 단계 상태 갱신 (승인/반려/신청 전환 — 작업 E). */
    int updateStepStatus(@Param("cmpnyCd") String cmpnyCd,
                         @Param("reqId") String reqId,
                         @Param("approvalStep") Integer approvalStep,
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
}
