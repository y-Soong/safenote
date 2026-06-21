package com.prafta.web.user.user01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserInfoListRequest{
	private String userId;
	private String userNm;
	private String useYn;
	private String siteCd;
	private String nodeCd;
	private String incSubNodeYn;
	private String searchMode;
}
