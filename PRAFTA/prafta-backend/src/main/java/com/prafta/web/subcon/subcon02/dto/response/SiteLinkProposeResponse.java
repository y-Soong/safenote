package com.prafta.web.subcon.subcon02.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 사업장 연동 제안 생성 응답(생성된 링크ID).
 */
@Value
@Builder
public class SiteLinkProposeResponse {
    Long linkId;
}
