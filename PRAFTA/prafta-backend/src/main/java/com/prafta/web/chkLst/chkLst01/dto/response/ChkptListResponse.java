package com.prafta.web.chkLst.chkLst01.dto.response;

import java.util.List;

import com.prafta.web.chkLst.chkLst01.result.ChkptResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChkptListResponse{
	private List<ChkptResult> chkptResultList;
}
