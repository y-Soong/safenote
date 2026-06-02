package com.prafta.web.attd.attd01.dto.response;

import java.util.List;

import com.prafta.web.attd.attd01.result.ShiftSchInfoResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShiftSchInfoListResponse{
	private List<ShiftSchInfoResult> shiftSchInfoResultList;
}
