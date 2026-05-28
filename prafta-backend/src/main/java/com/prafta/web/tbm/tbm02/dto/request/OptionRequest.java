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
	private String processCd;		// 위험성평가 공정코드 필터(옵션)
}
