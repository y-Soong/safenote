package com.prafta.web.attd.attd07.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.attd07.application.command.DailyAttdDetailDeleteCommand;
import com.prafta.web.attd.attd07.application.command.InsertUserAttdHistsCommand;
import com.prafta.web.attd.attd07.application.command.InsertUserOvertimeCommand;
import com.prafta.web.attd.attd07.application.command.RejectUserAttdRequestCommand;
import com.prafta.web.attd.attd07.application.command.UpdateUserAttdInfosCommand;
import com.prafta.web.attd.attd07.application.command.UpdateUserAttdRequestCommand;
import com.prafta.web.attd.attd07.application.query.DailyAttdDetailsQuery;
import com.prafta.web.attd.attd07.application.query.MonthlyAttdListQuery;
import com.prafta.web.attd.attd07.application.query.OvertimeAllowedWindowQuery;
import com.prafta.web.attd.attd07.result.AllowedWindowResult;
import com.prafta.web.attd.attd07.result.DailyAttdDetailHistoryResult;
import com.prafta.web.attd.attd07.result.DailyAttdDetailsResult;
import com.prafta.web.attd.attd07.result.DailyOvertimeResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdListResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqSummaryResult;
import com.prafta.web.attd.attd07.result.UserAttdReqResult;

@Mapper
public interface Attd07Mapper {

    List<MonthlyAttdListResult> selectMonthlyAttdList(MonthlyAttdListQuery query);

    List<MonthlyAttdReqSummaryResult> selectMonthlyAttdReqSummary(MonthlyAttdListQuery query);

    String selectAttdId(@Param("gvCmpnyCd")	String gvCmpnyCd);

    String selectHistId(@Param("gvCmpnyCd") String gvCmpnyCd);

    void updateUserAttdInfos(UpdateUserAttdInfosCommand command);

    void insertUserAttdInfos(InsertUserAttdHistsCommand command);

    DailyAttdDetailsResult selectDailyAttdDetails(DailyAttdDetailsQuery query);

    List<DailyAttdDetailHistoryResult> selectDailyAttdDetailHistory(DailyAttdDetailsQuery query);

    List<MonthlyAttdReqResult> selectMonthlyAttdReq(DailyAttdDetailsQuery query);

    /**
     * PRAFTA-003-6: 일자 상세 조회 시 함께 노출되는 OT(초과근무) 리스트.
     * (CMPNY_CD, SITE_CD, USER_CD, WORK_YMD, DEL_YN='N', OT_STATUS != 'CANCELLED')
     * 조건으로 TB_USER_OVERTIME_MGMT 를 조회한다.
     */
    List<DailyOvertimeResult> selectDailyOvertimeList(DailyAttdDetailsQuery query);

    void dailyAttdDetailDelete(DailyAttdDetailDeleteCommand command);

    void insertDailyAttdDetailDeleteHist(DailyAttdDetailDeleteCommand command);

    /**
     * Server-authoritative load of an attendance request row.
     * Used to validate that the body fields match the stored request and to enforce
     * self-approval / status-state policies before issuing the approve UPDATE.
     */
    UserAttdReqResult selectUserAttdReqByReqId(
            @Param("reqId") String reqId,
            @Param("gvCmpnyCd") String gvCmpnyCd);

    /**
     * Returns the existing ATTD_ID for the (cmpny, site, user, ymd, seq) tuple
     * so the approve flow re-uses the existing MGMT row instead of inserting a
     * new ATTD_ID into TB_USER_ATTD_REQ.
     */
    String selectExistingAttdId(
            @Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("siteCd") String siteCd,
            @Param("userCd") String userCd,
            @Param("workYmd") String workYmd,
            @Param("workSeq") String workSeq);

    int updateUserAttdReqApprove(UpdateUserAttdRequestCommand command);

    /**
     * PRAFTA-008 / PRAFTA-010 - 근태 / 초과근무 요청 반려.
     *
     * TB_USER_ATTD_REQ 를 REQ_STATUS='REJECTED' 로 전이하고 처리자 / 반려사유 /
     * 처리일시를 기록한다. WHERE 절에 REQ_STATUS='REQUESTED' 가드를 두어 정확히
     * 1행만 영향을 받게 하며, 0행이면 호출 측에서 동시 처리 충돌로 보고 롤백한다.
     *
     * @return 영향받은 행 수 (0 또는 1)
     */
    int updateUserAttdReqReject(RejectUserAttdRequestCommand command);

    /**
     * Loads the work plan / schedule / standardized actual time intervals
     * required to compute the OT-allowed window. Returns null if the worker
     * has no work plan for the given day.
     */
    AllowedWindowResult selectAllowedWindow(OvertimeAllowedWindowQuery query);

    /**
     * Issues a new OT_ID from the company-scoped sequence (FNC_CMM_SEQ_NEXTVAL).
     */
    String selectOtId(@Param("gvCmpnyCd") String gvCmpnyCd);

    /**
     * Inserts a single TB_USER_OVERTIME_MGMT row.
     */
    int insertUserOvertime(InsertUserOvertimeCommand command);

    /**
     * SEC-017 - returns the count of TB_USER rows matching
     * (cmpnyCd, siteCd, userCd) that are usable (USE_YN='Y', not withdrawn).
     * Used to confirm the target user is in the caller's company/site scope
     * before any OT row is inserted on their behalf.
     */
    int selectUserExistInCmpnySite(
            @Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd,
            @Param("userCd") String userCd);

    /**
     * SEC-017 - returns the count of TB_USER_ATTD_MGMT rows matching
     * (cmpnyCd, siteCd, userCd, attdId) that are not deleted. Used to confirm
     * the supplied ATTD_ID actually belongs to the target user inside the
     * caller's scope before linking it to the OT row.
     */
    int selectAttdExistInScope(
            @Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd,
            @Param("userCd") String userCd,
            @Param("attdId") String attdId);

    /**
     * PRAFTA-009-001 - 초과근무 중복 INSERT 방지.
     *
     * 주어진 (cmpnyCd, siteCd, userCd, attdId) 의 기존 활성 OT 행 중
     * 요청 OT 구간([reqStart, reqEnd))과 시간이 겹치는 행 수를 반환한다.
     *
     * - 활성 행 기준: DEL_YN='N' AND OT_STATUS != 'CANCELLED' (취소/삭제 행은 재등록을 막지 않음).
     * - 시각 비교: ACTUAL_START_DATE(yyyyMMdd) + ACTUAL_START_TIME(HHmm) 을 CONCAT 한
     *   12자리 문자열로 비교한다.
     * - 겹침 판정: 표준식 (기존시작 &lt; 요청종료 AND 요청시작 &lt; 기존종료).
     * - ACTUAL_END_DATE / ACTUAL_END_TIME 이 NULL 인 행은 종료 시각이 확정되지 않아
     *   유한 구간 겹침 판정이 불가능하므로 비교 대상에서 제외한다.
     *
     * @param reqStart 요청 OT 시작 시각 (yyyyMMddHHmm, 12자리)
     * @param reqEnd   요청 OT 종료 시각 (yyyyMMddHHmm, 12자리)
     */
    int selectOverlappingOvertimeCount(
            @Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd,
            @Param("userCd") String userCd,
            @Param("attdId") String attdId,
            @Param("reqStart") String reqStart,
            @Param("reqEnd") String reqEnd);
}
