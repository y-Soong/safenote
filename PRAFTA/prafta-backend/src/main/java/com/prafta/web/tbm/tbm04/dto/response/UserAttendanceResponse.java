package com.prafta.web.tbm.tbm04.dto.response;

import java.util.List;

import com.prafta.web.tbm.tbm04.result.UserInfoResult;

import lombok.Builder;
import lombok.Getter;

/**
 * W-15 사용자별 이수 이력 응답(사용자 + 이력 목록 + 통계).
 *
 * <p>PRAFTA-SUBCON-T5: 자기 직원이 <b>타사(개최사) 세션</b>에 참여한 이력도 포함된다(요청서 §3.4).
 * 타사 세션 행은 사업장 정보를 내리지 않고(siteCd/siteNm=null — 타사 사업장은 인접 차수 밖 정보,
 * plan D3) 개최 회사명({@code hostCmpnyNm} = 나를 지정한 직상위 회사)만 표시한다.
 */
@Getter
@Builder
public class UserAttendanceResponse {
	private UserInfoResult user;
	private List<AttendanceItem> attendances;
	private int totalCount;
	private int page;
	private int pageSize;
	private Summary summary;

	@Getter
	@Builder
	public static class AttendanceItem {
		private String attendanceCd;
		private String sessionCd;
		private String sessionTitle;
		private String siteCd;			// 타사 세션이면 null
		private String siteNm;			// 타사 세션이면 null
		/** 개최 회사(타사 세션일 때만). 자사 세션은 null. */
		private String hostCmpnyNm;
		private String sessionDate;		// 세션 종료일(없으면 개설/등록일)
		private String entryAt;
		private String exitAt;
		private String completionStatusCd;
		private String completionStatusNm;
		private int riskCount;			// 해당 세션의 연계 위험성평가 수
	}

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
