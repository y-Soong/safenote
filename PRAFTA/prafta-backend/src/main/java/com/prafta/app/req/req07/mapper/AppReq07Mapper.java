package com.prafta.app.req.req07.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.app.req.req07.application.command.AttdReqInsertCommand;
import com.prafta.app.req.req07.dto.response.result.ActualAttdWindowResult;
import com.prafta.app.req.req07.dto.response.result.AppliedOvertimeResult;
import com.prafta.app.req.req07.dto.response.result.PendingOvertimeResult;
import com.prafta.app.req.req07.dto.response.result.ScheduleWindowResult;
import com.prafta.app.req.req07.dto.response.result.SchedOptionResult;

/**
 * prafta-app-007: 모바일 앱 근태 요청 폼 3종 (스케줄 수정 / 근태 보정 / 초과근무) 전용 Mapper.
 *
 * <p>REQ_ID 채번은 LeaveFlowMapper.selectNextReqId 와 동등한 SQL 을 복제 (네임스페이스 분리, dependency 최소화).
 */
@Mapper
public interface AppReq07Mapper {

    /**
     * REQ_ID 채번 (CONCAT(YYYYMMDD, FNC_CMM_SEQ_NEXTVAL)).
     * LeaveFlowMapper.selectNextReqId 와 동일 시퀀스('ATTD_REQ_ID') 사용 — 채번 공간 공유.
     */
    String selectNextReqId(@Param("cmpnyCd") String cmpnyCd);

    /**
     * tb_user_attd_req INSERT (3 endpoint 공통).
     * 각 endpoint 가 사용하지 않는 컬럼은 command 에서 null 로 전달.
     */
    int insertAttdReq(AttdReqInsertCommand command);

    /**
     * 중복 요청 차단 (P10): 동일 USER_CD + WORK_YMD + REQ_TYPE + REQ_STATUS='01' 미처리 행 카운트.
     * 결과 > 0 이면 4xx (ATTD_400_090).
     */
    int countDuplicateReq(@Param("cmpnyCd") String cmpnyCd
                          , @Param("siteCd") String siteCd
                          , @Param("userCd") String userCd
                          , @Param("workYmd") String workYmd
                          , @Param("reqType") String reqType);

    /**
     * 근태 보정 자동 분기 (Q2): 해당 (USER_CD, WORK_YMD, WORK_SEQ) 의 미삭제 tb_user_attd_mgmt 행의 ATTD_ID 조회.
     * 결과:
     * <ul>
     *   <li>행 존재 → ATTD_ID 반환 → REQ_TYPE='02' (근태 수정), TARGET_ID=반환값.</li>
     *   <li>행 부재 → null 반환 → REQ_TYPE='01' (근태 생성), TARGET_ID=null.</li>
     * </ul>
     */
    String selectExistingAttdId(@Param("cmpnyCd") String cmpnyCd
                                , @Param("siteCd") String siteCd
                                , @Param("userCd") String userCd
                                , @Param("workYmd") String workYmd
                                , @Param("workSeq") Integer workSeq);

    /**
     * prafta-app-007 F2: 스케줄 선택 옵션 목록 조회.
     * tb_sch_mgmt 의 사용중(USE_YN='Y') 스케줄을 회사/사업장 범위로 조회한다.
     * 식별값(cmpnyCd/siteCd)은 컨트롤러에서 JWT 로만 도출 (IDOR 가드).
     * 결과 없으면 빈 List.
     */
    List<SchedOptionResult> selectSchedOptions(@Param("cmpnyCd") String cmpnyCd
                                               , @Param("siteCd") String siteCd);

    /**
     * prafta-com-008-E-9a: 사용자 본인 기본 근무타입(tb_user.DEFAULT_SCH_CD) 조회.
     * 스케줄 선택 UI 의 "기본" 칩/상단 정렬 기준(사업장 BASE_YN 폐기 대체). 미설정이면 null.
     * 식별값(cmpnyCd/userCd)은 JWT 도출값만 사용(IDOR).
     */
    String selectUserDefaultSchCd(@Param("cmpnyCd") String cmpnyCd
                                  , @Param("userCd") String userCd);

    /**
     * prafta-app-017(이슈①): OT 겹침 검증용 — 해당 근무일의 근무계획 스케줄 1건(1·2구간 시각) 조회.
     * TB_USER_WORK_PLAN(하루 1행).WORK_PLAN_CD 가 SCH_CD 일 때만 TB_SCH_MGMT 조인이 성립한다.
     * 연차코드(LEAVE_CD)/미배정(NULL)/매칭 스케줄 없음 → 결과 0행 → null 반환(정규구간 부재 → 겹침검사 면제).
     * 식별값은 Param 의 JWT 도출값만 사용(IDOR).
     */
    ScheduleWindowResult selectWorkPlanSchedule(@Param("cmpnyCd") String cmpnyCd
                                                , @Param("siteCd") String siteCd
                                                , @Param("userCd") String userCd
                                                , @Param("workYmd") String workYmd);

    /**
     * prafta-app-017(이슈②): 미처리 근태보정 요청(생성01·수정02) 카운트 — 구간 단위.
     * REQ_TYPE IN ('01','02') AND REQ_STATUS='01' AND DEL_YN='N', 해당 WORK_SEQ.
     * 결과 > 0 이면 그 구간 OT 거부(ATTD_400_101).
     */
    int countPendingAttdCorrectionBySlot(@Param("cmpnyCd") String cmpnyCd
                                         , @Param("siteCd") String siteCd
                                         , @Param("userCd") String userCd
                                         , @Param("workYmd") String workYmd
                                         , @Param("workSeq") Integer workSeq);

    /**
     * PRAFTA-APP-022 룰A1(prafta-app-017 이슈② 확장): 미처리 스케줄수정 요청(10) 카운트 — 그날 전체(구간 무관).
     * REQ_TYPE='10' AND REQ_STATUS='01' AND DEL_YN='N'.
     * (2026-06-21 정책정정: 대기01만 충돌. 승인02/반려03/취소04 등 처리 완료분은 비차단.)
     * 결과 > 0 이면 그날 모든 구간 OT 거부(ATTD_400_106).
     */
    int countActiveSchedModify(@Param("cmpnyCd") String cmpnyCd
                               , @Param("siteCd") String siteCd
                               , @Param("userCd") String userCd
                               , @Param("workYmd") String workYmd);

    /**
     * PRAFTA-APP-022 룰A2/A3: 미처리 초과근무 요청(생성03·수정04) 카운트 — 그날 전체(WORK_SEQ 무관).
     * REQ_TYPE IN ('03','04') AND REQ_STATUS='01' AND DEL_YN='N'.
     * (2026-06-21 정책정정: 대기01만 충돌. 승인02/반려03/취소04 등 처리 완료분은 비차단.)
     * 결과 > 0 이면 스케줄수정 거부(ATTD_400_107). 식별값은 JWT 도출 Param 만 사용(IDOR).
     */
    int countActiveOvertimeReq(@Param("cmpnyCd") String cmpnyCd
                               , @Param("siteCd") String siteCd
                               , @Param("userCd") String userCd
                               , @Param("workYmd") String workYmd);

    /**
     * prafta-app-019(1-A): OT 실근태 범위 검증용 — 해당 (USER_CD, WORK_YMD, WORK_SEQ) 의
     * 실제 근태기록 1건([CHECK_IN ~ CHECK_OUT] 원본 시각) 조회.
     * TB_USER_ATTD_MGMT 에서 DEL_YN='N' 으로 1건 조회한다. 행 부재면 null 반환 → 그 구간 범위 확정 불가
     * → OT 거부(ATTD_400_104). 표준화 적용시각이 아닌 원본 CHECK_IN/OUT 시각을 내려준다.
     * 식별값은 Param 의 JWT 도출값만 사용(IDOR).
     * ⚠️ SELECT 컬럼 순서 = ActualAttdWindowResult 생성자 인자 순서(위치 기반 record 매핑).
     */
    ActualAttdWindowResult selectActualAttdWindowBySlot(@Param("cmpnyCd") String cmpnyCd
                                                        , @Param("siteCd") String siteCd
                                                        , @Param("userCd") String userCd
                                                        , @Param("workYmd") String workYmd
                                                        , @Param("workSeq") Integer workSeq);

    /**
     * prafta-app-009 F13: 해당 (USER_CD, WORK_YMD) 본인 근무계획 행 존재 여부.
     * TB_USER_WORK_PLAN 하루 1행 기준(스케줄/연차 무관 — 배정 자체의 존재만 확인).
     * 스케줄 수정/근태 보정은 배정된 근무일에 대해서만 요청 가능. 0 이면 ATTD_400_098 거부.
     * 식별값은 JWT 도출 Param 만 사용(IDOR).
     */
    int countUserWorkPlan(@Param("cmpnyCd") String cmpnyCd
                          , @Param("siteCd") String siteCd
                          , @Param("userCd") String userCd
                          , @Param("workYmd") String workYmd);

    /**
     * prafta-app-009 F15: MySQL advisory lock 획득(GET_LOCK). 중복 차단 SELECT→INSERT race window 를
     * 같은 (회사+사업장+사용자+일자+요청유형) 키로 직렬화한다. 1=획득, 0=타임아웃, null=오류.
     * 트랜잭션 종료(커밋/롤백)와 무관하게 세션 단위로 잡히므로, 호출부가 finally 에서 releaseAdvisoryLock 한다.
     */
    Integer getAdvisoryLock(@Param("lockKey") String lockKey, @Param("timeoutSec") int timeoutSec);

    /** prafta-app-009 F15: advisory lock 해제(RELEASE_LOCK). */
    Integer releaseAdvisoryLock(@Param("lockKey") String lockKey);

    /**
     * prafta-app-030: 이미 등록(적용)된 초과근무 목록 조회 — 신규 OT 신청 시 표시/겹침 대조 원천.
     * TB_USER_OVERTIME_MGMT 에서 DEL_YN='N' AND OT_STATUS&lt;&gt;'CANCELLED'
     * AND ACTUAL_END_DATE IS NOT NULL AND ACTUAL_END_TIME IS NOT NULL 인 행을
     * (cmpny/site/user/workYmd) 스코프로 조회한다. 식별값은 JWT 도출 Param 만 사용(IDOR).
     * 결과 없으면 빈 List. ★TB_USER_OVERTIME_MGMT 에 WORK_SEQ 컬럼 없음 → 미포함.
     * ⚠️ SELECT 컬럼 순서 = AppliedOvertimeResult 생성자 인자 순서(위치 기반 record 매핑).
     */
    List<AppliedOvertimeResult> selectAppliedOvertimes(@Param("cmpnyCd") String cmpnyCd
                                                       , @Param("siteCd") String siteCd
                                                       , @Param("userCd") String userCd
                                                       , @Param("workYmd") String workYmd);

    /**
     * prafta-app-030 후속: 대기중(미승인) 초과근무 신청 목록 조회 — 신규 OT 신청 시 표시 전용.
     * TB_USER_ATTD_REQ 에서 REQ_TYPE IN ('03','04') AND REQ_STATUS='01' AND DEL_YN='N'
     * AND START/END_DATE·TIME IS NOT NULL 인 행을 (cmpny/site/user/workYmd) 스코프로 조회한다.
     * 식별값은 JWT 도출 Param 만 사용(IDOR). 결과 없으면 빈 List.
     * 승인분('02')은 TB_USER_OVERTIME_MGMT(selectAppliedOvertimes)로 노출되므로 본 쿼리는 '01'만 → 이중표시 없음.
     * ⚠️ SELECT 컬럼 순서 = PendingOvertimeResult 생성자 인자 순서(위치 기반 record 매핑).
     */
    List<PendingOvertimeResult> selectPendingOvertimeReqs(@Param("cmpnyCd") String cmpnyCd
                                                          , @Param("siteCd") String siteCd
                                                          , @Param("userCd") String userCd
                                                          , @Param("workYmd") String workYmd);

    /**
     * 근태 보정으로 정해질 새 실근무 구간 [newStart, newEnd] 을 벗어나는 활성 OT 행 수 조회.
     *
     * <p>그 근태(attdId)에 연결된 활성 OT(DEL_YN='N' AND OT_STATUS&lt;&gt;'CANCELLED') 중 하나라도
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
     * CONCAT 한 12자리(yyyyMMddHHmm) 문자열로 비교한다(시계열 정렬, 오버나이트 정확). 식별값은 JWT 도출 Param 만 사용(IDOR).
     *
     * @param newStart 새 출근 시각 (yyyyMMddHHmm, 12자리)
     * @param newEnd   새 퇴근 시각 (yyyyMMddHHmm, 12자리). null/blank 이면 open(미정).
     */
    int countActiveOvertimeOutsideAttdWindow(@Param("cmpnyCd") String cmpnyCd
                                             , @Param("siteCd") String siteCd
                                             , @Param("userCd") String userCd
                                             , @Param("attdId") String attdId
                                             , @Param("newStart") String newStart
                                             , @Param("newEnd") String newEnd);
}
