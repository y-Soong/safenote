package com.prafta.web.baim.baim05.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DailyUserSlotListRequest{
	private String siteCd;
	private String slotType;
	private String slotStatus;
	private String useYn;
	private String currUserId;
}
