package com.prafta.web.tbm.tbm03.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** T7 사용자별 세션 이수 이력 드릴다운 조회 요청. */
@Getter
@Setter
@NoArgsConstructor
public class UserProgressDetailRequest {
	private String userCd;				// 필수(대상 사용자)
	private String userTypeCd;			// REGULAR / DAILY (미지정 시 REGULAR)
	private String startDate;			// 이수일(STATUS_UPDATED_AT) 시작(YYYY-MM-DD)
	private String endDate;				// 이수일 종료(YYYY-MM-DD)
	private String completionStatusCd;	// COMPLETED / NOT_COMPLETED (미지정 시 전체)
	private Integer page;				// 1-base
	private Integer pageSize;
}
