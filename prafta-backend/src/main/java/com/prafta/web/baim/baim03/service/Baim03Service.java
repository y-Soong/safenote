package com.prafta.web.baim.baim03.service;

import java.util.List;
import java.util.Map;

import com.prafta.web.baim.baim03.dto.Baim03;
import com.prafta.web.baim.baim03.dto.Baim03ReqDto;

public interface Baim03Service {
	List<Baim03> selectTermsList(Baim03ReqDto dto, Map<String, Object> tokenInfo);
	
	List<Baim03> selectTermsDList(Baim03ReqDto dto, Map<String, Object> tokenInfo);
	
	Baim03 selectTermsInfo(Baim03ReqDto dto, Map<String, Object> tokenInfo);
	
	void updateTermsInfo(Baim03 dto, Map<String, Object> tokenInfo);
	
	void deleteCmmCodeDetailInfo(List<Baim03> dtoList, Map<String, Object> tokenInfo);
}
