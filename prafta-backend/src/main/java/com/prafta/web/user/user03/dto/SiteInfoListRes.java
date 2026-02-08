package com.prafta.web.user.user03.dto;

import java.util.List;

import com.prafta.web.user.user03.vo.SiteInfo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SiteInfoListRes {
	List<SiteInfo> siteInfoList;
}
