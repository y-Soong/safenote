package com.prafta.common.cmm.baseinfo.dto.response;

import com.prafta.common.cmm.baseinfo.result.TermsDetailInfoResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TermsDetailInfoResponse {
	private TermsDetailInfoResult termsDetailInfoResult;
}
