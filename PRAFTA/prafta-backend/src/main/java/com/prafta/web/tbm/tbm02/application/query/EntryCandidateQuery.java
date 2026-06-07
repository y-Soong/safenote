package com.prafta.web.tbm.tbm02.application.query;

import com.prafta.web.tbm.tbm02.application.param.EntryCandidateParam;

/**
 * 입실 후보 검색 쿼리(prafta-051-11).
 *
 * <p>siteCd 는 세션 사업장(서버 검증된 값)으로 주입한다. 클라이언트가 보낸 사업장은 신뢰하지 않는다.
 */
public record EntryCandidateQuery(
	String gvCmpnyCd
	, String sessionCd
	, String siteCd
	, String keyword
){
	public static EntryCandidateQuery of(EntryCandidateParam param, String siteCd) {
		return new EntryCandidateQuery(
			param.gvCmpnyCd()
			, param.sessionCd()
			, siteCd
			, param.keyword()
		);
	}
}
