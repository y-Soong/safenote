package com.prafta.web.chkLst.chkLst03.vo;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InspectResult {
	String chk;
	String cmpnyCd;								// 회사코드
	String siteCd;								// 사업장코드
	String siteNm;								// 사업장명
	String chkptCd;								// 점검대상코드
	String chkptNm;								// 점검대상명
	String siteAdminId;							// 사업장 관리자 ID
	String siteAdminNm;							// 사업장 관리자명
	String chkLstType;							// 체크리스트 타입[COM001]
	String chkptDesc;							// 점검대상비고
	String workDate;							// 점검조회월
	String inspectDayCnt;						// 점검시행 일자 수
	String defectiveResultCnt;					// 불량요소 수
}
