package com.prafta.web.subcon.subcon03.dto.response;

import lombok.Builder;
import lombok.Value;

/** 데이터 공유 요청 생성 응답(생성된 요청ID). */
@Value
@Builder
public class ShareReqCreateResponse {
    Long shareReqId;
}
