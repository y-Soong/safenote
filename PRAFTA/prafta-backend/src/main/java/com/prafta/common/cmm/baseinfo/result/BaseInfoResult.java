package com.prafta.common.cmm.baseinfo.result;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BaseInfoResult {
	String baimValCd;
	String baimValNm;
	String sortIdx;
	String baimValDCd;
	String baimValDNm;
}
