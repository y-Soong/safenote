package com.prafta.web.chkLst.chkLst04.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DefectActionRequest {
	private String siteCd;			// 사업장코드
	private String chkptCd;			// 점검대상코드
	private String inspectItemCd;	// 점검항목코드
	private String workDate;		// 점검일자(YYYYMMDD)
	private String actionDesc;		// 조치 상세 내역
}
