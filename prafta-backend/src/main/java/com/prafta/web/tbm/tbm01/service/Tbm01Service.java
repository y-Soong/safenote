package com.prafta.web.tbm.tbm01.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.prafta.web.tbm.tbm01.dto.TbmEduItemInfoListReq;
import com.prafta.web.tbm.tbm01.dto.TbmEduItemInfoListRes;
import com.prafta.web.tbm.tbm01.dto.TbmEduItemInfoReq;
import com.prafta.web.tbm.tbm01.dto.TbmEduInfoListReq;
import com.prafta.web.tbm.tbm01.dto.TbmEduInfoListRes;
import com.prafta.web.tbm.tbm01.dto.TbmEduInfoReq;

public interface Tbm01Service {
	
	TbmEduInfoListRes selectTbmEduInfo(TbmEduInfoListReq dto, Map<String, Object> tokenInfo);

	TbmEduItemInfoListRes selectTbmEduItemInfo(TbmEduItemInfoListReq dto, Map<String, Object> tokenInfo);
	
	void saveTbmEduInfos(TbmEduInfoReq dto, List<TbmEduItemInfoReq> itemList, Map<String, MultipartFile> fileMap, Map<String, Object> tokenInfo);
	
	void deleteTbmEduItemInfo(List<TbmEduItemInfoReq> dtoList, Map<String, Object> tokenInfo);
	
	void saveTbmEdu(List<TbmEduInfoReq> dtoList, Map<String, Object> tokenInfo);
	
	void deleteTbmEdu(List<TbmEduInfoReq> dtoList, Map<String, Object> tokenInfo);
}
