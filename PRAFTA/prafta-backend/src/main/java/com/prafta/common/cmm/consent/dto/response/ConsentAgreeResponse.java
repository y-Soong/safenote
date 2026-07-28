package com.prafta.common.cmm.consent.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 동의 저장 결과 응답(선택약관 토글 / 제3자 제공 동의 응답 공통).
 *
 * <p>{ "resultYn": "Y", "agrYn": "Y"|"N", "affected": n } — 전이가 없으면 affected=0 이며 이 역시 정상이다(멱등).
 */
@Getter
@Builder
public class ConsentAgreeResponse {

    private String resultYn;
    /** 저장된 동의여부('Y'|'N'). */
    private String agrYn;
    /** upsert 영향행(동일 값 재저장 등 전이 없음이면 0). */
    private int affected;

    public static ConsentAgreeResponse success(String agrYn, int affected) {
        return ConsentAgreeResponse.builder()
                .resultYn("Y")
                .agrYn(agrYn)
                .affected(affected)
                .build();
    }
}
