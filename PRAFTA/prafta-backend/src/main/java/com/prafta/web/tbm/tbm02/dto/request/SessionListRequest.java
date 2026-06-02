package com.prafta.web.tbm.tbm02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** W-04 세션 목록 조회 요청. */
@Getter
@Setter
@NoArgsConstructor
public class SessionListRequest {
	private String siteCd;
	private String statusCd;
	private String startDate;		// 개설일 시작(YYYY-MM-DD)
	private String endDate;			// 개설일 종료(YYYY-MM-DD)
	private String managerUserCd;
	private String searchKeyword;	// 제목 검색
	private Integer page;			// 1-base
	private Integer pageSize;
}
