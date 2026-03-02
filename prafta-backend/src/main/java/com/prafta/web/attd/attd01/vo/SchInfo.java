package com.prafta.web.attd.attd01.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SchInfo{
	String cmpnyCd;
	String siteCd;
	String schCd;
	String schNo;
	String schType;
	String schTypeNm;
	String baseYn;
	String baseYnNm;
	String applyDate;
	String fstSchStrTime;
	String fstSchEndTime;
	String fstSchTime;
	String fstSchBrkMin;
	String fstSchBrkYn;
	String secSchStrTime;
	String secSchEndTime;
	String secSchTime;
	String secSchBrkMin;
	String secSchBrkYn;
	String useYn;
	String useYnNm;
}
