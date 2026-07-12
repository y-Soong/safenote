package com.prafta.app.terms.terms01.dto.response;

import java.util.List;

import com.prafta.app.terms.terms01.mapper.result.PendingTermsResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 미동의 필수약관 목록 응답(앱 로그인 게이트).
 *
 * <p>terms 가 비어 있으면 게이트 불필요(곧바로 진입). pending=terms.size().
 */
@Getter
@Builder
public class PendingTermsResponse {

    // 미동의 필수약관 목록(빈 목록이면 게이트 불필요)
    private List<PendingTermsResult> terms;

    public static PendingTermsResponse of(List<PendingTermsResult> terms) {
        return PendingTermsResponse.builder()
                .terms(terms == null ? List.of() : terms)
                .build();
    }
}
