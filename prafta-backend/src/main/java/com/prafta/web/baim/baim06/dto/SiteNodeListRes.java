package com.prafta.web.baim.baim06.dto;

import java.util.List;

import com.prafta.web.baim.baim06.vo.SiteNode;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SiteNodeListRes{
	List<SiteNode> siteNodeList;
}
