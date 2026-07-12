package com.prafta.common.cmm.baseinfo.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SiteInfoRequest {
	String cmpnyCd;
	String siteNo;
	String siteNm;
	// 사용여부 필터('Y'/'N'). 일반(로그인 후) 조회에서 선택. 빈 값이면 전체. 회원가입은 서버가 'Y' 강제(무시).
	String useYn;
}
