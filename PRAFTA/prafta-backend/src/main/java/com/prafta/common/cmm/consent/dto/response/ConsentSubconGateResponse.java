package com.prafta.common.cmm.consent.dto.response;

import com.prafta.common.cmm.consent.mapper.result.ConsentTermsResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 연동 회사 제3자 제공 동의(006) 게이트 판정 응답(웹 로그인 게이트).
 *
 * <p>앱 SubconConsentGateResponse 와 같은 형태다 — 프론트 분기 로직을 채널별로 갈라놓지 않기 위함.
 * <p>gateRequiredYn='Y' 일 때만 약관 메타가 채워진다. 약관 전문은 싣지 않는다(전문은 별도 상세 팝업).
 */
@Getter
@Builder
public class ConsentSubconGateResponse {

    /** 게이트 노출 필요 여부('Y'|'N'). 'N' 이면 팝업 미표시 통과. */
    private String gateRequiredYn;

    private String termsId;
    private String termsNm;
    private String termsVersion;
    /** 약관 요약(TB_TERMS.TERMS_DESC) — 화면 문구 하드코딩 금지를 위해 서버가 내려준다. */
    private String termsDesc;

    /** 게이트 불필요(약관 미배포 / 비연동 사업장 / 이미 응답함). */
    public static ConsentSubconGateResponse notRequired() {
        return ConsentSubconGateResponse.builder()
                .gateRequiredYn("N")
                .build();
    }

    /** 게이트 필요(활성 연동 사업장 소속 + 현재버전 미응답). */
    public static ConsentSubconGateResponse required(ConsentTermsResult terms) {
        return ConsentSubconGateResponse.builder()
                .gateRequiredYn("Y")
                .termsId(terms.termsId())
                .termsNm(terms.termsNm())
                .termsVersion(terms.termsVersion())
                .termsDesc(terms.termsDesc())
                .build();
    }
}
