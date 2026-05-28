package com.prafta.web.tbm.tbm01.service;

import com.prafta.web.tbm.tbm01.application.param.TbmEduDetailParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduInfoListParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduInfoParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduItemInfoListParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduItemParam;
import com.prafta.web.tbm.tbm01.application.param.TbmEduMtrlInfoParam;
import com.prafta.web.tbm.tbm01.dto.response.TbmEduDetailResponse;
import com.prafta.web.tbm.tbm01.dto.response.TbmEduInfoListResponse;
import com.prafta.web.tbm.tbm01.dto.response.TbmEduItemInfoListResponse;

public interface Tbm01Service {

	TbmEduInfoListResponse selectTbmEduInfo(TbmEduInfoListParam param);

	TbmEduItemInfoListResponse selectTbmEduItemInfo(TbmEduItemInfoListParam param);

	/** prafta-033-A: W-03 상세(묶음+세부항목+사용 TBM 이력) */
	TbmEduDetailResponse selectTbmEduDetail(TbmEduDetailParam param);

	void saveTbmEduInfos(TbmEduInfoParam param);

	void deleteTbmEduItemInfo(TbmEduItemParam param);

	void saveTbmEdu(TbmEduMtrlInfoParam param);

	void deleteTbmEdu(TbmEduMtrlInfoParam param);
}
