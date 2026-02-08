package com.prafta.web.chkLst.chkLst03.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InspectItemSubj {
	String InspectItemCd;			// 점검문항코드
	String InspectItemSubj;			// 점검문항명
}
