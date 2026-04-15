package com.prafta.web.chkLst.chkLst03.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.chkLst.chkLst03.application.query.InspectAnswerQuery;
import com.prafta.web.chkLst.chkLst03.application.query.InspectItemSubjQuery;
import com.prafta.web.chkLst.chkLst03.application.query.InspectResultQuery;
import com.prafta.web.chkLst.chkLst03.result.InspectAnswerResult;
import com.prafta.web.chkLst.chkLst03.result.InspectItemSubjResult;
import com.prafta.web.chkLst.chkLst03.result.InspectResult;

@Mapper
public interface ChkLst03Mapper {
	
	List<InspectResult> selectChkptInspectItemList(InspectResultQuery query);
	
	List<InspectItemSubjResult> selectInspectItemSubjList(InspectItemSubjQuery query);
	
	List<InspectAnswerResult> selectInspectAnswerList(InspectAnswerQuery query);
}
