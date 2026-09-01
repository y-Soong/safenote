package com.prafta.web.baim.baim03.result;

public record TermsDetailInfoResult(
	String termsId
	, String termsVersion
	, String requiredYn
	, String termsContent
	, String strDate
	, String useYn
	, String termsDesc
	/** 'Y' = TB_TERMS 의 현행(서비스 노출) 버전, 'N' = TB_TERMS_ID_VERSION 의 과거 이력. */
	, String currentYn
){

}