package com.prafta.web.baim.baim05.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.baim.baim05.application.command.ClearSlotCommand;
import com.prafta.web.baim.baim05.application.command.DailyUserSlotCommand;
import com.prafta.web.baim.baim05.application.command.DailyUserSlotUpdCommand;
import com.prafta.web.baim.baim05.application.command.InsertDailyQrUserCommand;
import com.prafta.web.baim.baim05.application.command.LinkPoliciesCommand;
import com.prafta.web.baim.baim05.application.command.SetSlotFixedCommand;
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

	/** 사업장 접근 권한 확인(TB_USER_SITE_AUTH 매핑, USE_YN='Y'). 1 이상이면 접근 가능. */
	int countUserSiteAuth(@Param("cmpnyCd") String cmpnyCd, @Param("userCd") String userCd, @Param("siteCd") String siteCd);

	/** 슬롯 점유자(CURR_USER_CD) 재조회. 비점유 또는 미존재면 null. */
	String selectSlotCurrUserCd(@Param("cmpnyCd") String cmpnyCd, @Param("siteCd") String siteCd, @Param("slotNo") String slotNo);

	/** 슬롯 비우기: CURR_USER_CD=NULL, SLOT_STATUS='01', FIXED_YN='N'. */
	void updateClearSlot(ClearSlotCommand command);

	/** 비우기 - 점유자 TB_DAILY_USER 비활성(USE_YN='N', ACCOUNT_STATUS='05'). */
	void deactivateDailyUser(ClearSlotCommand command);

	/** 비우기 - 통합형 TB_USER 비활성(EMPLOYMENT_TYPE='DAILY' 가드). */
	void deactivateTbUser(ClearSlotCommand command);

	/** 점유 유지/해지: 점유 슬롯(SLOT_STATUS='02')의 FIXED_YN 토글. */
	void updateSlotFixed(SetSlotFixedCommand command);
}
