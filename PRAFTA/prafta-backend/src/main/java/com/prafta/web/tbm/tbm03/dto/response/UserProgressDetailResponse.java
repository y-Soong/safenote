package com.prafta.web.tbm.tbm03.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm03.result.UserProgressUserResult;

import lombok.Builder;
import lombok.Getter;

/**
 * T7 드릴다운 응답(사용자 헤더 + 요약 + 세션 이력 목록 + 페이징).
 *
 * <p>PRAFTA-SUBCON-T5 F4: 자기 직원이 <b>타사(개최사) 세션</b>에 참여한 이력도 포함된다(요청서 §3.4).
 * 타사 세션 행은 개최 회사명({@code hostCmpnyNm} = 나를 지정한 직상위 회사)만 표시하며, 타사
 * 사업장/회사코드는 응답에 싣지 않는다(마스터 §1-3 인접 차수 가시성, plan D3).
 */
@Getter
@Builder
public class UserProgressDetailResponse {
	private UserProgressUserResult user;
	private Summary summary;
	private List<AttendanceItem> attendances;
	private int totalCount;
	private int page;
	private int pageSize;

	/** 세션별 이수 이력 1행. */
	@Getter
	@Builder
	public static class AttendanceItem {
		private String attendanceCd;
		private String sessionCd;
		private String sessionTitle;
		private String sessionDate;		// 세션 종료일(없으면 등록일)
		private Integer eduMinutes;		// 인정시간(분, NULL 가능)
		private String entryAt;
		private String exitAt;
		private String completionStatusCd;
		private String completionStatusNm;
		/** 개최 회사(타사 연동 세션일 때만). 자사 세션은 null. */
		private String hostCmpnyNm;
	}

	/** 요약 통계(기간/이수상태 필터 동일 스코프). */
	@Getter
	@Builder
	public static class Summary {
		private int totalEduMinutes;	// 누적 교육시간(이수 세션 EDU_MINUTES 합)
		private int completedCount;		// 수료 세션수
		private int notCompletedCount;	// 미이수 세션수
	}
}
