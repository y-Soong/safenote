package com.prafta.web.tbm.tbm01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TbmEduItemInfoSave{
	String mtrlItemCd;
	String mtrlCd;
	String mtrlItemType;
	String sortIdx;
	String fileMgmtCd;
	String mtrlDesc;
	String url;
	String useYn;
}


