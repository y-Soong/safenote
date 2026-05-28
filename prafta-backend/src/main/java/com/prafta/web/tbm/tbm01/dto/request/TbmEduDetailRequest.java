package com.prafta.web.tbm.tbm01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * prafta-033-A: W-03 콘텐츠 상세(묶음+세부항목+사용 TBM 이력) 조회 요청.
 */
@Getter
@Setter
@NoArgsConstructor
public class TbmEduDetailRequest {
	private String mtrlCd;
}
