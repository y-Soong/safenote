package com.prafta.web.attd.attd01.dto;

import lombok.Data;

@Data
public class SchInfoReq{
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
