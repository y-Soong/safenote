package com.prafta.web.baim.baim06.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CopySiteNodeSave {
	String siteCd;
	String targetSiteCd;
}
