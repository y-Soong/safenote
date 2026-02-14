package com.prafta.web.chkLst.chkLst01.service;

import java.util.List;
import java.util.Map;

import com.prafta.web.chkLst.chkLst01.dto.ChkLst01;
import com.prafta.web.chkLst.chkLst01.dto.ChkLst01ReqDto;

public interface ChkLst01Service {
	List<ChkLst01> selectChkptList(ChkLst01ReqDto dto, Map<String, Object> tokenInfo);
	
	void updateChkptList(List<ChkLst01> dtoList, Map<String, Object> tokenInfo);
	
	void deleteChkptList(List<ChkLst01> dtoList, Map<String, Object> tokenInfo);
}
