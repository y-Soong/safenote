package com.prafta.web.attd.attd05.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserWorkPlansRequest {
	private String siteCd;
	private String nodeCd;
	private String incSubNodeYn;
	private String workYm;
	private String userNm;
}
