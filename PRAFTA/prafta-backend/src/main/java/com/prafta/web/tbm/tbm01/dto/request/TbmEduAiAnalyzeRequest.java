package com.prafta.web.tbm.tbm01.dto.request;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * TBM 교육자료 AI 분석 지정 저장 요청.
 * <p>사용 중(TBM 세션 참조로 잠긴) 교육자료는 내용 수정이 불가하나,
 * 세부항목의 AI 분석 지정(AI_ANALYZE_YN)만은 이 경로로 갱신할 수 있다.
 */
@Getter
@Setter
@NoArgsConstructor
public class TbmEduAiAnalyzeRequest {
	/** 소속 교육자료 코드(회사 스코프/부모매칭 검증용) */
	private String mtrlCd;
	/** AI 분석 지정 대상 세부항목 목록 */
	private List<TbmEduAiAnalyzeItemRequest> itemList;
}
