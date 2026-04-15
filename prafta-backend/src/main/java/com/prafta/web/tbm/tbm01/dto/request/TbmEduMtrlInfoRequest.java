package com.prafta.web.tbm.tbm01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TbmEduMtrlInfoRequest{
	private String mtrlCd;
	private String title;
	private String contents;
	private String mtrlType;
	private String useYn;
}
