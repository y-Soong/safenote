package com.prafta.app.terms.terms01.dto.response;

import java.util.List;

import com.prafta.app.terms.terms01.mapper.result.OptionalTermsResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 선택약관 목록 응답(마이페이지 약관 동의 설정).
 *
 * <p>각 항목은 현재버전 + 사용자 동의여부(agrYn)를 포함한다.
 */
@Getter
@Builder
public class OptionalTermsResponse {

    private List<OptionalTermsResult> terms;

    public static OptionalTermsResponse of(List<OptionalTermsResult> terms) {
        return OptionalTermsResponse.builder()
                .terms(terms == null ? List.of() : terms)
                .build();
    }
}
