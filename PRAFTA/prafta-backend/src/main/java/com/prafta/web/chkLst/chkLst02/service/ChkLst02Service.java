package com.prafta.web.chkLst.chkLst02.service;

import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemHistListParam;
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemListParam;
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemParam;
import com.prafta.web.chkLst.chkLst02.application.param.CopyChkptInspectItemParam;
import com.prafta.web.chkLst.chkLst02.dto.response.ChkptInspectItemHistListResponse;
import com.prafta.web.chkLst.chkLst02.dto.response.ChkptInspectItemListResponse;

public interface ChkLst02Service {
	ChkptInspectItemListResponse selectChkptInspectItemList(ChkptInspectItemListParam param);

	void updateChkptInspectItemList(ChkptInspectItemParam param);

	void deleteChkptInspectItemList(ChkptInspectItemParam param);

	ChkptInspectItemHistListResponse selectChkptInspectItemHistList(ChkptInspectItemHistListParam param);

	/** PRAFTA-SUBCON-T0-04: 타 사업장 점검문항 가져오기(선택 복사) */
	void copyChkptInspectItemList(CopyChkptInspectItemParam param);
}
