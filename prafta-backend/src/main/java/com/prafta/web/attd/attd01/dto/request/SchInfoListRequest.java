package com.prafta.web.attd.attd01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SchInfoListRequest{
	private String siteCd;
	private String schNo;
	private String schType;
	private String useYn;
}
