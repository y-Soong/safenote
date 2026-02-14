package com.prafta.web.baim.baim03.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TermsInfo{
	String termsId;
	String termsNm;
	String termsVersion;
}
