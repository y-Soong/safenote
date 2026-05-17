package com.prafta.web.chkLst.chkLst03.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class InspectResultRequest{
	private String cmpnyCd;					// 회사코드
	private String userId;					// 사용자코드
	private String fromDate;				// 점검조회 시작 월
	private String toDate;					// 점검조회 종료 월
	private String siteCd;					// 사업장코드
	private String chkptNm;					// 점검대상명칭
	private String chkLstType;				// 일일점검구분
}
