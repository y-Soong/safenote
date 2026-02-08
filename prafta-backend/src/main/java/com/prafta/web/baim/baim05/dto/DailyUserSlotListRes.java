package com.prafta.web.baim.baim05.dto;

import java.util.List;

import com.prafta.web.baim.baim05.vo.DailyUserSlotList;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DailyUserSlotListRes{
	List<DailyUserSlotList> dailyUserSlotList;
}
