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
     * 회사 활성 연차종류 + 종류별 현재 잔여(활성집합 SUM(GRANT_DAYS)-SUM(USED_DAYS)).
     * USE_YN='N' 종류는 제외. balanceDays 는 부여 없으면 0.
     */
    List<LeaveTypeMetaRow> selectApplicableLeaveTypes(@Param("cmpnyCd") String cmpnyCd,
                                                      @Param("userCd") String userCd);

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

    /** 요청 ID 채번(YYYYMMDD + 시퀀스). */
    String selectNextReqId(@Param("cmpnyCd") String cmpnyCd);

    /** 연차 사용기록 ID 채번(LV + YYYYMMDD + 시퀀스). */
    String selectNextLeaveId(@Param("cmpnyCd") String cmpnyCd);

    /** 연차 종류 메타(systemYn/aprvUseYn/useUnitType). 없으면 null. */
    LeaveTypeInfoRow selectLeaveTypeInfo(@Param("cmpnyCd") String cmpnyCd,
                                         @Param("leaveCd") String leaveCd);

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

    /** 요청 상태 갱신(즉시확정 시 '02'). */
    int updateReqStatus(@Param("cmpnyCd") String cmpnyCd,
                        @Param("reqId") String reqId,
                        @Param("reqStatus") String reqStatus,
                        @Param("processUserCd") String processUserCd,
                        @Param("processComment") String processComment);

    /** 출근 차단: 일 단위 휴가 확정 시 근무계획을 연차코드로 덮어 출근 차단(upsert). */
    int upsertWorkPlanLeave(@Param("cmpnyCd") String cmpnyCd,
                            @Param("siteCd") String siteCd,
                            @Param("userCd") String userCd,
                            @Param("workYmd") String workYmd,
                            @Param("leaveCd") String leaveCd,
                            @Param("insertNo") String insertNo);

    /**
     * 신청자의 자체근태승인 자격('Y'/null). 소속노드 SELF_ATTD_APPRV_YN='Y' 이면서
     * 본인이 그 노드 담당 정/부(MAIN/SUB_ADMIN_CD)일 때만 'Y'. 자격 미달이면 행 없음(NULL=fail-closed).
     */
    String selectUserNodeSelfApproveYn(@Param("cmpnyCd") String cmpnyCd,
                                       @Param("userCd") String userCd);
}
