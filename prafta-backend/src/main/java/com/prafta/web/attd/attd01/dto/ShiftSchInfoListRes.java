package com.prafta.web.attd.attd01.dto;

import java.util.List;

import com.prafta.web.attd.attd01.vo.ShiftSchInfo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ShiftSchInfoListRes{
	List<ShiftSchInfo> shiftSchInfoList;
}
