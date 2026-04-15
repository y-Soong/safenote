package com.prafta.web.user.user03.dto.response;

import java.util.List;

import com.prafta.web.user.user03.result.SiteInfoResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SiteInfoListResponse {
	List<SiteInfoResult> siteInfoList;
}
