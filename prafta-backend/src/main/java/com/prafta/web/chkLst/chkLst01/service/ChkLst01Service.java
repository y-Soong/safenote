package com.prafta.web.chkLst.chkLst01.service;

import com.prafta.web.chkLst.chkLst01.application.param.ChkptInfoParam;
import com.prafta.web.chkLst.chkLst01.application.param.ChkptListParam;
import com.prafta.web.chkLst.chkLst01.dto.response.ChkptListResponse;

public interface ChkLst01Service {
	ChkptListResponse selectChkptList(ChkptListParam param);
	
	void updateChkptList(ChkptInfoParam param);
	
	void deleteChkptList(ChkptInfoParam param);
}
