package com.prafta.web.chkLst.chkLst02.service;

import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemListParam;
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemParam;
import com.prafta.web.chkLst.chkLst02.dto.response.ChkptInspectItemListResponse;

public interface ChkLst02Service {
	ChkptInspectItemListResponse selectChkptInspectItemList(ChkptInspectItemListParam param);
	
	void updateChkptInspectItemList(ChkptInspectItemParam param);
	
	void deleteChkptInspectItemList(ChkptInspectItemParam param);
}
