package com.prafta.app.leave.leaveflow.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.leave.leaveflow.application.command.LeaveReqInsertCommand;
import com.prafta.app.leave.leaveflow.application.command.LeaveUseCommand;
import com.prafta.app.leave.leaveflow.result.ApproverRow;
import com.prafta.app.leave.leaveflow.result.DeductibleGrantRow;
import com.prafta.app.leave.leaveflow.result.LeaveTypeInfoRow;
import com.prafta.app.leave.leaveflow.result.LeaveTypeMetaRow;
import com.prafta.app.leave.leaveflow.result.LeaveUsagePolicyRow;

/**
 * prafta-app-018-A: 앱 연차 신청 폼 메타 조회 Mapper.
 *
 * <p>읽기 전용. 식별값(cmpny/site/user)은 서비스에서 토큰값만 전달한다(IDOR 차단).
 *   결재선 프리셋 조회(approval-presets)는 신규 SQL 을 두지 않고 mypage01 매퍼를 재사용하므로 본 매퍼에 없음.
 */
@Mapper
public interface AppLeaveFlowMapper {

    /**
     * 회사 활성 연차종류 + 종류별 현재 잔여. USE_YN='N' 종류는 제외.
     *
     * <p>연차개편: balanceDays 가 LEAVE_TYPE 별로 분기한다.
     *   '01'(사용자 신청)은 {@code IFNULL(MAX_APLY_DAYS,0) - Σ(당해 회계연도 CONFIRMED 사용)},
     *   그 외는 기존 활성 부여집합 {@code SUM(GRANT_DAYS)-SUM(USED_DAYS)}.
     *   회계연도 경계 {@code [fiscalStartYmd, fiscalEndYmdExclusive)} 는 호출부가 FiscalYearUtils 로 산출해 전달한다.
     */
    List<LeaveTypeMetaRow> selectApplicableLeaveTypes(@Param("cmpnyCd") String cmpnyCd,
                                                      @Param("userCd") String userCd,
                                                      @Param("fiscalStartYmd") String fiscalStartYmd,
                                                      @Param("fiscalEndYmdExclusive") String fiscalEndYmdExclusive);

    /**
     * 회사 활성 연차정책의 단일 허용단위(USAGE_UNIT) + 법정 결재여부(APRV_USE_YN).
     * 활성 법정정책이 없으면 null.
     */
    LeaveUsagePolicyRow selectCompanyUsageUnit(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 결재자 후보 검색(동일 회사/사업장 활성 사용자, 본인·system 제외, 이름 부분일치 선택).
     * LIMIT/OFFSET 강제(대량조회 방지). limit 은 hasNext 판정용 size+1 을 넘긴다.
     * PII 정렬보조(rankCd/sortIdx)는 SELECT 하지 않는다.
     */
    List<ApproverRow> searchApprovers(@Param("cmpnyCd") String cmpnyCd,
                                      @Param("siteCd") String siteCd,
                                      @Param("excludeUserCd") String excludeUserCd,
                                      @Param("keyword") String keyword,
                                      @Param("limit") int limit,
                                      @Param("offset") int offset);

    // ============================================================
    // prafta-app-018-B: 연차 신청 쓰기 (웹 LeaveFlowMapper SQL 미러, 네임스페이스만 app)
    // 웹 LeaveFlowMapper 를 직접 주입하지 않는다(앱/웹 패키지 분리). SQL 본문/컬럼/바인딩은 웹과 100% 동일.
    // ============================================================

    /**
     * prafta-app-018-B 보안(결재자 스코프 가드): 주어진 USER_CD 목록 중 "동일 회사 + 동일 사업장 +
     * 재직(USE_YN='Y', WITHDRAWAL_DATE IS NULL, ACCOUNT_STATUS='01') + system 아님" 조건을 만족하는
     * 사용자 수를 센다. 호출부는 중복 제거한 결재자 수와 비교하여 불일치(타 사업장/회사/비재직/존재없음)
     * 시 거부한다(cross-site/cross-company 결재자 주입 차단 — searchApprovers 화이트리스트와 동일 조건).
     */
    int countValidApprovers(@Param("cmpnyCd") String cmpnyCd,
                            @Param("siteCd") String siteCd,
                            @Param("userCds") List<String> userCds);

    /**
     * PRAFTA-APP-022 룰B: 해당 일자에 출근 기록(TB_USER_ATTD_MGMT)이 존재하는지 카운트.
     * 같은 (CMPNY_CD, SITE_CD, USER_CD, WORK_YMD) 의 DEL_YN='N' 행 수.
     * CHECK_IN_* 가 NOT NULL 이므로 행이 존재하면 곧 출근 기록 존재 → 결과 > 0 이면 연차 거부(ATTD_400_108).
     * 식별값은 서비스에서 토큰값만 전달한다(IDOR).
     */
    int countAttendanceByDate(@Param("cmpnyCd") String cmpnyCd,
                              @Param("siteCd") String siteCd,
                              @Param("userCd") String userCd,
                              @Param("workYmd") String workYmd);

    /**
     * 같은 날 이미 점유된 연차 일수(중복 등록 가드). 해당 일자(START_DATE~END_DATE 포함)의 CONFIRMED·미삭제
     * 연차 사용기록을 합산하되, 종일(USE_UNIT_TYPE='00')은 1.0, 그 외(반차/시간차)는 LEAVE_DAYS 로 본다.
     * 호출부는 (점유 + 신규 신청일수) &gt; 1.0 이면 거부(ATTD_400_111)한다. 식별값은 토큰 도출값(IDOR).
     */
    BigDecimal selectOccupiedLeaveDaysOnDate(@Param("cmpnyCd") String cmpnyCd,
                                             @Param("userCd") String userCd,
                                             @Param("workYmd") String workYmd);

    /**
     * 같은 날 시간대가 겹치는 시간차 연차(USE_UNIT_TYPE in '02','03','04') 개수. 신규 [startTime,endTime) 와
     * 기존 [START_TIME,END_TIME) 가 겹치면(기존시작 &lt; 신규종료 AND 기존종료 &gt; 신규시작) 카운트.
     * 결과 &gt; 0 이면 거부(ATTD_400_112). 시간차 단위 신청 시에만 호출(HHMM 문자열, 좌폐우개 비교).
     */
    int countOverlappingTimeLeaveOnDate(@Param("cmpnyCd") String cmpnyCd,
                                        @Param("userCd") String userCd,
                                        @Param("workYmd") String workYmd,
                                        @Param("startTime") String startTime,
                                        @Param("endTime") String endTime);

    /** 요청 ID 채번(YYYYMMDD + 시퀀스). */
    String selectNextReqId(@Param("cmpnyCd") String cmpnyCd);

    /** 연차 사용기록 ID 채번(LV + YYYYMMDD + 시퀀스). */
    String selectNextLeaveId(@Param("cmpnyCd") String cmpnyCd);

    /** 연차 종류 메타(systemYn/aprvUseYn/useUnitType/leaveType/maxAplyDays). 없으면 null. */
    LeaveTypeInfoRow selectLeaveTypeInfo(@Param("cmpnyCd") String cmpnyCd,
                                         @Param("leaveCd") String leaveCd);

    /**
     * 연차개편(사용자 신청 '01' 한도검증): 당해 회계연도 CONFIRMED 사용 합계(Σ LEAVE_DAYS).
     * 술어 LEAVE_STATUS='CONFIRMED' AND DEL_YN='N' (반려/취소 제외). 합계 없으면 0.
     */
    BigDecimal selectFiscalUsedDays(@Param("cmpnyCd") String cmpnyCd,
                                    @Param("userCd") String userCd,
                                    @Param("leaveCd") String leaveCd,
                                    @Param("fiscalStartYmd") String fiscalStartYmd,
                                    @Param("fiscalEndYmdExclusive") String fiscalEndYmdExclusive);

    /**
     * prafta-com-016-B(3-1): 사용자 신청 '01' + 사용가능기간 '01'(설정안함=전체 누적) 한도검증용.
     * 회계연도 경계 없이 전체 CONFIRMED 사용 합계(Σ LEAVE_DAYS). 술어는 selectFiscalUsedDays 와 동일. 합계 없으면 0. (웹 미러)
     */
    BigDecimal selectTotalUsedDays(@Param("cmpnyCd") String cmpnyCd,
                                   @Param("userCd") String userCd,
                                   @Param("leaveCd") String leaveCd);

    /**
     * 연차개편 동시성: 사용자 신청('01') 직렬화용 advisory lock 획득(GET_LOCK).
     * '01'은 차감 GRANT 가 없어 FOR UPDATE 를 못 쓰므로 (USER_CD,LEAVE_CD) 키로 세션 단위 직렬화한다.
     * 1=획득, 0=타임아웃, null=오류. 트랜잭션 무관(세션 단위) → 호출부가 트랜잭션 완료
     * (afterCompletion) 시점에 releaseAdvisoryLock (등록 불가 시 finally 폴백 — 보안리뷰 Medium).
     */
    Integer getAdvisoryLock(@Param("lockKey") String lockKey, @Param("timeoutSec") int timeoutSec);

    /** 연차개편 동시성: advisory lock 해제(RELEASE_LOCK). */
    Integer releaseAdvisoryLock(@Param("lockKey") String lockKey);

    /** 법정 연차정책 결재여부(APRV_USE_YN). 활성정책 없으면 null. */
    String selectPolicyAprvUseYn(@Param("cmpnyCd") String cmpnyCd);

    /**
     * 차감 대상 부여: 활성(STATUS='ACTIVE'+DEL_YN='N') + 유효기간 포함 + 잔여 충분.
     * 만료 임박(AVAIL_TO_DATE ASC) 우선 소진, FOR UPDATE 직렬화. 없으면 null.
     */
    DeductibleGrantRow selectDeductibleGrant(@Param("cmpnyCd") String cmpnyCd,
                                             @Param("userCd") String userCd,
                                             @Param("leaveCd") String leaveCd,
                                             @Param("workYmd") String workYmd,
                                             @Param("neededDays") BigDecimal neededDays);

    /** 요청 INSERT(REQ_TYPE='05' 고정). */
    int insertLeaveReq(LeaveReqInsertCommand command);

    /** 연차 사용기록 INSERT(LEAVE_STATUS='CONFIRMED'). */
    int insertLeaveUse(LeaveUseCommand command);

    /** 부여 USED_DAYS = 해당 부여 CONFIRMED 사용 합계로 재동기화. */
    int recomputeGrantUsedDays(@Param("cmpnyCd") String cmpnyCd,
                               @Param("grantId") String grantId,
                               @Param("updateNo") String updateNo);

    /**
     * prafta-com-011-2 가불: 대상 직원의 입사일(HIRE_DATE, YYYYMMDD). 활성 사용자만, 스코프 밖/미존재면 null.
     * 웹 {@code LeaveFlowMapper.selectUserHireDate} 미러. 가불 projection/만료검증 입력(식별값 토큰 도출).
     */
    String selectUserHireDate(@Param("cmpnyCd") String cmpnyCd,
                              @Param("userCd") String userCd);

    /**
     * prafta-com-011-2 가불(Q1=b 잔여 우선 차감): 차감 가능한 활성 부여 목록(만료 임박순, 잔여>0, FOR UPDATE).
     * 웹 {@code LeaveFlowMapper.selectBorrowDeductibleGrants} 미러(SQL 본문 동일). 비가불 경로는 호출하지 않는다.
     */
    List<DeductibleGrantRow> selectBorrowDeductibleGrants(@Param("cmpnyCd") String cmpnyCd,
                                                          @Param("userCd") String userCd,
                                                          @Param("leaveCd") String leaveCd,
                                                          @Param("workYmd") String workYmd);

    /** 요청 상태 갱신(즉시확정 시 '02'). */
    int updateReqStatus(@Param("cmpnyCd") String cmpnyCd,
                        @Param("reqId") String reqId,
                        @Param("reqStatus") String reqStatus,
                        @Param("processUserCd") String processUserCd,
                        @Param("processComment") String processComment);

    // prafta-com-008-E (L1): upsertWorkPlanLeave 제거.
    //   E-2 모델 전환으로 work_plan 에 LEAVE_CD 를 쓰지 않으므로(연차일 판정 단일출처=leave_use) 사장됨. 호출처 0건.

    /**
     * 신청자의 자체근태승인 자격('Y'/null). 소속노드 SELF_ATTD_APPRV_YN='Y' 이면서
     * 본인이 그 노드 담당 정/부(MAIN/SUB_ADMIN_CD)일 때만 'Y'. 자격 미달이면 행 없음(NULL=fail-closed).
     */
    String selectUserNodeSelfApproveYn(@Param("cmpnyCd") String cmpnyCd,
                                       @Param("userCd") String userCd);
}
