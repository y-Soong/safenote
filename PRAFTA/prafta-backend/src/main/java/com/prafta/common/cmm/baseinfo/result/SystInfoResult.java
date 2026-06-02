package com.prafta.common.cmm.baseinfo.result;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SystInfoResult {
	String systValCd;
	String systValNm;
	String sortIdx;
	String systValDCd;
	String systValDNm;
}
