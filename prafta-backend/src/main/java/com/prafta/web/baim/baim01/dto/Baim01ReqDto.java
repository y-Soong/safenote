package com.prafta.web.baim.baim01.dto;

import com.prafta.common.dto.cmmReqDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Baim01ReqDto extends cmmReqDto{
	private String cmpnyCd;
	private String siteCd;
	private String siteNo;
	private String siteNm;
	private String useYn;
}
