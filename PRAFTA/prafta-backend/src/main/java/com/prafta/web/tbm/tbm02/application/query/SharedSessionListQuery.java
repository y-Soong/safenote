package com.prafta.web.tbm.tbm02.application.query;

import com.prafta.web.tbm.tbm02.application.param.SharedSessionListParam;

/**
 * 연동받은 교육 목록 Query(PRAFTA-SUBCON-T5 D2).
 *
 * <p>스코프(= 인가)는 매퍼의 SHARE EXISTS 다: 내 회사({@code gvCmpnyCd}, 토큰 출처)가 유효하게
 * 지정받은 세션만. 클라가 보낸 세션코드/회사코드를 신뢰하는 지점이 없다.
 * 사업장 스코프는 적용하지 않는다 — 타사 세션의 SITE_CD 는 개설사 네임스페이스이고, 지정은 회사 단위다.
 */
public record SharedSessionListQuery(
	String gvCmpnyCd
	, String statusCd
	, String searchKeyword
	, int offset
	, int pageSize
){
	public static SharedSessionListQuery from(SharedSessionListParam param) {
		return new SharedSessionListQuery(
			param.gvCmpnyCd()
			, param.statusCd()
			, param.searchKeyword()
			, (param.page() - 1) * param.pageSize()
			, param.pageSize()
		);
	}
}
