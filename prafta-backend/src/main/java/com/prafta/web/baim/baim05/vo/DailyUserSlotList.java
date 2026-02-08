package com.prafta.web.baim.baim05.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DailyUserSlotList{
	String chk;
	String cmpnyCd;
	String siteCd;
	String siteNm;
	String slotNo;
	String slotType;
	String fixedYn;
	String useYn;
	String currUserId;
	String currUserNm;
	String slotStatus;
}
