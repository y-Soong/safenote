package com.prafta.web.chkLst.chkLst03.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.prafta.web.chkLst.chkLst03.dto.InspectAnswerQuery;
import com.prafta.web.chkLst.chkLst03.dto.InspectItemSubjQuery;
import com.prafta.web.chkLst.chkLst03.dto.InspectResultQuery;
import com.prafta.web.chkLst.chkLst03.vo.InspectAnswer;
import com.prafta.web.chkLst.chkLst03.vo.InspectItemSubj;
import com.prafta.web.chkLst.chkLst03.vo.InspectResult;

@Mapper
public interface ChkLst03Mapper {
	
	List<InspectResult> selectChkptInspectItemList(@Param(value = "param") InspectResultQuery dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	List<InspectItemSubj> selectInspectItemSubjList(@Param(value = "param") InspectItemSubjQuery dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
	List<InspectAnswer> selectInspectAnswerList(@Param(value = "param") InspectAnswerQuery dto, @Param(value = "token") Map<String, Object> tokenInfo);
	
//	List<ChkLst02> selectChkptInspectItemList(@Param(value = "param") ChkLst02ReqDto dto, @Param(value = "token") Map<String, Object> tokenInfo);
//	
//	void mergeChkptInspectItemList(@Param(value = "param") ChkLst02 dto, @Param(value = "token") Map<String, Object> tokenInfo);
//	
//	void updateChkptInspectItemList(@Param(value = "param") ChkLst02 dto, @Param(value = "token") Map<String, Object> tokenInfo);
}
