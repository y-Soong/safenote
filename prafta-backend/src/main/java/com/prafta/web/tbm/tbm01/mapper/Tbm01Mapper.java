package com.prafta.web.tbm.tbm01.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.tbm.tbm01.dto.TbmEduItemInfoListQry;
import com.prafta.web.tbm.tbm01.dto.TbmEduInfoListQry;
import com.prafta.web.tbm.tbm01.dto.TbmEduInfoSave;
import com.prafta.web.tbm.tbm01.dto.TbmEduItemInfoSave;
import com.prafta.web.tbm.tbm01.vo.TbmEduInfo;
import com.prafta.web.tbm.tbm01.vo.TbmEduItemInfo;

@Mapper
public interface Tbm01Mapper {
	
	List<TbmEduInfo> selectTbmEduInfo(@Param(value = "param") TbmEduInfoListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	List<TbmEduItemInfo> selectTbmEduItemInfo(@Param(value = "param") TbmEduItemInfoListQry dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	String selectMtrlCd(@Param(value = "token") Map<String, Object> tokenInfo);
	
	String selectMtrlItemCd(@Param(value = "token") Map<String, Object> tokenInfo);
	
	void mergeTbmEduInfo(@Param(value = "param") TbmEduInfoSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void mergeTbmEduItemInfo(@Param(value = "param") TbmEduItemInfoSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void deleteTbmEduItemInfo(@Param(value = "param") TbmEduItemInfoSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	void deleteTbmEduInfo(@Param(value = "param") TbmEduInfoSave dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
