package com.prafta.web.tbm.tbm02.application.query;

import java.util.List;

/**
 * 대리입실 대상 사용자 유효성 검증 + <b>소속 회사 도출</b> 쿼리(prafta-051-11 + PRAFTA-SUBCON-T5 F2).
 *
 * <p>대상이 활성 사용자인지(정규직/일용직) 서버에서 재확인한다. 일용직은 C7 만료/탈퇴 필터까지
 * 재검증해 만료자 입실을 차단한다(신원 보증 책임은 고용 회사 — 요청서 §3.2).
 *
 * <p><b>F2</b>: 결과로 "대상 사용자가 실제로 속한 회사코드"를 반환한다. 클라이언트는 1차 회사까지만
 * 알고 2차 이하 회사코드는 모르므로, 출결행에 기록할 CMPNY_CD 를 <b>서버가 체인 범위
 * ({@code targetCmpnyCds}) 안에서 도출</b>한다.
 *
 * <p>{@code ownTarget}(개설사 자신) 일 때만 사업장 조건을 건다(plan D4).
 */
public record EntryTargetQuery(
	List<String> targetCmpnyCds
	, String siteCd
	, String userTypeCd
	, String userCd
	, boolean ownTarget
){

}
