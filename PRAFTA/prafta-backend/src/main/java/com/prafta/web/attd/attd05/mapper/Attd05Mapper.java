package com.prafta.web.attd.attd05.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.attd.attd05.application.command.SchTypeCommand;
import com.prafta.web.attd.attd05.application.command.SchTypeDeleCommand;
import com.prafta.web.attd.attd05.application.query.LeaveTypeListQuery;
import com.prafta.web.attd.attd05.application.query.SchListQuery;
import com.prafta.web.attd.attd05.application.query.UserWorkPlansQuery;
import com.prafta.web.attd.attd05.result.DayResult;
import com.prafta.web.attd.attd05.result.LeaveOverlayResult;
import com.prafta.web.attd.attd05.result.LeaveTypeResult;
import com.prafta.web.attd.attd05.result.SchTypeResult;
import com.prafta.web.attd.attd05.result.SchTypeUseYnResult;
import com.prafta.web.attd.attd05.result.SchedResult;
import com.prafta.web.attd.attd05.result.ShiftLockOverlayResult;
import com.prafta.web.attd.attd05.result.UserResult;

@Mapper
public interface Attd05Mapper {
	
	List<UserResult> selectUserList(UserWorkPlansQuery query);
	
	List<DayResult> selectDayList(UserWorkPlansQuery query);
	
	List<SchedResult> selectSchedList(UserWorkPlansQuery query);

	/**
	 * prafta-com-008-E-6: 그리드 연차 오버레이 — 조회 스코프(노드트리/사업장/월/사용자명) 내
	 * 종일 확정 연차(leave_use, USE_UNIT_TYPE='00', CONFIRMED, DEL_YN='N')를 (userCd, workYmd, leaveCd) 로 반환.
	 * 셀의 "연차" 표시 단일 출처(work_plan 코드가 아닌 leave_use 기준 — E-2).
	 */
	List<LeaveOverlayResult> selectLeaveOverlayList(UserWorkPlansQuery query);

	/**
	 * 그리드 부분 휴가(반차/시간차) 오버레이 — 조회 스코프(노드트리/사업장/월/사용자명) 내
	 * 종일이 아닌 확정 휴가(leave_use, USE_UNIT_TYPE != '00', CONFIRMED, DEL_YN='N')를
	 * (userCd, workYmd, 사용단위, 시간대, 사용분) 으로 반환한다. 종일 오버레이가 셀 값을 대체하는 것과 달리,
	 * 부분 휴가는 근무 스케줄(SCH_CD)이 살아있으므로 셀 위에 마커(칩)로만 얹는다(셀 값 미변경).
	 */
	List<com.prafta.web.attd.attd05.result.PartialLeaveOverlayResult> selectPartialLeaveOverlayList(UserWorkPlansQuery query);

	/**
	 * prafta-com-008-D-5: 그리드 교대 잠금 오버레이 — 조회 스코프(노드트리/사업장/월/사용자명) 사용자의
	 * 교대팀 소속 구간(잠금) 일자를 (userCd, workYmd) 로 펼쳐 반환한다.
	 * 판정은 D-1 경계 술어(팀 STR_DATE~END_DATE inclusive ∩ 멤버십 행 존재 ∩ LEAVE_TEAM_YMD 미포함)를
	 * 그리드 조회월 범위로 확장한 단일 출처. 프론트가 SCH 셀 비활성/자물쇠 표시에 사용한다(연차 셀은 활성).
	 */
	List<ShiftLockOverlayResult> selectShiftLockOverlayList(UserWorkPlansQuery query);

	List<SchTypeResult> selectSchTypeList(SchListQuery query);

	/**
	 * 근무타입(SCH_CD)별 effective-dating 버전 목록.
	 * TB_SCH_MGMT(현재본) + TB_SCH_MGMT_HIST(이력본) 합집합을
	 * 동일 SCH_CD+APPLY_DATE 중복 시 MAX(HIST_IDX) 1건으로 정리하여 반환한다.
	 * USE_YN='N' 버전도 차단 근거로 살려야 하므로 USE_YN 필터를 걸지 않는다.
	 */
	List<SchTypeUseYnResult> selectSchTypeUseYnList(SchListQuery query);

	List<LeaveTypeResult> selectLeaveTypeList(LeaveTypeListQuery query);

	/** 법정 휴가(LEAVE_NATURE_TYPE='01') 연차코드 목록 — 저장 시 직접 차감 대상 판별용 (prafta-021). */
	List<String> selectLegalLeaveCds(@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd);
	
	void saveUserWorkPlans(SchTypeCommand command);

	void deleteUserWorkPlans(SchTypeDeleCommand command);

	/**
	 * PRAFTA-041 - 셀(사용자+근무일)의 현재 WORK_PLAN_CD 조회 — 비우기 시 법정연차 여부 판별(서버 권위).
	 * 행이 없으면 null.
	 */
	String selectWorkPlanCellCd(com.prafta.web.attd.attd05.application.command.WorkPlanCellDeleCommand command);

	/**
	 * PRAFTA-041 - 근무계획 셀(사용자+근무일) 단위 삭제(PK 완전 일치 DELETE).
	 * @return 삭제된 행 수
	 */
	int deleteUserWorkPlanCell(com.prafta.web.attd.attd05.application.command.WorkPlanCellDeleCommand command);

	/**
	 * prafta-com-008-E-6: 대상 사용자의 기본 근무타입(tb_user.DEFAULT_SCH_CD). 미설정이면 null.
	 * 연차 셀 적용 시 work_plan 에 LEAVE_CD 대신 이 SCH_CD 를 유지/기록한다(모델 전환).
	 */
	String selectUserDefaultSchCd(@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd,
			@org.apache.ibatis.annotations.Param("userCd") String userCd);

	/**
	 * prafta-com-008-E-6: 해당 셀(사용자+근무일)에 종일 확정 연차(leave_use, USE_UNIT_TYPE='00') 존재 카운트.
	 * 셀 비우기 시 "연차였는지" 판정을 work_plan 코드가 아니라 leave_use 존재로 한다(E-2 단일출처).
	 */
	int countFullDayLeaveUseOnCell(@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd,
			@org.apache.ibatis.annotations.Param("userCd") String userCd,
			@org.apache.ibatis.annotations.Param("workYmd") String workYmd);

	/**
	 * prafta-com-008-E-6: 셀 비우기 시 취소 대상 직접(REQ_ID NULL) 종일 연차의 LEAVE_CD 조회.
	 * cancelDirectLeaveUsage 가 leaveCd 키를 요구하므로, work_plan 코드(SCH_CD 화) 대신 leave_use 에서 도출.
	 * 없으면 null(취소 대상 없음).
	 */
	String selectDirectFullDayLeaveCdOnCell(@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd,
			@org.apache.ibatis.annotations.Param("userCd") String userCd,
			@org.apache.ibatis.annotations.Param("workYmd") String workYmd);

	/**
	 * prafta-com-008-E (M2): 셀 비우기 시 승인기반(REQ_ID NOT NULL) 종일 연차의 LEAVE_CD 조회.
	 * 존재하면 셀에서 직접 삭제할 수 없고 연차 변경/삭제 요청(attd13 동의흐름)을 이용해야 하므로 skip 대상.
	 * 없으면 null(승인기반 연차 없음).
	 *
	 * <p>prafta-com-008-B-5(D-E8): 전면 동의 원칙으로 직접/승인 모두 통합 판정(selectFullDayLeaveCdOnCell)으로
	 * 대체되어 비우기 경로에서는 더 이상 호출되지 않는다(쿼리 자체는 호환 위해 보존).
	 */
	String selectApprovedFullDayLeaveCdOnCell(@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd,
			@org.apache.ibatis.annotations.Param("userCd") String userCd,
			@org.apache.ibatis.annotations.Param("workYmd") String workYmd);

	/**
	 * prafta-com-008-B-5(D-E8): 해당 셀(사용자+근무일)에 종일 확정 연차(leave_use, USE_UNIT_TYPE='00')가
	 * 존재하면 그 LEAVE_CD 1건을 반환한다. <b>REQ_ID 무관(직접/승인 모두)</b> — 전면 동의 원칙에 따라
	 * 연차 등록일 셀은 비우기로 삭제할 수 없고 연차 변경/삭제 요청(attd13)을 이용해야 하므로 skip 판정에 쓴다.
	 * 그리드 오버레이(selectLeaveOverlayList)와 동일하게 START_DATE~END_DATE 구간 포함으로 판정한다.
	 * 없으면 null.
	 */
	String selectFullDayLeaveCdOnCell(@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd,
			@org.apache.ibatis.annotations.Param("userCd") String userCd,
			@org.apache.ibatis.annotations.Param("workYmd") String workYmd);

	/**
	 * 해당 셀(사용자+근무일)에 근무 스케줄(SCH 배정) 존재 카운트 — 연차 적용 가드.
	 * TB_USER_WORK_PLAN 행이 있고 그 WORK_PLAN_CD 가 TB_SCH_MGMT 의 실제 근무타입(SCH_CD)인 경우만 카운트.
	 * 빈 셀(근무 스케줄 미배정)이거나 WORK_PLAN_CD 가 연차코드(LEAVE_CD)면 0 → 연차 적용 거부(스킵).
	 * (E-6 모델: 이미 연차 적용된 셀은 DEFAULT_SCH_CD 가 들어가 SCH 로 잡히므로 연차 멱등 재적용은 허용.)
	 */
	int countUserWorkPlanSchedule(@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd,
			@org.apache.ibatis.annotations.Param("siteCd") String siteCd,
			@org.apache.ibatis.annotations.Param("userCd") String userCd,
			@org.apache.ibatis.annotations.Param("workYmd") String workYmd);

	/**
	 * 해당 셀(사용자+근무일)에 초과근무 보유 카운트 — 스케줄 변경 차단 판정(앱 스케줄수정요청 게이트와 정합).
	 * 등록 초과근무(tb_user_overtime_mgmt, 취소 제외) ∪ 초과근무 신청(tb_user_attd_req 03/04, 상태 신청/승인).
	 * &gt; 0 이면 그 일자는 스케줄(SCH) 변경 불가.
	 */
	int countDayOvertime(@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd,
			@org.apache.ibatis.annotations.Param("siteCd") String siteCd,
			@org.apache.ibatis.annotations.Param("userCd") String userCd,
			@org.apache.ibatis.annotations.Param("workYmd") String workYmd);

	/**
	 * 근태 E2E F4(4-13): 해당 셀(사용자+근무일)에 처리 대기 중인 스케줄수정요청 보유 카운트.
	 * TB_USER_ATTD_REQ 의 REQ_TYPE='10'(스케줄수정), REQ_STATUS='01'(신청/대기), DEL_YN='N' 만 카운트한다.
	 * 승인('02')/반려('03')/취소('04')된 요청은 잠그지 않는다(오탐 방지).
	 * &gt; 0 이면 그 일자는 관리자 직접 스케줄 변경 불가(경합 방지 — 대기 요청이 참조할 기준 스케줄 보호).
	 */
	int countPendingSchModifyReq(@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd,
			@org.apache.ibatis.annotations.Param("siteCd") String siteCd,
			@org.apache.ibatis.annotations.Param("userCd") String userCd,
			@org.apache.ibatis.annotations.Param("workYmd") String workYmd);

	/**
	 * 해당 월(YYYYMM)에 초과근무 보유 일자 카운트 — 월 단위 근무계획 삭제 차단 판정.
	 * 등록 초과근무(취소 제외) ∪ 초과근무 신청(03/04, 신청/승인). &gt; 0 이면 월 삭제 차단.
	 * <p>prafta-com-016-C-3: 월 삭제가 "전체 차단"에서 "OT일 부분 제외 삭제"로 바뀌어 본 카운트는
	 *   더 이상 차단 판정에 쓰지 않는다(원문 LEFT(...,6)=workYm 포맷 버그 경로 폐기). 호환 위해 시그니처만 보존.
	 */
	int countMonthOvertime(@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd,
			@org.apache.ibatis.annotations.Param("siteCd") String siteCd,
			@org.apache.ibatis.annotations.Param("userCd") String userCd,
			@org.apache.ibatis.annotations.Param("workYm") String workYm);

	/**
	 * prafta-com-016-C-3: 해당 월(YYYYMM)에 초과근무(등록 또는 신청) 보유 일자(WORK_YMD, YYYYMMDD) 목록.
	 * 월 부분 삭제에서 제외된 OT 보유일을 사용자에게 안내(BatchResultPop)하기 위한 조회.
	 * 판정 술어는 countDayOvertime 와 동일(등록 비취소 ∪ 신청 03/04 상태 01/02). workYm 은 YYYYMM(8자리 LEFT 매칭).
	 */
	List<String> selectMonthOvertimeDays(@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd,
			@org.apache.ibatis.annotations.Param("siteCd") String siteCd,
			@org.apache.ibatis.annotations.Param("userCd") String userCd,
			@org.apache.ibatis.annotations.Param("workYm") String workYm);

	/**
	 * prafta-com-016-C-3 후속: 해당 월(YYYYMM)에 확정 연차로 삭제에서 보존된 일자(WORK_YMD) 목록.
	 * 월 부분 삭제에서 제외된 연차 보존일을 사용자에게 안내(BatchResultPop)하기 위한 조회.
	 * E3(당일분모 전환, DFCT-1 보완): 보존이 전 단위(USE_UNIT_TYPE 무관)로 확장되어 안내도 종일 한정 해제.
	 * 판정 술어는 deleteUserWorkPlans 의 확정 연차 NOT EXISTS 와 동일. workYm 은 YYYYMM(6자리 LEFT 매칭).
	 */
	List<String> selectMonthLeaveDays(@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd,
			@org.apache.ibatis.annotations.Param("siteCd") String siteCd,
			@org.apache.ibatis.annotations.Param("userCd") String userCd,
			@org.apache.ibatis.annotations.Param("workYm") String workYm);

	/**
	 * E3(당일분모 전환, DFCT-1 보완): 해당 월(YYYYMM)에 미결 시간차 신청으로 삭제에서 보존된
	 * 일자(WORK_YMD) 목록 — BatchResultPop 안내용(HAS_PENDING_LEAVE).
	 * 판정 술어는 deleteUserWorkPlans 의 미결 시간차 NOT EXISTS 와 동일(F-F — ScheduleGuardMapper 동형).
	 */
	List<String> selectMonthPendingHourlyReqDays(@org.apache.ibatis.annotations.Param("cmpnyCd") String cmpnyCd,
			@org.apache.ibatis.annotations.Param("siteCd") String siteCd,
			@org.apache.ibatis.annotations.Param("userCd") String userCd,
			@org.apache.ibatis.annotations.Param("workYm") String workYm);
}
