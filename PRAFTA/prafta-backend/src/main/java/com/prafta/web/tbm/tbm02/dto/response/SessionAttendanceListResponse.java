package com.prafta.web.tbm.tbm02.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 교육준비 단계 입실자 명단 응답(prafta-051-12 + PRAFTA-SUBCON-T5).
 *
 * <p>T5: 타사(지정 체인) 참석자가 섞이므로 소속 표시(affilCmpnyNm)를 추가한다. 표시값은 서버가
 * 개설사 직하 <b>1차 회사명으로 접은 relabel 값</b>이며(마스터 §1-3 인접 차수 가시성), 2차 이하
 * 회사코드/회사명은 응답에 싣지 않는다.
 */
@Getter
@Builder
public class SessionAttendanceListResponse {
	private String sessionCd;
	private int totalCount;
	private List<AttendanceItem> attendanceList;

	@Getter
	@Builder
	public static class AttendanceItem {
		private String attendanceCd;
		private String userTypeCd;
		private String userNm;
		private String mblNoLast4;
		private String entryTypeCd;
		private String entryAt;
		private Integer entryDistanceM;
		private boolean exited;
		/** 소속(1차 relabel). 자사 참석자는 자사명. */
		private String affilCmpnyNm;
	}
}
