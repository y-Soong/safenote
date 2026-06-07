package com.prafta.web.chkLst.chkLst04.dto.response;

import java.util.List;

import com.prafta.web.chkLst.chkLst04.result.ChkptTargetResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChkptTargetListResponse {
	private List<ChkptTargetResult> chkptTargetResultList;
}
