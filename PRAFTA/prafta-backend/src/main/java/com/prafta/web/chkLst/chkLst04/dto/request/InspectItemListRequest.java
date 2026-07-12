package com.prafta.web.chkLst.chkLst04.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InspectItemListRequest {
	private String chkLstType;		// 점검구분[COM001]
	private String inspectItemSubj;	// 점검항목명(검색어, optional)
	private String useYn;			// 사용여부 필터(전체='' / 'Y' / 'N', optional) - PRAFTA_COM_001-T5-12.3
}
