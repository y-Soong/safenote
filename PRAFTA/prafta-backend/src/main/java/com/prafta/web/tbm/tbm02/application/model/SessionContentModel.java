package com.prafta.web.tbm.tbm02.application.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 세션-콘텐츠 묶음 매핑 입력 모델(개설/수정 요청에 포함). */
@Getter
@Setter
@NoArgsConstructor
public class SessionContentModel {
	private String mtrlCd;			// 교육자료 묶음코드(TB_TBM_EDU_MTRL)
	private Integer displayOrder;	// 세션 내 표시 순서
	private String overrideDesc;	// 세션별 설명 override
}
