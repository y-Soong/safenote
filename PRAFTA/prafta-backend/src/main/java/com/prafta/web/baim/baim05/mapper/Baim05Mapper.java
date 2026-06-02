package com.prafta.web.baim.baim05.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.baim.baim05.application.command.DailyUserSlotCommand;
import com.prafta.web.baim.baim05.application.command.DailyUserSlotUpdCommand;
import com.prafta.web.baim.baim05.application.command.InsertDailyQrUserCommand;
import com.prafta.web.baim.baim05.application.command.LinkPoliciesCommand;
import com.prafta.web.baim.baim05.application.query.DailyUserLinkPoliciesQuery;
import com.prafta.web.baim.baim05.application.query.DailyUserSlotListQuery;
import com.prafta.web.baim.baim05.application.query.UserSlotCountQuery;
import com.prafta.web.baim.baim05.result.DailyUserLinkPolicyResult;
import com.prafta.web.baim.baim05.result.DailyUserSlotListResult;

@Mapper
public interface Baim05Mapper {
	DailyUserLinkPolicyResult selectDailyUserLinkPolicyList(DailyUserLinkPoliciesQuery query);
	
	List<DailyUserSlotListResult> selectDailyUserSlotList(DailyUserSlotListQuery query);
	
	void saveDailyUserLinkPolicy(LinkPoliciesCommand command);
	
	int selectDailyUserSlotCnt(UserSlotCountQuery query);
	
	void saveDailyUserSlot(DailyUserSlotCommand command);
	
	String selectDailyUserCd(@Param("cmpnyCd") String cmpnyCd);
	
	int selectDailyUserDuplicateCnt(@Param("mblNoHmac") String mblNoHmac);

	void insertDailyQrUser(InsertDailyQrUserCommand command);
	
	void updateDailyUserSlotCurrUserCd(DailyUserSlotUpdCommand command);
}
