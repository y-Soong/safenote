package com.prafta.web.attd.attd01.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SchHist{
	String cmpnyCd;
	String siteCd;
	String schCd;
	String applyDate;

	String fstSchTime;	
	String fstSchBrkMin;

	String secSchTime;
	String secSchBrkMin;
	String useYn;
	
	String userId;
	String userNm;
	String insertDate;
}
