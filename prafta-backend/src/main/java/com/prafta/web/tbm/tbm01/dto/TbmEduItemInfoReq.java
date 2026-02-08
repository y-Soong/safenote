package com.prafta.web.tbm.tbm01.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TbmEduItemInfoReq{
	String mtrlItemCd;
	String mtrlCd;
	String mtrlItemType;
	String sortIdx;
	String fileMgmtCd;
	String mtrlDesc;
	String url;
	String useYn;
}
