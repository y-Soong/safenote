package com.prafta.web.tbm.tbm01.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TbmEduInfoSave{
	String mtrlCd;
	String title;
	String contents;
	String mtrlType;
	String useYn;
}
