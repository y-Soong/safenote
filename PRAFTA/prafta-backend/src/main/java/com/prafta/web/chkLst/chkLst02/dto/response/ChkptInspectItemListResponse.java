package com.prafta.web.chkLst.chkLst02.dto.response;

import java.util.List;

import com.prafta.web.chkLst.chkLst02.result.ChkptInspectItemResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChkptInspectItemListResponse{
	private List<ChkptInspectItemResult> chkptInspectItemResultList;
}
