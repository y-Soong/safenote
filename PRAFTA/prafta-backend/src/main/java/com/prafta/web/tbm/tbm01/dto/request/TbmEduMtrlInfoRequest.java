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
	private String siteCd;		// prafta-033-A: 스코프(목록 그리드 인라인 저장 시 전달). 미전달이면 기존 값 보존
}
