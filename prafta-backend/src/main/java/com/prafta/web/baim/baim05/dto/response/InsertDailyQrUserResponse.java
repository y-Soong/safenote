package com.prafta.web.baim.baim05.dto.response;

import com.prafta.web.baim.baim05.result.DailyUserQrInfoResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InsertDailyQrUserResponse{
	DailyUserQrInfoResult dailyUserQrInfoResult;
}
