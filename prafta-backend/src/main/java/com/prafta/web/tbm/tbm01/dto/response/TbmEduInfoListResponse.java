package com.prafta.web.tbm.tbm01.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm01.result.TbmEduInfoResult;
import com.prafta.web.tbm.tbm01.result.TbmEduItemInfoResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class TbmEduInfoListResponse{
	List<TbmEduInfoResult> tbmEduInfoResultList;
	
	List<TbmEduItemInfoResult> tbmEduItemInfoResultList;
}
