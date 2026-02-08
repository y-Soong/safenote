package com.prafta.common.cmm.baseinfo.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BaseInfo {
	String baimValCd;
	String baimValNm;
	String sortIdx;
	String baimValDCd;
	String baimValDNm;
}
