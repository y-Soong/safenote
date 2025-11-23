package com.prafta.app.chkLst.chkLst01.service;

import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.app.chkLst.chkLst01.dto.ChecklistInfoReq;
import com.prafta.app.chkLst.chkLst01.dto.ChecklistInfoRes;
import com.prafta.app.chkLst.chkLst01.dto.SaveInspectResultReq;

public interface AppChkLst01Service {
	ChecklistInfoRes selectChkLstInfo(ChecklistInfoReq request, Map<String, Object> tokenInfo);
	
	void saveInspectResult(SaveInspectResultReq request, Map<String, MultipartFile> file, Map<String, Object> tokenInfo);
}
