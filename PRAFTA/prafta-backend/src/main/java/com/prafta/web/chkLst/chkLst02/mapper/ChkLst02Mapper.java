package com.prafta.web.chkLst.chkLst02.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.prafta.web.chkLst.chkLst02.application.command.ChkptInspectItemCommand;
import com.prafta.web.chkLst.chkLst02.application.query.ChkptInspectItemListQuery;
import com.prafta.web.chkLst.chkLst02.result.ChkptInspectItemResult;

@Mapper
public interface ChkLst02Mapper {
	List<ChkptInspectItemResult> selectChkptInspectItemList(ChkptInspectItemListQuery query);
	
	void mergeChkptInspectItemList(ChkptInspectItemCommand command);
	
	void updateChkptInspectItemList(ChkptInspectItemCommand command);
}
