package com.prafta.web.tbm.tbm02.application.query;

import java.util.List;

import com.prafta.web.tbm.tbm02.application.param.EntryCandidateParam;

/**
 * 입실 후보 검색 쿼리(prafta-051-11 + PRAFTA-SUBCON-T5).
 *
 * <p>siteCd 는 세션 사업장(서버 검증된 값)으로 주입한다. 클라이언트가 보낸 사업장은 신뢰하지 않는다.
 *
 * <p><b>T5 F2</b>: 클라이언트는 "대상 회사"로 개설사 또는 <b>1차 회사</b>만 보낸다. 서버가 그 하위
 * 재지정 체인까지 확장한 회사코드 집합({@code targetCmpnyCds})으로 후보를 검색한다. 2차 이하
 * 회사코드는 클라이언트에 노출되지 않으며 응답에도 실리지 않는다(마스터 §1-3 인접 차수 가시성).
 *
 * <p>{@code ownTarget}(= 대상이 개설사 자신)일 때만 사업장 조건을 건다. 타사 대상은 회사 단위
 * 지정이라 사업장 조건을 걸지 않는다(타사 사업장코드는 그 회사 네임스페이스 — plan D4).
 */
public record EntryCandidateQuery(
	List<String> targetCmpnyCds
	, String sessionCd
	, String siteCd
	, String keyword
	, boolean ownTarget
){
	public static EntryCandidateQuery of(EntryCandidateParam param, String siteCd,
			List<String> targetCmpnyCds, boolean ownTarget) {
		return new EntryCandidateQuery(
			targetCmpnyCds
			, param.sessionCd()
			, siteCd
			, param.keyword()
			, ownTarget
		);
	}
}
