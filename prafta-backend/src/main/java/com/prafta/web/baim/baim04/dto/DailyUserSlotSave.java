package com.prafta.web.baim.baim04.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class DailyUserSlotSave{
	String cmpnyCd;
	String siteCd;
	int slotNo;
	String slotType;
	String useYn;
	String currUserId;
	String slotStatus;
}
