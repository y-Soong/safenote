package com.prafta.common.cmm.baseinfo.result;

/**
 * 회원가입 화면에 띄울 필수약관 1건.
 *
 * <p>★record 위치 매핑이므로 {@code BaseinfoMapper.selectJoinTermsList} 의 SELECT 순서와
 * 아래 컴포넌트 순서를 반드시 일치시킬 것.
 *
 * <p>필드명은 앱·웹 가입 화면이 종전에 쓰던 SYS008 코드 응답과 다르다(termsId/termsNm).
 * 화면이 코드표가 아니라 약관 정의(TB_TERMS)를 보고 있음을 이름으로 드러내기 위함이다.
 */
public record JoinTermsResult(
	String termsId
	, String termsNm
	, String termsVersion
) {

}
