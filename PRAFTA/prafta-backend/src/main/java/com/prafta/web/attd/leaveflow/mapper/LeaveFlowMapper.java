package com.prafta.web.attd.leaveflow.mapper;

import java.math.BigDecimal;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.leaveflow.application.command.LeaveReqInsertCommand;
import com.prafta.web.attd.leaveflow.vo.AutoDeductibleGrantVO;
import com.prafta.web.attd.leaveflow.vo.DeductibleGrantVO;
import com.prafta.web.attd.leaveflow.vo.LeaveReqRowVO;
import com.prafta.web.attd.leaveflow.vo.LeaveTypeInfoVO;
import com.prafta.web.attd.leaveflow.vo.LeaveUseVO;

/**
 * 연차 신청·결재 흐름 전용 Mapper (prafta-019-E).
 */
@Mapper
public interface LeaveFlowMapper {

    /**
     * 같은 날 이미 점유된 연차 일수(중복 등록 가드, 앱 AppLeaveFlowMapper 미러).
     * 해당 일자(START_DATE~END_DATE 포함)의 CONFIRMED·미삭제 연차를 합산하되 종일(USE_UNIT_TYPE='00')은 1.0,
     * 그 외(반차/시간차)는 LEAVE_DAYS 로 본다. 호출부는 (점유 + 신규) &gt; 1.0 이면 ATTD_400_111 로 거부.
     */
    BigDecimal selectOccupiedLeaveDaysOnDate(@Param("cmpnyCd") String cmpnyCd,
                                             @Param("userCd") String userCd,
                                             @Param("workYmd") String workYmd);

    /**
     * 같은 날 시간대가 겹치는 시간차 연차(USE_UNIT_TYPE in '02','03','04') 개수(앱 미러).
     * 좌폐우개 겹침: 기존시작 &lt; 신규종료 AND 기존종료 &gt; 신규시작. &gt; 0 이면 ATTD_400_112 로 거부.
     */
    int countOverlappingTimeLeaveOnDate(@Param("cmpnyCd") String cmpnyCd,
                                        @Param("userCd") String userCd,
                                        @Param("workYmd") String workYmd,
                                        @Param("startTime") String startTime,
                                        @Param("endTime") String endTime);

    /** 요청 ID 채번 (CONCAT(YYYYMMDD, seq)). */
    String selectNextReqId(@Param("cmpnyCd") String cmpnyCd);

    /** 연차 사용 ID 채번. */
    String selectNextLeaveId(@Param("cmpnyCd") String cmpnyCd);

    /** 연차 타입 메타(결재여부/시스템시드/사용단위/타입/최대신청일수) 조회. 없으면 null. */
    LeaveTypeInfoVO selectLeaveTypeInfo(@Param("cmpnyCd") String cmpnyCd,
                                        @Param("leaveCd") String leaveCd);

    /**
     * 연차개편(사용자 신청 '01' 한도검증): 당해 회계연도 CONFIRMED 사용 합계(Σ LEAVE_DAYS).
     * 술어 LEAVE_STATUS='CONFIRMED' AND DEL_YN='N' (반려/취소 제외). 합계 없으면 0. (앱 미러)
     */
    BigDecimal selectFiscalUsedDays(@Param("cmpnyCd") String cmpnyCd,
                                    @Param("userCd") String userCd,
                                    @Param("leaveCd") String leaveCd,
                                    @Param("fiscalStartYmd") String fiscalStartYmd,
                                    @Param("fiscalEndYmdExclusive") String fiscalEndYmdExclusive);

    /**
     * prafta-com-016-B(3-1): 사용자 신청 '01' + 사용가능기간 '01'(설정안함=전체 누적) 한도검증용.
     * 회계연도 경계 없이 전체 CONFIRMED 사용 합계(Σ LEAVE_DAYS). 술어는 selectFiscalUsedDays 와 동일. 합계 없으면 0. (앱 미러)
     */
    BigDecimal selectTotalUsedDays(@Param("cmpnyCd") String cmpnyCd,
                                   @Param("userCd") String userCd,
                                   @Param("leaveCd") String leaveCd);

    /**
     * 연차개편 동시성: 사용자 신청('01') 직렬화용 advisory lock 획득(GET_LOCK).
     * 1=획득, 0=타임아웃, null=오류. 세션 단위 → 호출부가 트랜잭션 완료(afterCompletion)
     * 시점에 releaseAdvisoryLock (등록 불가 시 finally 폴백 — 보안리뷰 Medium). (앱 미러)
     */
    Integer getAdvisoryLock(@Param("lockKey") String lockKey, @Param("timeoutSec") int timeoutSec);

    /** 연차개편 동시성: advisory lock 해제(RELEASE_LOCK). */
    Integer releaseAdvisoryLock(@Param("lockKey") String lockKey);

    /** 활성 법정 정책의 결재 여부 (tb_leave_policy.APRV_USE_YN). 없으면 null. */
    String selectPolicyAprvUseYn(@Param("cmpnyCd") String cmpnyCd);

    /**
     * LC-10: 활성 법정정책의 사용 단위(tb_leave_usage_policy.USAGE_UNIT).
     * 반반차 허용 여부는 이 값이 'QUARTER_DAY' 인지로 판정한다(구 ALLOW_QUARTER 토글 폐기).
     * 사용정책 행 미존재/활성정책 없음이면 null → 호출부는 비허용 취급(fail-closed).
     */
    String selectPolicyUsageUnit(@Param("cmpnyCd") String cmpnyCd);

    /**
     * PRAFTA-COM-004 보안: 주어진 USER_CD 목록 중 동일 회사 + 동일 사업장 + 재직(활성) 상태인
     * 사용자 수를 센다. 호출부는 중복 제거한 결재자 수와 비교하여 불일치(타 사업장/회사/비재직/존재없음)
     * 시 거부한다(cross-site/cross-company 결재자 주입 차단 — 앱 AppLeaveFlowMapper.countValidApprovers 미러).
     */
    int countValidApprovers(@Param("cmpnyCd") String cmpnyCd,
                            @Param("siteCd") String siteCd,
                            @Param("userCds") java.util.List<String> userCds);

    /**
     * 차감 대상 부여 1건 — 활성/유효기간 포함/잔여 충분, 만료 임박(AVAIL_TO_DATE) 우선.
     * 충분한 잔여를 가진 grant가 없으면 null.
     */
    DeductibleGrantVO selectDeductibleGrant(@Param("cmpnyCd") String cmpnyCd,
                                            @Param("userCd") String userCd,
                                            @Param("leaveCd") String leaveCd,
                                            @Param("workYmd") String workYmd,
                                            @Param("neededDays") BigDecimal neededDays);

    /**
     * prafta-com-016-C-4: 소멸 임박 통합순 자동 차감 대상 부여 1건 — 후보 휴가코드(연차/월차) 전체에서
     * 만료 임박(AVAIL_TO_DATE) 우선. 차감할 LEAVE_CD 를 함께 반환한다. 없으면 null.
     */
    AutoDeductibleGrantVO selectAutoDeductibleGrant(@Param("cmpnyCd") String cmpnyCd,
                                                    @Param("userCd") String userCd,
                                                    @Param("leaveCds") java.util.List<String> leaveCds,
                                                    @Param("workYmd") String workYmd,
                                                    @Param("neededDays") BigDecimal neededDays);

    /**
     * prafta-com-016-C-4: 해당 셀(직원+일자)에 이미 종일(USE_UNIT_TYPE='00') CONFIRMED 연차가 있는지 카운트.
     * 자동 차감 멱등(중복 차감 방지)용 — 직접/승인 무관 전부 센다.
     */
    int countAnyFullDayLeaveOnCell(@Param("cmpnyCd") String cmpnyCd,
                                   @Param("userCd") String userCd,
                                   @Param("workYmd") String workYmd);

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

    /**
     * PRAFTA-041 - 직접 연차 사용(REQ_ID 없음, CONFIRMED)이 차감한 부여(GRANT_ID) 목록을 셀 단위로 조회한다.
     * 취소 후 부여 USED_DAYS 재계산 대상을 미리 확보하기 위함(취소하면 매칭이 사라져 못 찾으므로 선조회).
     * 동일 직원·일자(START_DATE)·연차코드의 직접 사용기록을 대상으로 하며, GRANT_ID 중복은 제거한다.
     */
    java.util.List<String> selectDirectLeaveGrantIdsByCell(@Param("cmpnyCd") String cmpnyCd,
                                                           @Param("userCd") String userCd,
                                                           @Param("workYmd") String workYmd,
                                                           @Param("leaveCd") String leaveCd);

    /**
     * PRAFTA-041 - 직접 연차 사용기록(REQ_ID 없음, CONFIRMED)을 셀 단위로 soft cancel 한다(차감 해제).
     * 동일 직원·일자(START_DATE)·연차코드의 미삭제 CONFIRMED 직접 사용기록을 CANCELLED 로 전이한다.
     * @return 취소된 행 수
     */
    int cancelDirectLeaveUseByCell(@Param("cmpnyCd") String cmpnyCd,
                                   @Param("userCd") String userCd,
                                   @Param("workYmd") String workYmd,
                                   @Param("leaveCd") String leaveCd,
                                   @Param("cancelReason") String cancelReason,
                                   @Param("updateNo") String updateNo);

    /** 부여의 USED_DAYS를 확정 사용 합계로 재계산(차감/해제 후 동기화). */
    int recomputeGrantUsedDays(@Param("cmpnyCd") String cmpnyCd,
                               @Param("grantId") String grantId,
                               @Param("updateNo") String updateNo);

    /**
     * prafta-com-011-2 가불: 대상 직원의 입사일(HIRE_DATE, YYYYMMDD). 활성 사용자만, 스코프 밖/미존재면 null.
     * 가불 한도 projection/만료 검증의 입력으로만 쓴다(식별값은 토큰 도출).
     */
    String selectUserHireDate(@Param("cmpnyCd") String cmpnyCd,
                              @Param("userCd") String userCd);

    /**
     * prafta-com-011-2 가불(Q1=b 잔여 우선 차감): 차감 가능한 활성 부여 목록(만료 임박순, 잔여>0, FOR UPDATE).
     *
     * <p>{@code selectDeductibleGrant} 는 단일 부여로 전량(neededDays) 충당 가능한 1건만 반환하므로, 잔여를
     *   여러 부여에 걸쳐 분할 차감하려면 본 목록이 필요하다. 술어는 selectDeductibleGrant 와 동일하되
     *   잔여 충분 조건 대신 잔여>0 만 요구한다(부분 차감 허용). 비가불 경로는 호출하지 않는다(회귀 0).
     */
    java.util.List<DeductibleGrantVO> selectBorrowDeductibleGrants(@Param("cmpnyCd") String cmpnyCd,
                                                                   @Param("userCd") String userCd,
                                                                   @Param("leaveCd") String leaveCd,
                                                                   @Param("workYmd") String workYmd);

    /** 요청에 연결된 사용 행의 GRANT_ID 조회(반려 시 부여 재계산 대상). 없으면 null. */
    String selectGrantIdByReqId(@Param("cmpnyCd") String cmpnyCd,
                                @Param("reqId") String reqId);

    /**
     * prafta-com-011-2 가불: 요청에 연결된 사용 행의 GRANT_ID 전체(중복 제거). 반려 시 분할 차감(가불+잔여)된
     *   여러 부여를 모두 재계산하기 위함. 비가불(단일 부여)이면 1건 반환 → 기존 동작과 동일.
     */
    java.util.List<String> selectGrantIdsByReqId(@Param("cmpnyCd") String cmpnyCd,
                                                 @Param("reqId") String reqId);

    /** 요청에 연결된 확정 사용의 핵심 정보(출근차단/finalize용). 없으면 null. */
    com.prafta.web.attd.leaveflow.vo.LeaveUseDetailVO selectLeaveUseDetailByReqId(
            @Param("cmpnyCd") String cmpnyCd, @Param("reqId") String reqId);

    // prafta-com-008-E (L1): upsertWorkPlanLeave / deleteWorkPlanLeave 제거.
    //   E-2 모델 전환으로 work_plan 에 LEAVE_CD 를 쓰지 않으므로(연차일 판정 단일출처=leave_use) 사장됨. 호출처 0건.

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
