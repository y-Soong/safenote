package com.prafta.web.attd.attd01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SchInfoListQry{
	String siteCd;
	String schNo;
	String schType;
	String useYn;
}
