package com.prafta.common.cmm.baseinfo.dto;

import java.util.List;

import com.prafta.common.dto.cmmReqDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseinfoCmmReq extends cmmReqDto{
	private List<String> systCodeList;
	private List<String> baseCodeList;
	private String code;
	private String code_d;
	private String name_d;
	
	private String cmpnyCd;
	private String userId;
	private String userNm;
	private String userPw;
	private String mblNo;
	private String dupChkYn;		/* 휴대폰번호 중복 체크 필요 여부 Y/N */
	private String certNo;
	private String smsId;
	private String siteNo;
	private String siteNm;
	private String menuSrc;
	
	private String seqKey;
	
	private String termsId;
}
