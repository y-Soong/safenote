package com.prafta.web.baim.baim02.service;

import com.prafta.web.baim.baim02.application.param.CompCmmCodeDListParam;
import com.prafta.web.baim.baim02.application.param.CompCmmCodeDParam;
import com.prafta.web.baim.baim02.application.param.CompCmmCodeMListParam;
import com.prafta.web.baim.baim02.dto.response.CompCmmCodeDListResponse;
import com.prafta.web.baim.baim02.dto.response.CompCmmCodeMListResponse;

public interface Baim02Service {
	CompCmmCodeMListResponse selectCompCmmCodeMList(CompCmmCodeMListParam param);
	
	CompCmmCodeDListResponse selectCompCmmCodeDList(CompCmmCodeDListParam param);
	
	void updateCmmCodeDetailInfo(CompCmmCodeDParam param);
	
	void deleteCmmCodeDetailInfo(CompCmmCodeDParam param);
}
