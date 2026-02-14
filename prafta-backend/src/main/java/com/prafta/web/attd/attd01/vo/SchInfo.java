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
	String fstSchStrTime;
	String fstSchStrEnd;
	String fstSchBrkHour;
	String fstSchBrkYn;
	String secSchStrTime;
	String secSchStrEnd;
	String secSchBrkHour;
	String secSchBrkYn;
	String useYn;
}
