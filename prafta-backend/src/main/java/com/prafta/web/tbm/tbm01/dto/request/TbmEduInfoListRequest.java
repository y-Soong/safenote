package com.prafta.web.tbm.tbm01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TbmEduInfoListRequest{
	private String mtrlCd;
	private String mtrlType;
	private String title;
	private String useYn;
}
