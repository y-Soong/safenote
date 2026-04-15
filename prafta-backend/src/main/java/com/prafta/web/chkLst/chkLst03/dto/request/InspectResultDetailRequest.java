package com.prafta.web.chkLst.chkLst03.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InspectResultDetailRequest{
	private String workMonth;		// 조회 기준 월
	private String siteCd;			// 사업장코드
	private String chkLstType;		// 체크리스트 타입
	private String chkptCd;			// 점검항목코드
}
