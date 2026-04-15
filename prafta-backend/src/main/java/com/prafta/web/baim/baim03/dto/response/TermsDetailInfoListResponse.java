package com.prafta.web.baim.baim03.dto.response;

import java.util.List;

import com.prafta.web.baim.baim03.result.TermsDetailInfoResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TermsDetailInfoListResponse{
	List<TermsDetailInfoResult> termsDetailInfoList;
}
