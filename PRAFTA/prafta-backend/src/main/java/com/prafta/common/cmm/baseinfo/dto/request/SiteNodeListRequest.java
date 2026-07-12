package com.prafta.common.cmm.baseinfo.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SiteNodeListRequest {
	private String cmpnyCd;
	private String siteCd;
	private String nodeCd;
	private String nodeType;
	private String nodeNm;
	private String parentNodeNm;
	// PRAFTA-WEB_002-T1-02(1.3-3/1.4-1): true 면 담당(정/부) 미지정 노드도 조회 결과에 포함한다.
	//   미전달(null) 시 false 로 간주(현행 동작 = 담당 지정 노드만).
	private Boolean includeNoAdmin;
}
