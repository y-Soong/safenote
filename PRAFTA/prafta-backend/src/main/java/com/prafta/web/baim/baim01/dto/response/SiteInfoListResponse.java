package com.prafta.web.baim.baim01.dto.response;

import java.util.List;

import com.prafta.web.baim.baim01.result.SiteInfoResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SiteInfoListResponse {
	List<SiteInfoResult> siteInfoList;
}
