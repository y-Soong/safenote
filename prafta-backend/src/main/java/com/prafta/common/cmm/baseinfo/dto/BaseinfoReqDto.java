package com.prafta.common.cmm.baseinfo.dto;

import java.util.List;

import com.prafta.common.dto.cmmReqDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseinfoReqDto extends cmmReqDto{
	private List<String> systCodeList;
	private List<String> baseCodeList;
	private String code;
	private String code_d;
	private String name_d;
	
	private String cmpnyCd;
	private String userId;
	private String mblNo;
	private String certNo;
	private String smsId;
	private String siteNo;
	private String siteNm;
	private String menuSrc;
	
	private String seqKey;
}
