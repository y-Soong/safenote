package com.prafta.web.tbm.tbm04.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm04.result.UserAttendanceResult;
import com.prafta.web.tbm.tbm04.result.UserInfoResult;

import lombok.Builder;
import lombok.Getter;

/** W-15 사용자별 이수 이력 응답(사용자 + 이력 목록 + 통계). */
@Getter
@Builder
public class UserAttendanceResponse {
	private UserInfoResult user;
	private List<UserAttendanceResult> attendances;
	private int totalCount;
	private int page;
	private int pageSize;
	private Summary summary;

	@Getter
	@Builder
	public static class Summary {
		private int totalCount;			// 총 참여
		private int completedCount;		// 이수
		private int notCompletedCount;	// 미이수
		private double completionRate;	// 이수율(%) — 이수/총참여 * 100
		private Integer avgDurationMin;	// 평균 참여시간(분, 산출 불가 시 NULL)
	}
}
