package com.prafta.web.tbm.tbm02.dto.response;

import java.util.List;

import lombok.Builder;
import lombok.Getter;

/**
 * 입실 후보 검색 응답(prafta-051-11 + PRAFTA-SUBCON-T5).
 *
 * <p>T5: 후보는 선택된 대상 회사(개설사 또는 1차 회사)와 그 하위 재지정 체인에서 검색되며, 소속 표시명은
 * 1차 relabel 값 1개다({@code affilCmpnyNm}). 프론트는 이 값을 "소속" 컬럼에 그대로 표시한다
 * (회사코드로 이름을 재조립하지 않는다).
 *
 * <p><b>M1</b>: 각 행은 사용자코드 대신 서버가 발급한 <b>불투명 핸들</b>({@code entryHandle})을 갖는다.
 * 대리입실 요청은 이 핸들만 키로 보낸다(회사코드/사용자코드를 클라가 알지도 조작하지도 못한다).
 * <p><b>M2</b>: 타사 행의 사업장(siteCd/siteNm)은 null 로 내린다 — 사업장명으로 2차 회사가 식별되는 것을
 * 막는다(마스터 §1-3, tbm04 W-15 와 동일 규칙).
 */
@Getter
@Builder
public class EntryCandidateResponse {
	private String userTypeCd;
	/** 후보 검색 대상 회사(개설사 또는 1차 회사 — 클라가 고른 값). */
	private String targetCmpnyCd;
	/** 대상 회사의 표시 소속명(1차 relabel). */
	private String affilCmpnyNm;
	private List<CandidateItem> candidateList;

	@Getter
	@Builder
	public static class CandidateItem {
		/** 서버 발급 불투명 핸들(AES-GCM). 대리입실 요청의 유일한 대상 키. */
		private String entryHandle;
		private String userId;
		private String userNm;
		private String siteCd;		// 타사 행은 null(M2)
		private String siteNm;		// 타사 행은 null(M2)
		private String mblNoLast4;	// 일용직만
		private boolean alreadyEntered;
	}
}
