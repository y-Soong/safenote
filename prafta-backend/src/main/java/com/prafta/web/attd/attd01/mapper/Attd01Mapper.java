package com.prafta.web.attd.attd01.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.attd01.dto.SchCdQry;
import com.prafta.web.attd.attd01.dto.SchInfoHistQry;
import com.prafta.web.attd.attd01.dto.SchInfoHistSave;
import com.prafta.web.attd.attd01.dto.SchInfoListQry;
import com.prafta.web.attd.attd01.dto.SchInfoSave;
import com.prafta.web.attd.attd01.dto.ShiftAssignSave;
import com.prafta.web.attd.attd01.dto.ShiftCdQry;
import com.prafta.web.attd.attd01.dto.ShiftPatternSave;
import com.prafta.web.attd.attd01.dto.ShiftSchDetailQry;
import com.prafta.web.attd.attd01.dto.ShiftSchInfoListQry;
import com.prafta.web.attd.attd01.dto.ShiftTeamSave;
import com.prafta.web.attd.attd01.dto.ShiftTypeSave;
import com.prafta.web.attd.attd01.vo.SchHist;
import com.prafta.web.attd.attd01.vo.SchInfo;
import com.prafta.web.attd.attd01.vo.ShiftAssignInfo;
import com.prafta.web.attd.attd01.vo.ShiftPatternInfo;
import com.prafta.web.attd.attd01.vo.ShiftSchInfo;
import com.prafta.web.attd.attd01.vo.ShiftTeamInfo;
import com.prafta.web.attd.attd01.vo.ShiftTypeInfo;

@Mapper
public interface Attd01Mapper {
	
	List<SchInfo> selectSchInfoList(@Param(value = "param") SchInfoListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	String selectSchCd(@Param(value = "param") SchCdQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void updateSchInfo(@Param(value = "param") SchInfoSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	int selectSchHistIdx(@Param(value = "param") SchInfoHistQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void insertSchHistInfo(@Param(value = "param") SchInfoHistSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	List<SchHist> selectSchHistList(@Param(value = "param") SchInfoHistQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	String selectShiftCd(@Param(value = "param") ShiftCdQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void insertShiftSch(@Param(value = "param") ShiftTypeSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void insertShiftSchPtrn(@Param(value = "param") ShiftPatternSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void insertShiftSchTeam(@Param(value = "param") ShiftTeamSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void insertShiftSchAssign(@Param(value = "param") ShiftAssignSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	List<ShiftSchInfo> selectShiftSchInfoList(@Param(value = "param") ShiftSchInfoListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	List<ShiftTypeInfo> selectShiftTypeInfoList(@Param(value = "param") ShiftSchDetailQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	List<ShiftPatternInfo> selectShiftPatternInfoList(@Param(value = "param") ShiftSchDetailQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	List<ShiftTeamInfo> selectShiftTeamInfoList(@Param(value = "param") ShiftSchDetailQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	List<ShiftAssignInfo> selectShiftAssignInfoList(@Param(value = "param") ShiftSchDetailQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
