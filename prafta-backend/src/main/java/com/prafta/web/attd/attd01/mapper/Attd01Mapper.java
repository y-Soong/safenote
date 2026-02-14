package com.prafta.web.attd.attd01.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.attd.attd01.dto.SchInfoListQry;
import com.prafta.web.attd.attd01.vo.SchInfo;

@Mapper
public interface Attd01Mapper {
	
	List<SchInfo> selectSchInfoList(@Param(value = "param") SchInfoListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
//	List<DailyUserSlotList> selectDailyUserSlotList(@Param(value = "param") DailyUserSlotListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
