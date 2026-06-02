package com.prafta.web.tbm.tbm04.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** W-15 사용자별 TBM 이수 이력 조회 요청(정규직/일용직 공용). prafta-033-D. */
@Getter
@Setter
@NoArgsConstructor
public class UserAttendanceRequest {
	private String userCd;				// 필수(대상 사용자)
	private String startDate;			// 세션 종료일 시작(YYYY-MM-DD)
	private String endDate;				// 세션 종료일 종료(YYYY-MM-DD)
	private String completionStatusCd;	// COMPLETED / NOT_COMPLETED (미지정 시 전체)
	private Integer page;				// 1-base
	private Integer pageSize;
}
