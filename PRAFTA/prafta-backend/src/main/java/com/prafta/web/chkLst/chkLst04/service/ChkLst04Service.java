package com.prafta.web.chkLst.chkLst04.service;

import com.prafta.web.chkLst.chkLst04.application.param.ChkptTargetListParam;
import com.prafta.web.chkLst.chkLst04.application.param.DefectActionParam;
import com.prafta.web.chkLst.chkLst04.application.param.DefectListParam;
import com.prafta.web.chkLst.chkLst04.application.param.InspectItemListParam;
import com.prafta.web.chkLst.chkLst04.dto.response.ChkptTargetListResponse;
import com.prafta.web.chkLst.chkLst04.dto.response.DefectListResponse;
import com.prafta.web.chkLst.chkLst04.dto.response.InspectItemListResponse;

import org.springframework.web.multipart.MultipartFile;

public interface ChkLst04Service {

	DefectListResponse selectDefectList(DefectListParam param);

	ChkptTargetListResponse selectChkptTargetList(ChkptTargetListParam param);

	InspectItemListResponse selectInspectItemList(InspectItemListParam param);

	/** 조치 upsert(후행 덮어쓰기) — 조치 사진(선택) 저장 + 체인 전 티어 전파. */
	void saveDefectAction(DefectActionParam param, MultipartFile file);
}
