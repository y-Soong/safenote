package com.prafta.web.chkLst.chkLst01.dto;

import com.prafta.common.dto.cmmReqDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChkLst01ReqDto extends cmmReqDto{
	private String sr_siteCd;
	private String sr_chkptNm;
	private String sr_chkLstType;
	private String sr_useYn;
}
