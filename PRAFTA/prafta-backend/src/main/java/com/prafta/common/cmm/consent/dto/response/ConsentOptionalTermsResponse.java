package com.prafta.common.cmm.consent.dto.response;

import java.util.List;

import com.prafta.common.cmm.consent.mapper.result.OptionalTermsResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 선택약관 목록 응답(웹 내 정보 팝업 "약관 동의 설정").
 *
 * <p>앱 마이페이지 응답(app OptionalTermsResponse)과 같은 형태다 — { terms: [...] }.
 */
@Getter
@Builder
public class ConsentOptionalTermsResponse {

    private List<OptionalTermsResult> terms;

    public static ConsentOptionalTermsResponse of(List<OptionalTermsResult> terms) {
        return ConsentOptionalTermsResponse.builder()
                .terms(terms == null ? List.of() : terms)
                .build();
    }
}
