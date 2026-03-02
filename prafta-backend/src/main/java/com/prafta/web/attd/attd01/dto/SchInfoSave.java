package com.prafta.web.attd.attd01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SchInfoSave{
	String cmpnyCd;
	String siteCd;
	String schCd;
	String schNo;
	String schType;
	String applyDate;
	
	String fstSchStrTime;
	String fstSchEndTime;
	String fstSchBrkMin;
	
	String secSchStrTime;
	String secSchEndTime;
	String secSchBrkMin;
	
	String useYn;
}
