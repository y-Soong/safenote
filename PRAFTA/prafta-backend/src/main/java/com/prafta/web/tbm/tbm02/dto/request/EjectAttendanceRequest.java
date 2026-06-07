package com.prafta.web.tbm.tbm02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 입실자 내보내기 요청(prafta-051-12). POST /tbm02/eject-attendance.
 *
 * <p>관리자가 교육준비(OPENED) 단계에서 잘못 입실한 사용자를 내보낸다(soft delete).
 * reason 은 필수이며 EXIT_FORCED_REASON 에 책임 기록으로 저장한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class EjectAttendanceRequest {
	private String sessionCd;		// 대상 세션
	private String attendanceCd;	// 내보낼 출결코드
	private String reason;			// 내보내기 사유(필수)
}
