package com.prafta.web.chkLst.chkLst03.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InspectResultDetailRequest{
	String workMonth;		// 조회 기준 월
	String siteCd;			// 사업장코드
	String chkLstType;		// 체크리스트 타입
	String chkptCd;			// 점검항목코드
}
