package com.prafta.web.chkLst.chkLst03.service;

import com.prafta.web.chkLst.chkLst03.application.param.InspectResultDetailParam;
import com.prafta.web.chkLst.chkLst03.application.param.InspectResultParam;
import com.prafta.web.chkLst.chkLst03.dto.response.InspectResultDetailResponse;
import com.prafta.web.chkLst.chkLst03.dto.response.InspectResultResponse;

public interface ChkLst03Service {
	InspectResultResponse getChkptInspectItemList(InspectResultParam param);
	
	InspectResultDetailResponse getChkptInspectAnswerList(InspectResultDetailParam param);
}
