package com.prafta.web.attd.attd03.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.attd03.dto.LeaveNoDupChkQry;
import com.prafta.web.attd.attd03.dto.LeaveTypeListQry;
import com.prafta.web.attd.attd03.dto.LeaveTypeSave;
import com.prafta.web.attd.attd03.vo.LeaveNoDupChk;
import com.prafta.web.attd.attd03.vo.LeaveType;

@Mapper
public interface Attd03Mapper {
	
	void updateLeaveType(@Param(value = "param") LeaveTypeSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	String selectLeaveCd(@Param(value = "token") Map<String, Object> tokenInfo);
	
	List<LeaveType> selectLeaves(@Param(value = "param") LeaveTypeListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	LeaveNoDupChk selectLeaveNoDupChkResult(@Param(value = "param") LeaveNoDupChkQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
//	List<Holiday> selectHoliday(@Param(value = "param") HolidayListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
//	
//	String selectHolidayRuleId(@Param(value = "token") Map<String, Object> tokenInfo);
//	
//	String selectHolidayId(@Param(value = "token") Map<String, Object> tokenInfo);
//	
//	void updateHolidayRule(@Param(value = "param") HolidaySave dto, @Param(value = "token") Map<String, Object> tokenInfo);
//	
//	void updateHoliday(@Param(value = "param") HolidaySave dto, @Param(value = "token") Map<String, Object> tokenInfo);
//	
//	
//	List<SchInfo> selectSchInfoList(@Param(value = "param") SchInfoListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
//	
//	String selectSchCd(@Param(value = "param") SchCdQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
//	
//	void updateSchInfo(@Param(value = "param") SchInfoSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
//	
//	int selectSchHistIdx(@Param(value = "param") SchInfoHistQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
//	
//	void insertSchHistInfo(@Param(value = "param") SchInfoHistSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
//	
//	List<SchHist> selectSchHistList(@Param(value = "param") SchInfoHistQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
