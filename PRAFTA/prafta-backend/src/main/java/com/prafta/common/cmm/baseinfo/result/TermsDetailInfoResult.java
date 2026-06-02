package com.prafta.common.cmm.baseinfo.result;

public record TermsDetailInfoResult(
	String termsId
	, String termsVersion
	, String requiredYn
	, String termsContent
	, String strDate
	, String useYn
	, String termsDesc
	, String insertNo
	, String insertDate
	, String updateNo
	, String updateDate
) {

}
