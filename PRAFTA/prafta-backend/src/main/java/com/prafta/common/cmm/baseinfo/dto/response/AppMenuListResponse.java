package com.prafta.common.cmm.baseinfo.dto.response;

import java.util.List;

import com.prafta.common.cmm.baseinfo.result.AppMenuResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AppMenuListResponse {
	private List<AppMenuResult> appMenuResultList;
}
