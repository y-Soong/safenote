package com.prafta.app.device.device01.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 푸시 토큰 등록 응답. 단순 ack: { "result": "SUCCESS" }.
 */
@Getter
@Builder
public class PushTokenResponse {
    private String result;

    // 정상 ack 생성 헬퍼(영향행 0이어도 성공 ack 반환 — F01 §6)
    public static PushTokenResponse success() {
        return PushTokenResponse.builder()
            .result("SUCCESS")
            .build();
    }
}
