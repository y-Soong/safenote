package com.prafta.web.baim.baim05.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DailyUserSlotListQry{
	String siteCd;
	String slotType;
	String slotStatus;
	String useYn;
	String currUserId;
}
