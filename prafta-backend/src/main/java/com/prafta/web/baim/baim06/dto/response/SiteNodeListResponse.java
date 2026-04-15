package com.prafta.web.baim.baim06.dto.response;

import java.util.List;

import com.prafta.web.baim.baim06.result.SiteNodeResult;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SiteNodeListResponse{
	List<SiteNodeResult> siteNodeList;
}
