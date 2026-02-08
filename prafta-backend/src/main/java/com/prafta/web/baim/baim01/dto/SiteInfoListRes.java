package com.prafta.web.baim.baim01.dto;

import java.util.List;

import com.prafta.web.baim.baim01.vo.SiteInfo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SiteInfoListRes {
	List<SiteInfo> siteInfoList;
}
