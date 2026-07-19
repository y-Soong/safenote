package com.prafta.web.tbm.tbm02.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 입실 후보 검색 요청(prafta-051-11). GET /tbm02/entry-candidates.
 *
 * <p>관리자가 대리/검색 입실을 위해 후보 사용자를 조회한다. 세션 사업장(서버 검증) 기준으로
 * 정규직(REGULAR) 또는 일용직(DAILY)을 조회하며, keyword 는 이름/아이디 부분검색(옵션).
 */
@Getter
@Setter
@NoArgsConstructor
public class EntryCandidateRequest {
	private String sessionCd;	// 대상 세션
	private String userTypeCd;	// REGULAR | DAILY
	private String keyword;		// 이름/아이디 부분검색(옵션)
	// PRAFTA-SUBCON-T5: 대상 회사(미지정이면 자사). 서버가 {개설사} ∪ 지정 체인 소속인지 재검증한다.
	private String targetCmpnyCd;
}
