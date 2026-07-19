package com.prafta.app.terms.terms01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 연동 회사 제3자 제공 동의(006) 응답 저장 결과 — PRAFTA-SUBCON-T4-03.
 *
 * <p>{ "resultYn": "Y", "agrYn": "Y"|"N" } — 동의/미동의 둘 다 정상 응답(행 저장 = 게이트 해제).
 */
@Getter
@Builder
public class SubconConsentRespondResponse {

    private String resultYn;
    /** 저장된 동의여부('Y'|'N'). */
    private String agrYn;

    public static SubconConsentRespondResponse success(String agrYn) {
        return SubconConsentRespondResponse.builder()
                .resultYn("Y")
                .agrYn(agrYn)
                .build();
    }
}
