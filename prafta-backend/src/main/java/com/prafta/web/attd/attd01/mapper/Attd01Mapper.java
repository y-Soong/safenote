package com.prafta.web.attd.attd01.mapper;

import java.util.List;
import java.util.Map;

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
import com.prafta.web.attd.attd01.application.query.ShiftCdQuery;
import com.prafta.web.attd.attd01.application.query.ShiftSchDetailQuery;
import com.prafta.web.attd.attd01.application.query.ShiftSchInfoListQuery;
import com.prafta.web.attd.attd01.result.SchHistResult;
import com.prafta.web.attd.attd01.result.SchInfoResult;
import com.prafta.web.attd.attd01.result.ShiftAssignInfoResult;
import com.prafta.web.attd.attd01.result.ShiftPatternInfoResult;
import com.prafta.web.attd.attd01.result.ShiftSchInfoResult;
import com.prafta.web.attd.attd01.result.ShiftTeamInfoResult;
import com.prafta.web.attd.attd01.result.ShiftTypeInfoResult;

@Mapper
public interface Attd01Mapper {
	
	List<SchInfoResult> selectSchInfoList(SchInfoListQuery query);
	
	String selectSchCd(SchCdQuery query);
	
	void updateSchInfo(SchInfoCommand command);
	
	int selectSchHistIdx(SchInfoHistQuery query);
	
	void insertSchHistInfo(SchInfoHistCommand command);
	
	List<SchHistResult> selectSchHistList(SchInfoHistQuery query);
	
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
