package com.prafta.web.tbm.tbm01.result;

/**
 * prafta-033-A: W-03 상세 - 이 콘텐츠 묶음을 사용한 TBM 세션 1건.
 * B 단계(세션 관리) 이전에는 데이터가 없어 빈 목록으로 조회된다.
 */
public record TbmEduUsedSessionResult(
	String sessionCd
	, String title
	, String statusCd
	, String siteCd
	, String openedAt		// 개설 시각(yyyy-MM-dd HH:mm)
	, String endedAt		// 종료 시각(yyyy-MM-dd HH:mm)
	, String displayOrder
){

}
