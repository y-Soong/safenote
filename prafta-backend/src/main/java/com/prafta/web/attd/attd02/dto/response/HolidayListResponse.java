package com.prafta.web.attd.attd02.dto.response;

import java.util.List;

import com.prafta.web.attd.attd02.result.HolidayResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HolidayListResponse{
	List<HolidayResult> holidayResultList;
}
