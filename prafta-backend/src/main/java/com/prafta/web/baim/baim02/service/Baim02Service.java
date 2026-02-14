package com.prafta.web.baim.baim02.service;

import java.util.List;
import java.util.Map;

import com.prafta.web.baim.baim02.dto.CompCmmCodeDListReq;
import com.prafta.web.baim.baim02.dto.CompCmmCodeDListRes;
import com.prafta.web.baim.baim02.dto.CompCmmCodeDReq;
import com.prafta.web.baim.baim02.dto.CompCmmCodeMListReq;
import com.prafta.web.baim.baim02.dto.CompCmmCodeMListRes;

public interface Baim02Service {
	CompCmmCodeMListRes selectCompCmmCodeMList(CompCmmCodeMListReq dto, Map<String, Object> tokenInfo);
	
	CompCmmCodeDListRes selectCompCmmCodeDList(CompCmmCodeDListReq dto, Map<String, Object> tokenInfo);
	
	void updateCmmCodeDetailInfo(List<CompCmmCodeDReq> dtoList, Map<String, Object> tokenInfo);
	
	void deleteCmmCodeDetailInfo(List<CompCmmCodeDReq> dtoList, Map<String, Object> tokenInfo);
}
