package com.prafta.web.tbm.tbm01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * TBM 교육자료 AI 분석 지정(항목 단위) 요청 항목.
 * 사용 중(잠긴) 교육자료도 AI_ANALYZE_YN 만은 수정 가능하도록 하는 전용 경로 전용 DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class TbmEduAiAnalyzeItemRequest {
	private String mtrlItemCd;
	private String aiAnalyzeYn;
}
