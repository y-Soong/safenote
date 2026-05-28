package com.prafta.web.tbm.tbm02.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm02.result.SiteOptionResult;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SiteOptionResponse {
	private List<SiteOptionResult> siteList;
}
