package com.prafta.common.cmm.baseinfo.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SystInfo {
	String systValCd;
	String systValNm;
	String sortIdx;
	String systValDCd;
	String systValDNm;
}
