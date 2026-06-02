package com.prafta.web.tbm.tbm04.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** W-14 미이수 처리(이수/미이수 사후 변경) 요청. prafta-033-D. */
@Getter
@Setter
@NoArgsConstructor
public class CompletionUpdateRequest {
	private String attendanceCd;		// 필수
	private String completionStatusCd;	// COMPLETED / NOT_COMPLETED
	private String reason;				// 필수, 10자 이상
}
