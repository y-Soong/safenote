package com.prafta.web.baim.baim01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SiteInfoListQry {
	String siteCd;
	String siteNo;
	String siteNm;
	String useYn;
}
