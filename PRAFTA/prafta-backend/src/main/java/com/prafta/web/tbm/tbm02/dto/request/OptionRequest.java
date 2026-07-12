package com.prafta.web.tbm.tbm02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 보조 조회(콘텐츠/위험성평가/사업장 선택 모달) 공통 요청.
 *
 * <p>각 옵션 API가 공유한다. 콘텐츠/위험성평가는 사업장 스코프 필터를 적용한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class OptionRequest {
	private String siteCd;			// 스코프 필터(콘텐츠/위험성평가)
	private String searchKeyword;	// 콘텐츠 제목 검색(옵션)
	private String processCd;		// 위험성평가 위험구분(공정) 필터(옵션)
	private String riskTypeCd;		// 위험성평가 위험분류 필터(옵션, 17.3.2)
	private String hazardDesc;		// 위험성평가 유해요인 설명 like 검색(옵션, 17.3.2)
	private String initAssessorNm;	// 위험성평가 평가요청자 like 검색(옵션, 6.3 T6-13)
	private String initAssessDate;	// 위험성평가 평가요청일(YYYY-MM-DD) 일치 검색(옵션, 6.3 T6-13)
}
