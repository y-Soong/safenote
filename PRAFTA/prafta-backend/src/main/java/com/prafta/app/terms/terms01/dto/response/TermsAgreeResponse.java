package com.prafta.app.terms.terms01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 약관 동의/토글 처리 결과 ack 응답.
 *
 * <p>{ "result": "SUCCESS", "affected": N } — affected 는 upsert 영향행(멱등 진단용).
 */
@Getter
@Builder
public class TermsAgreeResponse {

    private String result;
    private int affected;

    public static TermsAgreeResponse success(int affected) {
        return TermsAgreeResponse.builder()
                .result("SUCCESS")
                .affected(affected)
                .build();
    }
}
