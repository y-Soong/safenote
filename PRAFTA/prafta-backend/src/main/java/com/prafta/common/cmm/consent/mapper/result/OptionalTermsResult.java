package com.prafta.common.cmm.consent.mapper.result;

/**
 * 선택약관(REQUIRED_YN='N' AND USE_YN='Y') 1건 + 사용자 현재버전 동의여부.
 *
 * <p>앱 마이페이지(app.terms.terms01)와 웹 내 정보 팝업(common.cmm.consent 컨트롤러)이 공유한다.
 *    원래 app.terms.terms01.mapper.result 에 있던 record 를 공용 위치로 이관한 것이며,
 *    JSON 필드명(컴포넌트명)은 그대로라 앱 응답 스펙은 변하지 않는다.
 *
 * <p>agrYn: 사용자가 현재버전을 'Y' 동의 중이면 'Y', 아니면(행 없음 포함) 'N'.
 * <p>termsContent: TB_TERMS 본문(보기용. 화면이 별도 상세 EP 를 쓰면 미사용일 수 있으나 단일 응답으로 동봉).
 * <p>★ record 매핑이므로 SELECT 컬럼 순서 = 아래 컴포넌트 순서를 반드시 유지할 것.
 */
public record OptionalTermsResult(
        String termsId
        , String termsNm
        , String termsVersion
        , String termsContent
        , String agrYn
) {
}
