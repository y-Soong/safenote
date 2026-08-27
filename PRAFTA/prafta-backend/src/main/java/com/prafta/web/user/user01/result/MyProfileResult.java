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
	// PRAFTA-001(기본근무타입-승인제, 2026-08-27): 대기중 신청 요약(MyInfoPop 배너용, 없으면 전부 null).
	// ★MyBatis record 는 SELECT 컬럼 순서 매핑 — 반드시 끝에 추가.
	, String pendingDefaultSchReqId
	, String pendingDefaultSchCd
	, String pendingDefaultSchNo
	, String pendingDefaultSchReqDate
){

}
