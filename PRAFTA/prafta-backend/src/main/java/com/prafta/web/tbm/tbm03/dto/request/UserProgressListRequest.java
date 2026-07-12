package com.prafta.web.tbm.tbm03.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** T7 사용자별 TBM 진행 집계 목록 조회 요청. */
@Getter
@Setter
@NoArgsConstructor
public class UserProgressListRequest {
	private String siteCd;			// 사업장 추가 필터(권한 스코프는 별도 강제)
	private String startDate;		// 이수일(STATUS_UPDATED_AT) 시작(YYYY-MM-DD)
	private String endDate;			// 이수일 종료(YYYY-MM-DD)
	private String searchKeyword;	// 이름/사번 LIKE
	private Integer page;			// 1-base
	private Integer pageSize;
}
