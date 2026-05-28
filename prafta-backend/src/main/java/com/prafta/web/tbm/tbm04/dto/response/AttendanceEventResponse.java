package com.prafta.web.tbm.tbm04.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm04.result.AttendanceEventResult;

import lombok.Builder;
import lombok.Getter;

/** W-13 이벤트 타임라인 응답(시간순, 페이징). */
@Getter
@Builder
public class AttendanceEventResponse {
	private List<AttendanceEventResult> eventList;
	private int totalCount;
	private int page;
	private int pageSize;
}
