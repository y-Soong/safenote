package com.prafta.web.chkLst.chkLst03.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.chkLst.chkLst03.application.query.InspectAnswerQuery;
import com.prafta.web.chkLst.chkLst03.application.query.InspectItemSubjQuery;
import com.prafta.web.chkLst.chkLst03.application.query.InspectResultQuery;
import com.prafta.web.chkLst.chkLst03.result.InspectAnswerResult;
import com.prafta.web.chkLst.chkLst03.result.InspectItemHistResult;
import com.prafta.web.chkLst.chkLst03.result.InspectItemSubjResult;
import com.prafta.web.chkLst.chkLst03.result.InspectResult;

@Mapper
public interface ChkLst03Mapper {

	List<InspectResult> selectChkptInspectItemList(InspectResultQuery query);

	List<InspectItemSubjResult> selectInspectItemSubjList(InspectItemSubjQuery query);

	List<InspectAnswerResult> selectInspectAnswerList(InspectAnswerQuery query);

	/** 문항 변경이력 조회(확인서 회색 게이팅용) */
	List<InspectItemHistResult> selectInspectItemHistList(InspectItemSubjQuery query);
}
