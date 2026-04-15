package com.prafta.web.tbm.tbm01.service;

import com.prafta.web.tbm.tbm01.application.param.TbmEduInfoListParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduInfoParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduItemInfoListParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduItemParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduMtrlInfoParam;
import com.prafta.web.tbm.tbm01.dto.response.TbmEduInfoListResponse;
import com.prafta.web.tbm.tbm01.dto.response.TbmEduItemInfoListResponse;

public interface Tbm01Service {
	
	TbmEduInfoListResponse selectTbmEduInfo(TbmEduInfoListParam param);

	TbmEduItemInfoListResponse selectTbmEduItemInfo(TbmEduItemInfoListParam param);

	void saveTbmEduInfos(TbmEduInfoParam param);
	
	void deleteTbmEduItemInfo(TbmEduItemParam param);
	
	void saveTbmEdu(TbmEduMtrlInfoParam param);
	
	void deleteTbmEdu(TbmEduMtrlInfoParam param);
}
