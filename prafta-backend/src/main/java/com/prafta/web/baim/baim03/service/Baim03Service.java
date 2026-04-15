package com.prafta.web.baim.baim03.service;

import com.prafta.web.baim.baim03.application.param.TermsDetailInfoListParam;
import com.prafta.web.baim.baim03.application.param.TermsInfoListParam;
import com.prafta.web.baim.baim03.application.param.TermsInfoParam;
import com.prafta.web.baim.baim03.application.param.TermsListParam;
import com.prafta.web.baim.baim03.dto.response.TermsDetailInfoListResponse;
import com.prafta.web.baim.baim03.dto.response.TermsInfoListResponse;

public interface Baim03Service {
	TermsInfoListResponse selectTermsList(TermsInfoListParam param);
	
	TermsDetailInfoListResponse selectTermsDList(TermsDetailInfoListParam param);
	
//	Baim03 selectTermsInfo(Baim03ReqDto dto, Map<String, Object> tokenInfo);
	
	void updateTermsInfo(TermsInfoParam param);
	
	void deleteCmmCodeDetailInfo(TermsListParam param);
}
