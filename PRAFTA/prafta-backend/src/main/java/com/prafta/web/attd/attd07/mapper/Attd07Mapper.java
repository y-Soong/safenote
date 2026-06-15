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
import com.prafta.web.attd.attd07.application.command.UpsertUserWorkPlanCommand;
import com.prafta.web.attd.attd07.application.query.DailyAttdDetailsQuery;
import com.prafta.web.attd.attd07.application.query.MonthlyAttdListQuery;
import com.prafta.web.attd.attd07.application.query.OvertimeAllowedWindowQuery;
import com.prafta.web.attd.attd07.result.AllowedWindowResult;
import com.prafta.web.attd.attd07.result.AttdSnapshotResult;
import com.prafta.web.attd.attd07.result.ConfirmedLeaveResult;
import com.prafta.web.attd.attd07.result.DailyAttdDetailHistoryResult;
import com.prafta.web.attd.attd07.result.DailyAttdDetailsResult;
import com.prafta.web.attd.attd07.result.DailyOvertimeResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdListResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqResult;
import com.prafta.web.attd.attd07.result.MonthlyAttdReqSummaryResult;
import com.prafta.web.attd.attd07.result.MonthlyOvertimeResult;
import com.prafta.web.attd.attd07.result.UserAttdReqResult;

@Mapper
public interface Attd07Mapper {

    List<MonthlyAttdListResult> selectMonthlyAttdList(MonthlyAttdListQuery query);

    List<MonthlyAttdReqSummaryResult> selectMonthlyAttdReqSummary(MonthlyAttdListQuery query);

    /**
     * PRAFTA-017 - Attd_07 목록뷰에서 함께 노출할 일자별 초과근무 목록(월 단위).
     * selectMonthlyAttdReqSummary 와 동일한 부서 트리 스코프(node_tree)로
     * TB_USER_OVERTIME_MGMT 를 조회한다.
     * (CMPNY_CD, SITE_CD, WORK_YMD LIKE 'YYYYMM%', DEL_YN='N',
     *  OT_STATUS != 'CANCELLED') 조건.
     */
    List<MonthlyOvertimeResult> selectMonthlyOvertimeList(MonthlyAttdListQuery query);

    /**
     * PRAFTA-COM-008-B-3: 그날 종일(USE_UNIT_TYPE='00') 확정 연차 존재 카운트(웹 OT 등록 차단 판정).
     *
     * <p>app 측 단일출처 {@code AppAttd01Mapper.countFullDayLeaveOn}(=AppHome01Mapper 미러) 와 동일 술어를
     * 웹 영역에서 재사용하기 위한 미러다(app↔web 매퍼 직접 의존 회피). 부분연차(반차/시간차)는 근무일을
     * 유지하므로 미카운트. &gt; 0 이면 종일 연차일 → 초과근무 신청 차단(ATTD_400_151). 촉진/자발 무관 일괄 차단.
     */
    int countFullDayLeaveOn(@Param("cmpnyCd") String cmpnyCd,
                            @Param("siteCd") String siteCd,
                            @Param("userCd") String userCd,
                            @Param("workYmd") String workYmd);

    String selectAttdId(@Param("gvCmpnyCd")	String gvCmpnyCd);

    String selectHistId(@Param("gvCmpnyCd") String gvCmpnyCd);

    void updateUserAttdInfos(UpdateUserAttdInfosCommand command);

    void insertUserAttdInfos(InsertUserAttdHistsCommand command);

    DailyAttdDetailsResult selectDailyAttdDetails(DailyAttdDetailsQuery query);

    List<DailyAttdDetailHistoryResult> selectDailyAttdDetailHistory(DailyAttdDetailsQuery query);

    /** PRAFTA: 연차(05/06) 승인/반려 처리 이력 — 결재라인 기준. 일자 상세 '처리 이력'에 병합 노출. */
    List<DailyAttdDetailHistoryResult> selectDailyLeaveApprovalHistory(DailyAttdDetailsQuery query);

    /**
     * PRAFTA-APP-007-WEB-6 + D15 - 스케줄 수정(REQ_TYPE='10') 처리된 요청(승인 '02'/반려 '03')을
     * 일자 상세 '처리 이력'에 노출. TB_USER_ATTD_REQ 직접 조회(결재라인 미사용 — 매니저 모델).
     * 승인 행은 PROCESS_COMMENT 마커에서 변경 전 스케줄 코드를 추출해 변경 전/후 원시 시각을 함께 내린다.
     * (승인 마커는 사용자 노출 금지 → processReason NULL.)
     */
    List<DailyAttdDetailHistoryResult> selectDailySchedModifyHistory(DailyAttdDetailsQuery query);

    /**
     * PRAFTA-APP-007 D15 - 스케줄 수정 승인 직전 현재 근무계획 코드(WORK_PLAN_CD) 조회.
     * upsert 로 덮어쓰기 전에 "변경 전 스케줄"을 캡처해 PROCESS_COMMENT 마커에 직렬화하기 위함.
     * 해당 일자에 근무계획이 없으면 null(변경 전 없음).
     */
    String selectUserWorkPlanCd(
            @Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("siteCd") String siteCd,
            @Param("userCd") String userCd,
            @Param("workYmd") String workYmd);

    List<MonthlyAttdReqResult> selectMonthlyAttdReq(DailyAttdDetailsQuery query);

    /**
     * PRAFTA-APP-018-F - 그날(workYmd) 확정 연차 사용내역(TB_USER_LEAVE_USE, CONFIRMED).
     *   결재 유무 무관. 단, 미처리(01) 결재대기분은 D 의 '근로자 요청' 카드가 소유하므로 제외(이중표시 방지).
     *   자동확정(02)/직접 적용(REQ_ID NULL)/직접 사용분만 표시 전용 섹션에 내린다.
     *   스코프는 일자상세와 동일(CMPNY/SITE/USER), 진입부 2단 권한 가드 승계.
     */
    List<ConfirmedLeaveResult> selectDailyConfirmedLeave(DailyAttdDetailsQuery query);

    /**
     * PRAFTA-003-6: 일자 상세 조회 시 함께 노출되는 OT(초과근무) 리스트.
     * (CMPNY_CD, SITE_CD, USER_CD, WORK_YMD, DEL_YN='N', OT_STATUS != 'CANCELLED')
     * 조건으로 TB_USER_OVERTIME_MGMT 를 조회한다.
     */
    List<DailyOvertimeResult> selectDailyOvertimeList(DailyAttdDetailsQuery query);

    /** ATTD_ID 의 근무일자(WORK_YMD) 조회 — 마감 가드용 (PRAFTA-028). */
    String selectAttdWorkYmd(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("attdId") String attdId);

    /**
     * 승인 처리 직전 근태(MGMT) 현재 출퇴근 스냅샷 조회 — 처리 이력 "변경 전(BEF_*)" 을
     * 서버 권위 데이터로 채우기 위한 용도(감사 무결성). 기존 행이 없으면(생성요청) null.
     */
    AttdSnapshotResult selectAttdSnapshotById(
            @Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("attdId") String attdId);

    void dailyAttdDetailDelete(DailyAttdDetailDeleteCommand command);

    void insertDailyAttdDetailDeleteHist(DailyAttdDetailDeleteCommand command);

    /**
     * PRAFTA-016 - 근태 전체삭제 시 OT 연쇄 soft-delete.
     *
     * 삭제 대상 ATTD_ID 와 일치하고 아직 활성(DEL_YN='N')이며 ATTD_ID 가
     * NULL 이 아닌 TB_USER_OVERTIME_MGMT 행을 DEL_YN='Y' 로 전이한다.
     * REQ 상태(OT_STATUS / REQ)는 변경하지 않는다.
     * dailyAttdDetailDelete 와 동일 트랜잭션에서 호출된다.
     *
     * @return 영향받은 행 수
     */
    int deleteOvertimeByAttdId(DailyAttdDetailDeleteCommand command);

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

    /**
     * PRAFTA-027 - 초과근무 처리 이력(TB_USER_ATTD_HIST) 앵커용 ATTD_ID 조회.
     *
     * TB_USER_ATTD_HIST.ATTD_ID 는 NOT NULL 이고 OT 요청에는 WORK_SEQ 가 없을 수 있어,
     * (cmpny, site, user, ymd) 의 활성 근태기록 중 WORK_SEQ 가 가장 빠른 1건의 ATTD_ID 를
     * 반환한다. 초과근무는 그날 출퇴근 기록이 있어야만 등록되므로 정상 흐름에서 항상 존재한다.
     */
    String selectAttdIdByDay(
            @Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("siteCd") String siteCd,
            @Param("userCd") String userCd,
            @Param("workYmd") String workYmd);

    int updateUserAttdReqApprove(UpdateUserAttdRequestCommand command);

    /**
     * PRAFTA-APP-007 - 스케줄 수정 요청(REQ_TYPE='10') 승인 시 tb_user_work_plan 의
     * 단일 행(CMPNY_CD, SITE_CD, USER_CD, WORK_YMD) 의 WORK_PLAN_CD 를 목표 스케줄
     * 코드로 upsert 한다. Attd_05 의 saveUserWorkPlans 와 동형
     * (INSERT ... ON DUPLICATE KEY UPDATE). 모든 값은 서버 권위 값으로 구성한다.
     *
     * @return 영향받은 행 수 (INSERT=1, UPDATE 시 MySQL 은 값 변경 시 2 / 무변경 시 0~1)
     */
    int upsertUserWorkPlan(UpsertUserWorkPlanCommand command);

    /**
     * PRAFTA-APP-007 - 스케줄 수정 요청(REQ_TYPE='10') 승인. TB_USER_ATTD_REQ 를
     * REQ_STATUS='02'(승인) 로 전이하고 처리자/처리일시를 기록한다. TARGET_ID 는
     * 스케줄 수정에 무의미하므로 건드리지 않는다. WHERE 절에 REQ_STATUS='01'(신청)
     * 가드를 두어 정확히 1행만 영향을 받게 하며, 0행이면 동시 처리 충돌로 보고 롤백한다.
     *
     * @return 영향받은 행 수 (0 또는 1)
     */
    int updateUserSchedModifyReqApprove(
            @Param("reqId") String reqId,
            @Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("siteCd") String siteCd,
            @Param("gvUserCd") String gvUserCd,
            @Param("processComment") String processComment);

    /**
     * PRAFTA-008 / PRAFTA-010 - 근태 / 초과근무 요청 반려.
     *
     * TB_USER_ATTD_REQ 를 REQ_STATUS='03'(반려) 로 전이하고 처리자 / 반려사유 /
     * 처리일시를 기록한다. WHERE 절에 REQ_STATUS='01'(신청) 가드를 두어 정확히
     * 1행만 영향을 받게 하며, 0행이면 호출 측에서 동시 처리 충돌로 보고 롤백한다.
     *
     * @return 영향받은 행 수 (0 또는 1)
     */
    int updateUserAttdReqReject(RejectUserAttdRequestCommand command);

    /**
     * 초과근무 요청 승인 — 요청 승인 관리(Attd_10) 인박스에서 OT를 등록하면서
     * 연결된 요청(reqId)을 승인('02')으로 전이한다. Attd_07 직접 등록(reqId=null)은
     * 이 메서드를 호출하지 않으므로 영향이 없다. WHERE 절에 REQ_STATUS='01'(신청)
     * 가드를 두어 정확히 1행만 영향을 받게 하며, 0행이면 동시 처리 충돌로 보고 롤백한다.
     *
     * @return 영향받은 행 수 (0 또는 1)
     */
    int updateUserOvertimeReqApprove(
            @Param("reqId") String reqId,
            @Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("siteCd") String siteCd,
            @Param("gvUserCd") String gvUserCd);

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

    /**
     * PRAFTA-025 - 초과근무 수정('04') 승인: 기존 OT 행(OT_ID=otId)을 요청 구간으로 UPDATE한다.
     *
     * 완료된 OT 확정 모델에 맞춰 PLAN 및 ACTUAL 시각을 동일 값으로 갱신하고
     * WORK_MINUTES 를 반영한다. BREAK_MINUTES, OT_STATUS 는 변경하지 않는다.
     * prafta-043: 초과근무 유형(OT_TYPE) 파기로 otType 파라미터 제거.
     * 스코프(cmpnyCd/siteCd/userCd) 일치 + 활성 행(DEL_YN N, OT_STATUS 가 CANCELLED 아님)만 대상이며,
     * 0행이면 스코프 밖이거나 취소/삭제된 OT 로 보고 호출부에서 롤백한다.
     *
     * @return 영향받은 행 수 (0 또는 1)
     */
    int updateUserOvertimeModify(
            @Param("otId") String otId,
            @Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd,
            @Param("userCd") String userCd,
            @Param("startDate") String startDate,
            @Param("startTime") String startTime,
            @Param("endDate") String endDate,
            @Param("endTime") String endTime,
            @Param("workMinutes") int workMinutes,
            @Param("updateNo") String updateNo);
}
