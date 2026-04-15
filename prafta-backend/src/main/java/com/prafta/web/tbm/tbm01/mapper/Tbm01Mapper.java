package com.prafta.web.tbm.tbm01.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.tbm.tbm01.application.command.TbmEduInfoCommand;
import com.prafta.web.tbm.tbm01.application.command.TbmEduItemCommand;
import com.prafta.web.tbm.tbm01.application.command.TbmEduItemInfoCommand;
import com.prafta.web.tbm.tbm01.application.query.TbmEduInfoListQuery;
import com.prafta.web.tbm.tbm01.application.query.TbmEduItemInfoListQuery;
import com.prafta.web.tbm.tbm01.result.TbmEduInfoResult;
import com.prafta.web.tbm.tbm01.result.TbmEduItemInfoResult;

@Mapper
public interface Tbm01Mapper {
	
	List<TbmEduInfoResult> selectTbmEduInfo(TbmEduInfoListQuery query);
	
	List<TbmEduItemInfoResult> selectTbmEduItemInfo(TbmEduItemInfoListQuery query);
	
	String selectMtrlCd(@Param(value = "gvCmpnyCd") String gvCmpnyCd);
	
	String selectMtrlItemCd(@Param(value = "gvCmpnyCd") String gvCmpnyCd);
	
	void mergeTbmEduInfo(TbmEduInfoCommand command);
	
	void mergeTbmEduItemInfo(TbmEduItemInfoCommand command);
	
	void deleteTbmEduItemInfo(TbmEduItemCommand command);
	
	void deleteTbmEduInfo(TbmEduInfoCommand command);
}
