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
import com.prafta.web.attd.attd07.result.AttdKeyFieldsResult;
import com.prafta.web.attd.attd07.result.AttdSnapshotResult;
import com.prafta.web.attd.attd07.result.ConfirmedLeaveResult;
import com.prafta.web.attd.attd07.result.LeaveExemptWindowResult;
import com.prafta.web.attd.attd07.result.DayAttdSegmentResult;
import com.prafta.web.attd.attd07.result.DailyAttdDetailHistoryResult;
import com.prafta.web.attd.attd07.result.DailyAttdDetailsResult;
import com.prafta.web.attd.attd07.result.DailyLeaveChangeReqResult;
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
     * A안(2026-08-17): 조회 월 안의 확정 "시각 보유" 연차(반차 01 + 시간차 02/03/04) 구간 목록.
     * 목록 뷰 실근로/인정시간 표시에서 실근태와의 겹침 차감용(연차 시간은 근로시간 미산입).
     */
    List<com.prafta.web.attd.attd07.result.MonthlyTimeLeaveWindowResult> selectMonthlyTimeLeaveWindows(MonthlyAttdListQuery query);

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
     * 그날(workYmd) 걸려 있는 연차 변경(이동/삭제) 활성 요청 — TB_LEAVE_CHANGE_REQUEST.
     *
     * <p>근태 요청(TB_USER_ATTD_REQ)과 별개 테이블이라 종전엔 일자 상세 팝업에서 보이지도 처리되지도
     * 않았다(Attd_13 / Attd_10 전용). 활성 상태(REQUESTED:근로자 응답대기 / AGREED:관리자 확인대기) 중
     * <b>출발일(연차 사용일) 또는 이동 대상일</b>이 조회 일자와 일치하는 건을 내려, 근태 요청과 동일하게
     * 양쪽 셀에서 같은 요청이 보이게 한다.
     *
     * <p>스코프는 일자상세와 동일(CMPNY/SITE/대상 USER). 진입부 2단 권한 가드
     * (canManageNode + selectUserExistInCmpnySite) 통과 후 호출되므로 추가 권한 코드 불필요
     * (selectDailyConfirmedLeave 와 동일 근거).
     */
    List<DailyLeaveChangeReqResult> selectDailyLeaveChangeReq(DailyAttdDetailsQuery query);

    /**
     * 월간 연차 변경(이동/삭제) 활성 요청 요약 — 캘린더 셀 강조용.
     *
     * <p>{@link #selectMonthlyAttdReqSummary} 와 동일한 부서 트리(node_tree) 스코프를 쓰되,
     * TB_LEAVE_CHANGE_REQUEST 에는 NODE_CD 가 없으므로 대상자(TARGET_USER_CD)의 TB_USER.NODE_CD 로
     * 스코프를 판정한다. MOVE 는 출발일·이동대상일 <b>두 행</b>(둘 다 조회 월에 속할 때), DELETE 는
     * 출발일 1행을 내린다 — 근태 요청처럼 관련된 모든 셀이 강조되게 하기 위함.
     *
     * <p>결과 타입은 (식별자, 일자, 사용자) 3컬럼이라 {@link MonthlyAttdReqSummaryResult} 를 재사용한다
     * (reqId 자리에 CHANGE_REQ_ID). 근태 요청 목록과는 응답에서 별도 리스트로 분리해 카운트 중복을 막는다.
     */
    List<MonthlyAttdReqSummaryResult> selectMonthlyLeaveChangeReqSummary(MonthlyAttdListQuery query);

    /**
     * PRAFTA-003-6: 일자 상세 조회 시 함께 노출되는 OT(초과근무) 리스트.
     * (CMPNY_CD, SITE_CD, USER_CD, WORK_YMD, DEL_YN='N', OT_STATUS != 'CANCELLED')
     * 조건으로 TB_USER_OVERTIME_MGMT 를 조회한다.
     */
    List<DailyOvertimeResult> selectDailyOvertimeList(DailyAttdDetailsQuery query);

    /** ATTD_ID 의 근무일자(WORK_YMD) 조회 — 마감 가드용 (PRAFTA-028). */
    String selectAttdWorkYmd(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("attdId") String attdId);

    /**
     * 근무 구간 시각 겹침 판정(정책서 attd §7.6)용 — (회사/사업장/사용자) 활성 근태행 중
     * 근무일 윈도우 [fromYmd, toYmd] 에 속하고 편집/등록 대상(excludeAttdId)이 아닌 구간들의 출퇴근 시각을 반환한다.
     *
     * <p>웹 보정승인(updateUserAttdRequest)/직접수정(updateUserAttdInfos)에서 편집 대상 외 구간과의
     *   시각 겹침을 검사하기 위한 소스다. DEL_YN='N' 만, 퇴근 시각이 NULL 이면 open 구간으로 본다.
     *   excludeAttdId 가 null/빈값이면 제외 없이 윈도우 내 전체 구간을 반환한다(신규 생성 경로).
     *
     * <p><b>QT-2-6 수정</b>: 종전에는 근무일 동치(WORK_YMD = workYmd)로만 조회해, 오버나이트 근태
     *   (WORK_YMD=D, 퇴근 D+1)와 이웃 근무일(D±1) 근태의 실제 시각이 겹치는 경우를 검사 대상에서
     *   통째로 누락했다(겹치는 근태 2건이 원장에 공존 → 근무시간 이중 산입). 호출자가 근무일 ±1 을
     *   넘기고, 모든 구간을 대상 근무일 기준 분 stamp 로 환산해 비교한다.
     */
    List<DayAttdSegmentResult> selectAttdSegmentsAroundDayExcept(
            @Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd,
            @Param("userCd") String userCd,
            @Param("fromYmd") String fromYmd,
            @Param("toYmd") String toYmd,
            @Param("excludeAttdId") String excludeAttdId);

    /**
     * 승인 처리 직전 근태(MGMT) 현재 출퇴근 스냅샷 조회 — 처리 이력 "변경 전(BEF_*)" 을
     * 서버 권위 데이터로 채우기 위한 용도(감사 무결성). 기존 행이 없으면(생성요청) null.
     */
    AttdSnapshotResult selectAttdSnapshotById(
            @Param("gvCmpnyCd") String gvCmpnyCd,
            @Param("attdId") String attdId);

    /**
     * 관리자 직접 "수정"(attdId 보유) 기존 행 대조 게이트용 — (cmpnyCd, attdId) 활성(DEL_YN='N')
     * 근태 행의 키 필드(WORK_YMD/WORK_SEQ/SITE_CD/USER_CD/NODE_CD)만 조회한다.
     *
     * <p>updateUserAttdInfos 매퍼는 upsert 라 편집 시 위 5개 컬럼을 갱신하지 않는다(불변 전제).
     * 본문 값이 기존 행과 불일치하면 조용한 병합·마감 우회가 생기므로, statement 실행 전에 본 조회
     * 결과와 대조한다. 행이 없으면(부재/삭제) null — 수정 대상 부재로 거부한다(신규 INSERT 유입 차단).
     */
    AttdKeyFieldsResult selectAttdKeyFieldsById(
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
     * A안(2026-08-17): 해당 구간(WORK_SEQ)의 활성 근태 행 ATTD_ID 조회.
     * OT 승인 시 REQ 권위값의 WORK_SEQ 로 소속 근태를 결정적으로 연결한다. 없으면 null.
     */
    String selectActiveAttdIdBySlot(
            @Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd,
            @Param("userCd") String userCd,
            @Param("workYmd") String workYmd,
            @Param("workSeq") String workSeq);

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
     * 근무계획(TB_USER_WORK_PLAN) 행이 없는 날(주말 등 미배정일) 폴백 윈도우.
     *
     * <p>{@link #selectAllowedWindow}는 TB_USER_WORK_PLAN 을 anchor 로 두므로 WP 행이 없으면
     * 0행(=null)이 되어 실근태(A1/A2)까지 함께 유실된다. 정책 §7.5(스케줄 없는 날 근무는
     * 전량 초과근무 대상)를 충족하려면 스케줄을 비우고 실근태만 로드해야 한다.
     *
     * <p>본 쿼리는 dummy anchor 를 사용해 WP 없이도 항상 정확히 1행을 반환한다.
     * plan1/plan2 시각은 전부 NULL(스케줄 비움)이고 act1/act2 는 TB_USER_ATTD_MGMT 의
     * WORK_SEQ=1/2 행을 LEFT JOIN 으로 로드한다. 하위 구간 차집합 로직은 schSeg=null 을
     * 자동으로 "전량 허용"으로 처리한다.
     *
     * <p>파라미터는 {@link #selectAllowedWindow} 와 동일하게 {@link OvertimeAllowedWindowQuery}
     * 를 재사용한다.
     */
    AllowedWindowResult selectActualWindowNoSchedule(OvertimeAllowedWindowQuery query);

    /**
     * HB-08(D5): 그날 확정된 <b>시각 보유</b> 연차(반차 '01' + 시간차 '02'~'04')의 면제 구간 목록.
     *
     * <p>초과근무 등록 가능 범위 = {@code 실근태 - (스케줄 구간 ∪ 연차 면제 구간)}.
     * 종전에는 {@code 실근태 - 스케줄}이라, 종료기준 반차 후 재출근한 2구간처럼 스케줄이 아예 없는
     * 구간이 전량 OT 로 인정되어 수당이 과다 지급될 수 있었다(D5).
     *
     * <p>시각이 없는 구 반차는 {@code START_TIME IS NOT NULL} 로 자연 제외되어 종전 동작을 유지한다.
     * 종일('00')은 대상이 아니다 — 종일 연차일 OT 는 {@link #countFullDayLeaveOn} 게이트가
     * ATTD_400_151 로 통째 차단한다(회귀 금지 ②).
     */
    List<LeaveExemptWindowResult> selectLeaveExemptWindows(@Param("cmpnyCd") String cmpnyCd,
                                                           @Param("siteCd") String siteCd,
                                                           @Param("userCd") String userCd,
                                                           @Param("workYmd") String workYmd);

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
     * 대상 사용자가 일용직(EMPLOYMENT_TYPE='DAILY')인지 여부 — 일치 시 1 이상 반환.
     * 초과근무 등록 경로(updateUserOvertimeRequests)에서 일용직 OT 등록을 fail-closed 로 차단하는 데 사용한다.
     */
    int selectDailyWorkerInScope(
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
     * @param excludeOtIds com-013-06 A - in-place 수정 시 갱신 대상 OT_ID 목록(자기 자신과의 겹침 제외).
     *                     비거나 null 이면 제외 없이 기존과 동일하게 동작한다.
     */
    int selectOverlappingOvertimeCount(
            @Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd,
            @Param("userCd") String userCd,
            @Param("attdId") String attdId,
            @Param("reqStart") String reqStart,
            @Param("reqEnd") String reqEnd,
            @Param("excludeOtIds") java.util.List<String> excludeOtIds);

    /**
     * 근태 보정/직접수정으로 정해질 새 실근무 구간 [newStart, newEnd] 을 벗어나는 활성 OT 행 수 조회.
     *
     * <p>그 근태(attdId)에 연결된 활성 OT(DEL_YN='N' AND OT_STATUS &lt;&gt; 'CANCELLED') 중 하나라도
     * 새 실근무 범위를 초과하면 결과가 1 이상이 되어 보정을 차단한다(ATTD_400_114).
     * <ul>
     *   <li>OT 시작 &lt; newStart (앞으로 삐져나감)</li>
     *   <li>OT 종료 &gt; newEnd (뒤로 삐져나감)</li>
     *   <li>OT 종료가 NULL(미완료) — 유한 종료 없음 → 범위 보장 불가</li>
     *   <li>newEnd 가 null/blank(open, 새 퇴근 미정)인데 활성 OT 가 존재 — 범위 상한 불확정</li>
     * </ul>
     * 경계 동일(OT시작==newStart, OT종료==newEnd)은 포함으로 보아 허용한다.
     *
     * <p>시각은 ACTUAL_START_DATE(8)+ACTUAL_START_TIME(4) / ACTUAL_END_DATE(8)+ACTUAL_END_TIME(4) 을
     * CONCAT 한 12자리(yyyyMMddHHmm) 문자열로 비교한다(시계열 정렬, 오버나이트 정확).
     *
     * @param newStart 새 출근 시각 (yyyyMMddHHmm, 12자리)
     * @param newEnd   새 퇴근 시각 (yyyyMMddHHmm, 12자리). null/blank 이면 open(미정).
     */
    int countActiveOvertimeOutsideAttdWindow(
            @Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd,
            @Param("userCd") String userCd,
            @Param("attdId") String attdId,
            @Param("newStart") String newStart,
            @Param("newEnd") String newEnd);

    /**
     * 소정-07 - 단축근무자 OT 주 한도(720분) 판정용 주간 초과근무 분 합계.
     *
     * <p>합계 = ①등록·승인된 활성 OT(TB_USER_OVERTIME_MGMT, DEL_YN='N' AND OT_STATUS &lt;&gt; 'CANCELLED')의
     * WORK_MINUTES 합 + ②대기중(REQ_STATUS='01') 초과근무 요청(TB_USER_ATTD_REQ, REQ_TYPE '03'/'04')의
     * 신청 구간 분 합. 형제 술어(selectDailyOvertimeList / selectPendingOvertimeReqs)와 동일 기준이다.
     *
     * <p>대기 요청 분은 START_DATE(8)+START_TIME(4) / END_DATE(8)+END_TIME(4) 를 분으로 환산해 더한다
     * (오버나이트 정확). 음수(데이터 이상)는 0으로 클램프한다.
     *
     * <p>주 범위는 호출부(ReducedWorkOtGuardService)가 근무일이 속한 주의 월~일로 산출해 넘긴다
     * (Attd_15 주52 기준과 동일). 귀속 기준은 WORK_YMD(근무일)다.
     *
     * <p><b>★사업장 무관 집계(M-1)</b> — 집계 키는 {@code CMPNY_CD + USER_CD + 주 범위}이며
     * <b>SITE_CD 술어를 두지 않는다.</b> 연장근로 주 한도는 근로자 기준이라, 사업장으로 좁히면
     * 추가 사업장 권한을 가진 근로자가 사업장마다 720분씩 등록해 법정 한도를 넘길 수 있다.
     *
     * <p>★수정('04') 대기 요청은 원본 OT 행과 함께 계상되어 실제보다 크게 잡힐 수 있다. 컴플라이언스
     * 게이트라 과소 계상보다 과대 계상이 안전측이므로 의도된 동작이다.
     *
     * @param excludeOtIds 이번 등록에서 in-place 갱신될 OT_ID 목록(자기 자신 이중 계상 방지). null/빈 값이면 제외 없음
     * @param excludeReqId 이번 승인으로 닫힐 REQ_ID(자기 자신 이중 계상 방지). null/빈 값이면 제외 없음
     * @return 주간 초과근무 분 합계 (해당 없으면 0)
     */
    Integer selectWeeklyOvertimeMinutes(
            @Param("cmpnyCd") String cmpnyCd,
            @Param("userCd") String userCd,
            @Param("weekStartYmd") String weekStartYmd,
            @Param("weekEndYmd") String weekEndYmd,
            @Param("excludeOtIds") java.util.List<String> excludeOtIds,
            @Param("excludeReqId") String excludeReqId);

    /**
     * com-013-06 A - 관리자 직접수정 in-place UPDATE.
     *
     * 기존 OT 행(OT_ID=otId)을 요청 구간으로 갱신한다. {@link #updateUserOvertimeModify} 와 달리
     * ATTD_ID 까지 스코프(WHERE)에 포함해, 다른 일자/근태에 속한 행을 이 일자로 옮기는 변조를 차단한다.
     * (cmpny/site/user/attdId/otId scope + 활성 조건). 갱신 행이 0 이면 스코프 밖이거나 이미 취소/삭제됨.
     *
     * @param reducedClaimYn 소정-07 M-4 — 단축 대상이면 "Y"(청구 확인 기록 갱신), 아니면 null(기존 값 보존)
     * @param reducedClaimBy 소정-07 M-4 — 확인 주체 USER_CD. reducedClaimYn 이 null 이면 무시된다
     */
    int updateUserOvertimeDirect(
            @Param("otId") String otId,
            @Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd,
            @Param("userCd") String userCd,
            @Param("attdId") String attdId,
            @Param("startDate") String startDate,
            @Param("startTime") String startTime,
            @Param("endDate") String endDate,
            @Param("endTime") String endTime,
            @Param("workMinutes") int workMinutes,
            @Param("updateNo") String updateNo,
            @Param("reducedClaimYn") String reducedClaimYn,
            @Param("reducedClaimBy") String reducedClaimBy);

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
    /**
     * com-013 #6 - 관리자 직접 등록 OT 단건 소프트삭제.
     *
     * (cmpnyCd, siteCd, userCd, otId) 일치 + 활성 행(DEL_YN='N' AND OT_STATUS != 'CANCELLED')만
     * DEL_YN='Y' 로 전이한다. selectDailyOvertimeList 술어(DEL_YN='N' AND OT_STATUS != 'CANCELLED')와
     * 정합하여 삭제 즉시 목록에서 사라진다. 0행이면 스코프 밖이거나 이미 삭제/취소된 OT.
     *
     * @return 영향받은 행 수 (0 또는 1)
     */
    int deleteUserOvertimeById(
            @Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd,
            @Param("userCd") String userCd,
            @Param("otId") String otId,
            @Param("updateNo") String updateNo);

    /**
     * com-016-E - 삭제 직전 OT 1행을 (cmpny, site, user, otId) scope + 활성 조건으로 조회한다.
     *
     * 삭제 이력(HIST_TYPE='13')의 AFT_* 에 어떤 OT 구간을 지웠는지 남기기 위해
     * 실제 시작/종료(ACTUAL_*) 를 읽어온다. 활성 행(DEL_YN='N' AND OT_STATUS != 'CANCELLED')만
     * 반환하여 selectDailyOvertimeList / deleteUserOvertimeById 술어와 정합한다. 없으면 null.
     */
    DailyOvertimeResult selectOvertimeRowById(
            @Param("cmpnyCd") String cmpnyCd,
            @Param("siteCd") String siteCd,
            @Param("userCd") String userCd,
            @Param("otId") String otId);

    /**
     * @param reducedClaimYn 소정-07 M-4 — 단축 대상이면 "Y"(청구 확인 기록 갱신), 아니면 null(기존 값 보존)
     * @param reducedClaimBy 소정-07 M-4 — 확인 주체 USER_CD. reducedClaimYn 이 null 이면 무시된다
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
            @Param("updateNo") String updateNo,
            @Param("reducedClaimYn") String reducedClaimYn,
            @Param("reducedClaimBy") String reducedClaimBy);
}
