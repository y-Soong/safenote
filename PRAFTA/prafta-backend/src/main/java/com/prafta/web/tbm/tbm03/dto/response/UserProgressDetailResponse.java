package com.prafta.web.tbm.tbm03.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm03.result.UserProgressDetailResult;
import com.prafta.web.tbm.tbm03.result.UserProgressUserResult;

import lombok.Builder;
import lombok.Getter;

/** T7 드릴다운 응답(사용자 헤더 + 요약 + 세션 이력 목록 + 페이징). */
@Getter
@Builder
public class UserProgressDetailResponse {
	private UserProgressUserResult user;
	private Summary summary;
	private List<UserProgressDetailResult> attendances;
	private int totalCount;
	private int page;
	private int pageSize;

	/** 요약 통계(기간/이수상태 필터 동일 스코프). */
	@Getter
	@Builder
	public static class Summary {
		private int totalEduMinutes;	// 누적 교육시간(이수 세션 EDU_MINUTES 합)
		private int completedCount;		// 수료 세션수
		private int notCompletedCount;	// 미이수 세션수
	}
}
