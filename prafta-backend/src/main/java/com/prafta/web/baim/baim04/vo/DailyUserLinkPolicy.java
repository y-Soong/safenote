package com.prafta.web.baim.baim04.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DailyUserLinkPolicy{
	String chk;
	String cmpnyCd;
	String siteCd;
	String siteNo;
	String siteNm;
	String useYn;
	String dayLimitCnt;
	String joinCd;
}
