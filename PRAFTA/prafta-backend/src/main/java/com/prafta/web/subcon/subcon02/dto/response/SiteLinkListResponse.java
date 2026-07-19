package com.prafta.web.subcon.subcon02.dto.response;

import java.util.List;

import com.prafta.web.subcon.subcon02.result.SiteLinkResult;

import lombok.Builder;
import lombok.Value;

/**
 * 사업장 연동 링크 목록 응답(전 상태 반환 — 목록이 곧 이력, 프론트가 제공/받은 2분류).
 */
@Value
@Builder
public class SiteLinkListResponse {
    List<SiteLinkResult> links;
}
