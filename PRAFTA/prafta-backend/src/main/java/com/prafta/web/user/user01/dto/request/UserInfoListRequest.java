package com.prafta.web.user.user01.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserInfoListRequest{
	private String userId;
	private String userNm;
	// 사용자정보 통합 검색어(사용자ID·사용자명 동시 부분일치). userId/userNm 분리 조건을 대체한다.
	private String userKeyword;
	private String useYn;
	private String siteCd;
	private String nodeCd;
	private String incSubNodeYn;
	private String searchMode;
	// 고용형태 필터(예: REGULAR). 전달 시에만 적용(미전달이면 무필터, 타 호출처 무영향).
	private String employmentType;
}
