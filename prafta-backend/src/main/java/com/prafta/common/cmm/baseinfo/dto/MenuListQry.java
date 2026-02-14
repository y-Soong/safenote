package com.prafta.common.cmm.baseinfo.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MenuListQry {
	String userId;
	String menuSrc;
}
