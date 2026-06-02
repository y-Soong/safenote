package com.prafta.web.tbm.tbm04.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** W-13 출결 단건 이벤트 타임라인 조회 요청. prafta-033-D. */
@Getter
@Setter
@NoArgsConstructor
public class AttendanceEventRequest {
	private String attendanceCd;	// 필수
	private Integer page;			// 1-base (대량 이벤트 페이징)
	private Integer pageSize;
}
