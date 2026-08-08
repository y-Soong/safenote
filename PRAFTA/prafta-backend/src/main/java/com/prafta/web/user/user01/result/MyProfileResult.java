package com.prafta.web.user.user01.result;

public record MyProfileResult(
	String userId
	, String userNm
	, String siteNm
	, String nodeNm
	, String mblNo
	, String email
	, String lastLoginDtime
	// F-8-2: 현재 기본 근무타입 표시용(있으면 SCH_CD/SCH_NO/시각, 미설정이면 전부 null).
	, String defaultSchCd
	, String defaultSchNo
	, String defaultSchStrTime
	, String defaultSchEndTime
){

}
