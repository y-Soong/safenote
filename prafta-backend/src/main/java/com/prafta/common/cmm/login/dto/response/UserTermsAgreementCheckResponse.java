package com.prafta.common.cmm.login.dto.response;

import java.util.List;

import com.prafta.common.cmm.login.result.UserTermsAgreementCheckResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserTermsAgreementCheckResponse{
	List<UserTermsAgreementCheckResult> userTermsAgreementCheckList;
}
