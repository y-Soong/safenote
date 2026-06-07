package com.prafta.web.tbm.tbm02.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm02.result.SessionAttendanceResult;

import lombok.Builder;
import lombok.Getter;

/** 교육준비 단계 입실자 명단 응답(prafta-051-12). */
@Getter
@Builder
public class SessionAttendanceListResponse {
	private String sessionCd;
	private int totalCount;
	private List<SessionAttendanceResult> attendanceList;
}
