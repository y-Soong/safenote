package com.prafta.common.cmm.login.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RequiredTermsInfo{
	String termsId;
	String termsNm;
	String termsVersion;
}
