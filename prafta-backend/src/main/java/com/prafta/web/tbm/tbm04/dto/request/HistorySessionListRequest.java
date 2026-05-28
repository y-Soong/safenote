package com.prafta.web.tbm.tbm04.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** W-12 TBM 이력 목록 조회 요청. prafta-033-D. */
@Getter
@Setter
@NoArgsConstructor
public class HistorySessionListRequest {
	private String siteCd;
	private String startDate;		// 종료일 시작(YYYY-MM-DD)
	private String endDate;			// 종료일 종료(YYYY-MM-DD)
	private String managerUserCd;
	private String searchKeyword;	// 제목 검색
	private String statusCd;		// 미지정 시 COMPLETED/CANCELLED 위주
	private Integer page;			// 1-base
	private Integer pageSize;
}
