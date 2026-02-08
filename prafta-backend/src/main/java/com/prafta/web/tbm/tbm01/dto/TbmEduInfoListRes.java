package com.prafta.web.tbm.tbm01.dto;

import java.util.List;

import com.prafta.web.tbm.tbm01.vo.TbmEduInfo;
import com.prafta.web.tbm.tbm01.vo.TbmEduItemInfo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class TbmEduInfoListRes{
	List<TbmEduInfo> tbmEduInfoList;
	
	List<TbmEduItemInfo> tbmEduItemInfoList;
}
