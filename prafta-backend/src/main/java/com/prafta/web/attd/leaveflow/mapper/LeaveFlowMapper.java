package com.prafta.web.attd.leaveflow.mapper;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.leaveflow.application.command.LeaveReqInsertCommand;
import com.prafta.web.attd.leaveflow.vo.DeductibleGrantVO;
import com.prafta.web.attd.leaveflow.vo.LeaveReqRowVO;
import com.prafta.web.attd.leaveflow.vo.LeaveTypeInfoVO;
import com.prafta.web.attd.leaveflow.vo.LeaveUseVO;

/**
 * 연차 신청·결재 흐름 전용 Mapper (prafta-019-E).
 */
@Mapper
public interface LeaveFlowMapper {

    /** 요청 ID 채번 (CONCAT(YYYYMMDD, seq)). */
    String selectNextReqId(@Param("cmpnyCd") String cmpnyCd);

    /** 연차 사용 ID 채번. */
    String selectNextLeaveId(@Param("cmpnyCd") String cmpnyCd);

    /** 연차 타입 메타(결재여부/시스템시드/사용단위) 조회. 없으면 null. */
    LeaveTypeInfoVO selectLeaveTypeInfo(@Param("cmpnyCd") String cmpnyCd,
                                        @Param("leaveCd") String leaveCd);

    /** 활성 법정 정책의 결재 여부 (tb_leave_policy.APRV_USE_YN). 없으면 null. */
    String selectPolicyAprvUseYn(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 차감 대상 부여 1건 — 활성/유효기간 포함/잔여 충분, 만료 임박(AVAIL_TO_DATE) 우선.
     * 충분한 잔여를 가진 grant가 없으면 null.
     */
    DeductibleGrantVO selectDeductibleGrant(@Param("cmpnyCd") String cmpnyCd,
                                            @Param("userCd") String userCd,
                                            @Param("leaveCd") String leaveCd,
                                            @Param("workYmd") String workYmd,
                                            @Param("neededDays") BigDecimal neededDays);

    /** 연차 요청(tb_user_attd_req REQ_TYPE='05') INSERT. */
    int insertLeaveReq(LeaveReqInsertCommand command);

    /** 요청 상태 전이 + 처리자/코멘트 기록. */
    int updateReqStatus(@Param("cmpnyCd") String cmpnyCd,
                        @Param("reqId") String reqId,
                        @Param("reqStatus") String reqStatus,
                        @Param("processUserCd") String processUserCd,
                        @Param("processComment") String processComment);

    /** 요청 단건 조회(소유권/상태 확인). 없으면 null. */
    LeaveReqRowVO selectLeaveReq(@Param("cmpnyCd") String cmpnyCd,
                                 @Param("reqId") String reqId);

    /** 연차 사용(차감) INSERT. */
    int insertLeaveUse(LeaveUseVO use);

    /**
     * 근무계획 직접 입력(REQ_ID 없음) 연차 사용 기록 존재 카운트 (prafta-021 멱등 — 중복 차감 방지).
     * 동일 직원·일자(START_DATE)·연차코드의 CONFIRMED, REQ_ID NULL 행을 센다.
     */
    int countDirectLeaveUse(@Param("cmpnyCd") String cmpnyCd,
                            @Param("userCd") String userCd,
                            @Param("workYmd") String workYmd,
                            @Param("leaveCd") String leaveCd);

    /** 요청에 연결된 연차 사용 취소(반려 시 차감 해제). */
    int cancelLeaveUseByReqId(@Param("cmpnyCd") String cmpnyCd,
                              @Param("reqId") String reqId,
                              @Param("cancelReason") String cancelReason,
                              @Param("updateNo") String updateNo);

    /** 부여의 USED_DAYS를 확정 사용 합계로 재계산(차감/해제 후 동기화). */
    int recomputeGrantUsedDays(@Param("cmpnyCd") String cmpnyCd,
                               @Param("grantId") String grantId,
                               @Param("updateNo") String updateNo);

    /** 요청에 연결된 사용 행의 GRANT_ID 조회(반려 시 부여 재계산 대상). 없으면 null. */
    String selectGrantIdByReqId(@Param("cmpnyCd") String cmpnyCd,
                                @Param("reqId") String reqId);

    /** 요청에 연결된 확정 사용의 핵심 정보(출근차단/finalize용). 없으면 null. */
    com.prafta.web.attd.leaveflow.vo.LeaveUseDetailVO selectLeaveUseDetailByReqId(
            @Param("cmpnyCd") String cmpnyCd, @Param("reqId") String reqId);

    /** 출근 차단: 해당 일자 근무계획 WORK_PLAN_CD를 연차코드로 upsert(일 단위 휴가 시 출근 차단). */
    int upsertWorkPlanLeave(@Param("cmpnyCd") String cmpnyCd,
                            @Param("siteCd") String siteCd,
                            @Param("userCd") String userCd,
                            @Param("workYmd") String workYmd,
                            @Param("leaveCd") String leaveCd,
                            @Param("insertNo") String insertNo);

    /** 출근 차단 해제: 우리가 설정한 연차 블록(WORK_PLAN_CD=leaveCd)만 삭제(반려/취소 시). */
    int deleteWorkPlanLeave(@Param("cmpnyCd") String cmpnyCd,
                            @Param("siteCd") String siteCd,
                            @Param("userCd") String userCd,
                            @Param("workYmd") String workYmd,
                            @Param("leaveCd") String leaveCd);

    /** 내 결재함: 내가 현재 단계('01' 신청) 결재자인 연차 요청 목록(상세 포함). */
    java.util.List<com.prafta.web.attd.leaveflow.vo.MyLeaveApprovalVO> selectMyPendingLeaveApprovals(
            @Param("cmpnyCd") String cmpnyCd, @Param("approverUserCd") String approverUserCd);

    /** 사용자 소속 노드의 자체근태승인 여부(SELF_ATTD_APPRV_YN). 자기 승인 원칙(§9.5) 판정용. 없으면 null. */
    String selectUserNodeSelfApproveYn(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd);

    /**
     * PRAFTA-025 - 연차 수정('06') 승인 대상 사용기록 + 차감 부여 잔여 조회.
     * 대상 LEAVE_ID 의 CONFIRMED 사용기록과 그 GRANT 의 GRANT_DAYS/USED_DAYS 를 함께 싣는다.
     * 없거나 취소된 기록이면 null.
     */
    com.prafta.web.attd.leaveflow.vo.LeaveModifyTargetVO selectLeaveModifyTarget(
            @Param("cmpnyCd") String cmpnyCd, @Param("leaveId") String leaveId);

    /**
     * PRAFTA-025 - 연차 수정('06') 승인: 기존 사용기록(LEAVE_ID)을 새 값으로 in-place 갱신한다.
     * 연차코드(LEAVE_CD)/차감 부여(GRANT_ID)/사용단위(USE_UNIT_TYPE)는 보존하고
     * 사용 일자·시각·일수·분·사유만 갱신한다. CONFIRMED + 미삭제 행만 대상.
     * @return 영향받은 행 수 (0 또는 1)
     */
    int updateLeaveUseModify(@Param("cmpnyCd") String cmpnyCd,
                             @Param("leaveId") String leaveId,
                             @Param("startDate") String startDate,
                             @Param("startTime") String startTime,
                             @Param("endDate") String endDate,
                             @Param("endTime") String endTime,
                             @Param("leaveDays") java.math.BigDecimal leaveDays,
                             @Param("leaveMinutes") Integer leaveMinutes,
                             @Param("updateNo") String updateNo);
}
