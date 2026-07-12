package com.prafta.web.user.user06.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 블랙리스트 등록 응답(채번된 blacklistId 반환).
 */
@Value
@Builder
public class BlacklistRegResponse {
    String blacklistId;
}
