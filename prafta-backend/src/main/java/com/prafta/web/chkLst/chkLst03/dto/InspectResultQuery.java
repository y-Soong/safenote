package com.prafta.web.chkLst.chkLst03.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InspectResultQuery{
	String cmpnyCd;					// 회사코드
	String userId;					// 사용자코드
	String fromDate;				// 점검조회 시작 월
	String toDate;					// 점검조회 종료 월
	String siteCd;					// 사업장코드
	String chkptNm;					// 점검대상명칭
	String chkLstType;				// 일일점검구분
}
