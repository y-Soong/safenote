package com.prafta.web.baim.baim04.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.baim.baim04.dto.DailyUserLinkPoliciesQry;
import com.prafta.web.baim.baim04.dto.DailyUserSlotSave;
import com.prafta.web.baim.baim04.dto.LinkPoliciesSave;
import com.prafta.web.baim.baim04.vo.DailyUserLinkPolicy;

@Mapper
public interface Baim04Mapper {
	List<DailyUserLinkPolicy> selectDailyUserLinkPolicyList(@Param(value = "param") DailyUserLinkPoliciesQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void saveDailyUserLinkPolicy(@Param(value = "param") LinkPoliciesSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	int selectDailyUserSlotCnt(@Param(value = "param") LinkPoliciesSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void saveDailyUserSlot(@Param(value = "param") DailyUserSlotSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void deleteDailyUserLinkPolicy(@Param(value = "param") LinkPoliciesSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
