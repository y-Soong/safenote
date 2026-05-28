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
	private String siteCd;		// prafta-033-A: 스코프 필터(사업장). 빈 값이면 회사공통+자기사업장
}
