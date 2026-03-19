package com.prafta.web.baim.baim04.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class DailyUserSlotSave{
	String cmpnyCd;
	String siteCd;
	int slotNo;
	String slotType;
	String useYn;
	String currUserId;
	String slotStatus;
}
