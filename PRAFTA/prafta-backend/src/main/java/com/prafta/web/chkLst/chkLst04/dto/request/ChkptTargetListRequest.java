package com.prafta.web.chkLst.chkLst04.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChkptTargetListRequest {
	private String siteCd;			// 사업장코드
	private String chkLstType;		// 점검구분[COM001]
	private String chkptNm;			// 점검대상명칭(검색어, optional)
}
