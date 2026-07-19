package com.prafta.common.cmm.consent.mapper.result;

/**
 * 활성 약관 1건(TB_TERMS + SYS008 약관명) — PRAFTA-SUBCON-T4-02.
 *
 * <p>USE_YN='Y' 인 약관의 현재버전/요약을 담는다. 조회 결과가 없으면 "약관 미배포"(제도 미가동)를 의미한다.
 * <p>본문(TERMS_CONTENT)은 담지 않는다 — 게이트 응답 페이로드 절감(전문은 기존 /TermsDetail 경로 재사용).
 * <p>★ record 매핑이므로 SELECT 컬럼 순서 = 아래 컴포넌트 순서를 반드시 유지할 것.
 */
public record ConsentTermsResult(
        String termsId
        , String termsNm
        , String termsVersion
        , String termsDesc
) {
}
