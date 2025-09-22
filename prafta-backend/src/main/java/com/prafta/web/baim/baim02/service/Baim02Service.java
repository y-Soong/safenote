package com.prafta.web.baim.baim02.service;

import java.util.List;
import java.util.Map;

import com.prafta.web.baim.baim02.dto.Baim02;
import com.prafta.web.baim.baim02.dto.Baim02ReqDto;

public interface Baim02Service {
	List<Map<String, Object>> selectCompCmmCodeMList(Baim02ReqDto dto, Map<String, Object> tokenInfo);
	
	List<Baim02> selectCompCmmCodeDList(Baim02ReqDto dto, Map<String, Object> tokenInfo);
	
	void updateCmmCodeDetailInfo(List<Baim02> dtoList, Map<String, Object> tokenInfo);
	
	void deleteCmmCodeDetailInfo(List<Baim02> dtoList, Map<String, Object> tokenInfo);
}
