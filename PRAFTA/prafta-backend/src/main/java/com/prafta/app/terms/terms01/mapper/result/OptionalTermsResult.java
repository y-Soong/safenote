package com.prafta.app.terms.terms01.mapper.result;

/**
 * 선택약관(REQUIRED_YN='N' AND USE_YN='Y') 1건 + 사용자 현재버전 동의여부(마이페이지용).
 *
 * <p>agrYn: 사용자가 현재버전을 'Y' 동의 중이면 'Y', 아니면(행 없음 포함) 'N'.
 * <p>termsContent: TB_TERMS 본문(보기용. 화면에서 별도 상세 EP 를 쓰면 미사용일 수 있으나 단일 응답으로 동봉).
 */
public record OptionalTermsResult(
        String termsId
        , String termsNm
        , String termsVersion
        , String termsContent
        , String agrYn
) {
}
