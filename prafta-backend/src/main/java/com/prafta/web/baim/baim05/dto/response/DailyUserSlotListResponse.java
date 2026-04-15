package com.prafta.web.baim.baim05.dto.response;

import java.util.List;

import com.prafta.web.baim.baim05.result.DailyUserSlotListResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DailyUserSlotListResponse{
	List<DailyUserSlotListResult> dailyUserSlotList;
}
