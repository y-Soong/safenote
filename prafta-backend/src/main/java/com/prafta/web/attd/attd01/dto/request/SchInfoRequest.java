package com.prafta.web.attd.attd01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SchInfoRequest{
	private String cmpnyCd;
	private String siteCd;
	private String schCd;
	private String schNo;
	private String schType;
	private String applyDate;
	
	private String fstSchStrTime;
	private String fstSchEndTime;
	private String fstSchBrkMin;
	
	private String secSchStrTime;
	private String secSchEndTime;
	private String secSchBrkMin;
	
	private String useYn;
}
