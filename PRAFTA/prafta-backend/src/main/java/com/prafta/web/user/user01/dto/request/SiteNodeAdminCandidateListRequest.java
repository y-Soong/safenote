package com.prafta.web.user.user01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SiteNodeAdminCandidateListRequest{
	
	private String userId;
	private String userNm;
	private String siteCd;
	private String nodeCd;
}
