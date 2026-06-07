package com.prafta.web.chkLst.chkLst04.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DefectListRequest {
	private String siteCd;			// 사업장코드
	private String chkLstType;		// 점검구분[COM001]
	private String chkptCd;			// 점검대상코드(선택값, optional)
	private String inspectItemCd;	// 점검항목코드(선택값, optional)
	private String actionStatus;	// 조치여부 필터: '' 전체 / 'Y' 조치완료 / 'N' 미조치
}
