package com.prafta.web.baim.baim06.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CopySiteNodeRequest {
	private String siteCd;
	private String targetSiteCd;
}
