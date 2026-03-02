package com.prafta.web.attd.attd01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SchInfoHistQry{
	String cmpnyCd;
	String siteCd;
	String schCd;
}
