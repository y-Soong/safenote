package com.prafta.web.chkLst.chkLst04.service;

import com.prafta.web.chkLst.chkLst04.application.param.ChkptTargetListParam;
import com.prafta.web.chkLst.chkLst04.application.param.DefectActionParam;
import com.prafta.web.chkLst.chkLst04.application.param.DefectListParam;
import com.prafta.web.chkLst.chkLst04.application.param.InspectItemListParam;
import com.prafta.web.chkLst.chkLst04.dto.response.ChkptTargetListResponse;
import com.prafta.web.chkLst.chkLst04.dto.response.DefectListResponse;
import com.prafta.web.chkLst.chkLst04.dto.response.InspectItemListResponse;

public interface ChkLst04Service {

	DefectListResponse selectDefectList(DefectListParam param);

	ChkptTargetListResponse selectChkptTargetList(ChkptTargetListParam param);

	InspectItemListResponse selectInspectItemList(InspectItemListParam param);

	void saveDefectAction(DefectActionParam param);
}
