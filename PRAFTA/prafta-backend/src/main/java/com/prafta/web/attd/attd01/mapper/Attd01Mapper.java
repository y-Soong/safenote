package com.prafta.web.attd.attd01.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.attd01.application.command.SchInfoCommand;
import com.prafta.web.attd.attd01.application.command.SchInfoHistCommand;
import com.prafta.web.attd.attd01.application.command.ShiftAssignCommand;
import com.prafta.web.attd.attd01.application.command.ShiftPatternCommand;
import com.prafta.web.attd.attd01.application.command.ShiftTeamCommand;
import com.prafta.web.attd.attd01.application.command.ShiftTypeCommand;
import com.prafta.web.attd.attd01.application.query.SchCdQuery;
import com.prafta.web.attd.attd01.application.query.SchInfoHistQuery;
import com.prafta.web.attd.attd01.application.query.SchInfoListQuery;
import com.prafta.web.attd.attd01.application.query.SchNoCountQuery;
import com.prafta.web.attd.attd01.application.query.ShiftCdQuery;
import com.prafta.web.attd.attd01.application.query.ShiftSchDetailQuery;
import com.prafta.web.attd.attd01.application.query.ShiftSchInfoListQuery;
import com.prafta.web.attd.attd01.application.query.ShiftSchNoCountQuery;
import com.prafta.web.attd.attd01.result.AssignedUserResult;
import com.prafta.web.attd.attd01.result.SchHistResult;
import com.prafta.web.attd.attd01.result.SchInfoResult;
import com.prafta.web.attd.attd01.result.ShiftAssignInfoResult;
import com.prafta.web.attd.attd01.result.ShiftPatternInfoResult;
import com.prafta.web.attd.attd01.result.ShiftSchInfoResult;
import com.prafta.web.attd.attd01.result.ShiftTeamInfoResult;
import com.prafta.web.attd.attd01.result.ShiftTypeInfoResult;
import com.prafta.web.attd.attd01.result.WorkPlanDayResult;

@Mapper
public interface Attd01Mapper {
	
	List<SchInfoResult> selectSchInfoList(SchInfoListQuery query);
	
	int selectSchNoCount(SchNoCountQuery query);
	
	String selectSchCd(SchCdQuery query);
	
	void updateSchInfo(SchInfoCommand command);
	
	int selectSchHistIdx(SchInfoHistQuery query);
	
	void insertSchHistInfo(SchInfoHistCommand command);
	
	List<SchHistResult> selectSchHistList(SchInfoHistQuery query);

	/**
	 * com-016-A 공통 가드 ③: 근무타입(SCH_CD) 시간/휴게 변경 시 영향 판정용.
	 * 변경 대상 SCH_CD 의 시간/휴게가 입력값과 1건이라도 다르면 1, 동일하거나 기존 행이 없으면 0.
	 * (입력값 정규화는 updateSchInfo upsert 와 동일 — 콜론 제거 / 빈값 NULLIF / 2구간 IFNULL.)
	 */
	int selectSchTimeBreakChanged(SchInfoCommand command);

	/**
	 * com-016-A 공통 가드 ③: 해당 근무타입(SCH_CD)을 쓰는 미래 적용분 근무계획 (userCd, workYmd) 목록.
	 * 회사/사업장 스코프. WORK_YMD &gt;= 기준일(fromYmd, YYYYMMDD).
	 */
	List<WorkPlanDayResult> selectFutureWorkPlanDaysBySchCd(@Param("cmpnyCd") String cmpnyCd,
			@Param("siteCd") String siteCd,
			@Param("schCd") String schCd,
			@Param("fromYmd") String fromYmd);

	/**
	 * 사용중지 가드: 저장 전 근무타입(SCH_CD)의 현재 USE_YN. 신규 생성 등 기존 행이 없으면 null.
	 * Y→N 전환일 때만 미래 근무계획 배정 여부를 검사하기 위한 판정값.
	 */
	String selectSchUseYn(@Param("cmpnyCd") String cmpnyCd,
			@Param("siteCd") String siteCd,
			@Param("schCd") String schCd);

	/**
	 * PRAFTA-SUBCON-T2-04: 사업장의 연동 원본 회사코드(NULL=일반, NOT NULL=미러=근무타입 전면 잠금).
	 * 행 미존재 시 null. (subcon 모듈 의존을 피해 attd01 패키지에 동형 쿼리를 둠 — 모듈 경계 보존)
	 */
	String selectSiteLinkSrcCmpny(@Param("gvCmpnyCd") String gvCmpnyCd, @Param("siteCd") String siteCd);

	/**
	 * F-12-2: 근무타입(SCH_CD)별 배정현황 — 사용자별 최초~최근 배정일 + 배정일수.
	 * guardScheduleDeactivate 와 동일 기준(WORK_YMD &gt;= fromYmd)으로 집계해, 팝업 목록이
	 * 실제 사용중지 시 차단되는 대상과 일치하도록 맞춘다.
	 */
	// 보안수정(security High): scopeNodeCd — master/hr/safe 는 null(사업장 전체),
	// 그 외는 세션 부서(gv_nodeCd) 앵커(+하위)로 조회 범위를 강제(Attd01ServiceImpl 게이트와 짝).
	List<AssignedUserResult> selectAssignedUsersBySchCd(@Param("cmpnyCd") String cmpnyCd,
			@Param("siteCd") String siteCd,
			@Param("schCd") String schCd,
			@Param("fromYmd") String fromYmd,
			@Param("scopeNodeCd") String scopeNodeCd);

	int selectShiftSchNoCount(ShiftSchNoCountQuery query);
	
	String selectShiftCd(ShiftCdQuery query);
	
	void insertShiftSch(ShiftTypeCommand command);
	
	void insertShiftSchPtrn(ShiftPatternCommand command);
	
	void insertShiftSchTeam(ShiftTeamCommand command);
	
	void insertShiftSchAssign(ShiftAssignCommand command);
	
	List<ShiftSchInfoResult> selectShiftSchInfoList(ShiftSchInfoListQuery query);
	
	List<ShiftTypeInfoResult> selectShiftTypeInfoList(ShiftSchDetailQuery query);
	
	List<ShiftPatternInfoResult> selectShiftPatternInfoList(ShiftSchDetailQuery query);
	
	List<ShiftTeamInfoResult> selectShiftTeamInfoList(ShiftSchDetailQuery query);
	
	List<ShiftAssignInfoResult> selectShiftAssignInfoList(ShiftSchDetailQuery query);
}
