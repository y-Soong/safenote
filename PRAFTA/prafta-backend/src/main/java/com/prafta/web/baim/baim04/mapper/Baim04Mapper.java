package com.prafta.web.baim.baim04.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.baim.baim04.application.command.DailyUserSlotCommand;
import com.prafta.web.baim.baim04.application.command.LinkPoliciesCommand;
import com.prafta.web.baim.baim04.application.param.UserSlotCountQuery;
import com.prafta.web.baim.baim04.application.query.DailyUserLinkPoliciesQuery;
import com.prafta.web.baim.baim04.result.DailyUserLinkPolicyResult;

@Mapper
public interface Baim04Mapper {
	List<DailyUserLinkPolicyResult> selectDailyUserLinkPolicyList(DailyUserLinkPoliciesQuery query);
	
	void saveDailyUserLinkPolicy(LinkPoliciesCommand command);
	
	int selectDailyUserSlotCnt(UserSlotCountQuery query);
	
	void saveDailyUserSlot(DailyUserSlotCommand command);
	
	void deleteDailyUserLinkPolicy(DailyUserSlotCommand command);
}
