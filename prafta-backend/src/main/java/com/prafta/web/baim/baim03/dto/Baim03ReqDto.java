package com.prafta.web.baim.baim03.dto;

import com.prafta.common.dto.cmmReqDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Baim03ReqDto extends cmmReqDto{
	private String sr_termsId;
	private String sr_termsNm;
	private String sr_termsVersion;
}
