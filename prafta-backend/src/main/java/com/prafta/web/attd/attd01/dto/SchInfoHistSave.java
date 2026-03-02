package com.prafta.web.attd.attd01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SchInfoHistSave{
	String cmpnyCd;
	String siteCd;
	int histIdx;
	String schCd;
	String applyDate;
	
	String fstSchStrTime;
	String fstSchEndTime;
	String fstSchBrkMin;
	
	String secSchStrTime;
	String secSchEndTime;
	String secSchBrkMin;
	
	String useYn;
}
