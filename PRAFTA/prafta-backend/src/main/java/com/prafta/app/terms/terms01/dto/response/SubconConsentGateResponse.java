package com.prafta.app.terms.terms01.dto.response;

import com.prafta.common.cmm.consent.mapper.result.ConsentTermsResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 연동 회사 제3자 제공 동의(006) 게이트 판정 응답 — PRAFTA-SUBCON-T4-03.
 *
 * <p>gateRequiredYn='Y' 일 때만 약관 메타(termsId/termsNm/termsVersion/termsDesc)가 채워진다.
 * <p>★ 약관 전문(TERMS_CONTENT)은 싣지 않는다 — 전문은 기존 /TermsDetail 경로가 렌더한다(페이로드 절감).
 */
@Getter
@Builder
public class SubconConsentGateResponse {

    /** 게이트 노출 필요 여부('Y'|'N'). 'N' 이면 화면 미표시 통과. */
    private String gateRequiredYn;

    private String termsId;
    private String termsNm;
    private String termsVersion;
    /** 약관 요약(TB_TERMS.TERMS_DESC) — 화면 문구 하드코딩 금지를 위해 서버가 내려준다. */
    private String termsDesc;

    /** 게이트 불필요(약관 미배포 / 비연동 사업장 / 이미 응답함). */
    public static SubconConsentGateResponse notRequired() {
        return SubconConsentGateResponse.builder()
                .gateRequiredYn("N")
                .build();
    }

    /** 게이트 필요(활성 연동 사업장 소속 + 현재버전 미응답). */
    public static SubconConsentGateResponse required(ConsentTermsResult terms) {
        return SubconConsentGateResponse.builder()
                .gateRequiredYn("Y")
                .termsId(terms.termsId())
                .termsNm(terms.termsNm())
                .termsVersion(terms.termsVersion())
                .termsDesc(terms.termsDesc())
                .build();
    }
}
