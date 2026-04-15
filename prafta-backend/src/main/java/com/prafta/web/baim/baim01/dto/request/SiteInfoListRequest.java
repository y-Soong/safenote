package com.prafta.web.baim.baim01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SiteInfoListRequest{
	private String cmpnyCd;
	private String siteCd;
	private String siteNo;
	private String siteNm;
	private String useYn;
}
