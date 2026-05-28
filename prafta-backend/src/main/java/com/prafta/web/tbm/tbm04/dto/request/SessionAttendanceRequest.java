package com.prafta.web.tbm.tbm04.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** W-13 세션 출결 명단 조회 요청. prafta-033-D. */
@Getter
@Setter
@NoArgsConstructor
public class SessionAttendanceRequest {
	private String sessionCd;			// 필수
	private String userTypeCd;			// REGULAR / DAILY (미지정 시 전체)
	private String completionStatusCd;	// COMPLETED / NOT_COMPLETED (미지정 시 전체)
	private Boolean includeEventSummary;	// default true
}
