package com.prafta.web.baim.baim03.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TermsInfoRequest{
	private String termsId;
	private String termsNm;
	private String requiredYn;
	private String termsContent;
	private String strDate;
	private String useYn;
	private String termsDesc;
}
