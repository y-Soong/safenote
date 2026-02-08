package com.prafta.common.cmm.baseinfo.dto;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SystInfoListQuery{
	List<String> systCodeList;
}
