package com.prafta.common.cmm.baseinfo.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserInfoListRequest {
	private String userId;
	private String userNm;
	private String useYn;
	private String siteCd;
	private String nodeCd;
	private String searchMode;
}
