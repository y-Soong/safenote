package com.prafta.web.subcon.subcon01.dto.response;

import lombok.Builder;
import lombok.Value;

/**
 * 연동 관계 요청 생성 응답.
 */
@Value
@Builder
public class RelationCreateResponse {
    Long relationId;
}
