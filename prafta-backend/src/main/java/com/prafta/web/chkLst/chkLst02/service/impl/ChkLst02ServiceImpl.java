package com.prafta.web.chkLst.chkLst02.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.prafta.web.chkLst.chkLst02.application.command.ChkptInspectItemCommand;
import com.prafta.web.chkLst.chkLst02.application.model.ChkptInspectItemModel;
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemListParam;
import com.prafta.web.chkLst.chkLst02.application.param.ChkptInspectItemParam;
import com.prafta.web.chkLst.chkLst02.application.query.ChkptInspectItemListQuery;
import com.prafta.web.chkLst.chkLst02.dto.response.ChkptInspectItemListResponse;
import com.prafta.web.chkLst.chkLst02.mapper.ChkLst02Mapper;
import com.prafta.web.chkLst.chkLst02.result.ChkptInspectItemResult;
import com.prafta.web.chkLst.chkLst02.service.ChkLst02Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChkLst02ServiceImpl implements ChkLst02Service{
	private final ChkLst02Mapper chkLst02Mapper;
		
	public ChkptInspectItemListResponse selectChkptInspectItemList(ChkptInspectItemListParam param) {
		
		ChkptInspectItemListResponse response = null;
		
		List<ChkptInspectItemResult> chkptInspectItemResultList = chkLst02Mapper.selectChkptInspectItemList(ChkptInspectItemListQuery.from(param));
		
		if(chkptInspectItemResultList != null && chkptInspectItemResultList.size() > 0) {
			response = ChkptInspectItemListResponse.builder().chkptInspectItemResultList(chkptInspectItemResultList).build();
		}
		
		return response;
	}
	
	public void updateChkptInspectItemList(ChkptInspectItemParam param) {
		for(ChkptInspectItemModel model : param.chkptInspectItemModelList()) {
			chkLst02Mapper.mergeChkptInspectItemList(ChkptInspectItemCommand.from(model));
		}
	}
	
	public void deleteChkptInspectItemList(ChkptInspectItemParam param) {
		for(ChkptInspectItemModel model : param.chkptInspectItemModelList()) {
			chkLst02Mapper.updateChkptInspectItemList(ChkptInspectItemCommand.from(model));
		}
	}
}
