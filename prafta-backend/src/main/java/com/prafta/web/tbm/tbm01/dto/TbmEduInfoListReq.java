package com.prafta.web.tbm.tbm01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TbmEduInfoListReq{
	String mtrlCd;
	String mtrlType;
	String title;
	String useYn;
}
