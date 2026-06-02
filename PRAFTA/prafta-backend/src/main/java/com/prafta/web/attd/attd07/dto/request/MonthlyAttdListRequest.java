package com.prafta.web.attd.attd07.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MonthlyAttdListRequest {
	private String workYm;     		// "2026-04" 또는 "202604"
    private String siteCd;     		// 사업장코드
    private String nodeCd;     		// 부서코드 (소속부서)
    private String incSubNodeYn; 	 // 하위부서 조회 여부 (Y/N)
    private String userNm;    		 // 사용자명 (부분일치, nullable)
}
