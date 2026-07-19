package com.prafta.web.subcon.subcon01.dto.response;

import java.util.List;

import com.prafta.web.subcon.subcon01.result.RelationResult;

import lombok.Builder;
import lombok.Value;

/**
 * 연동 관계 목록 응답(자사가 당사자인 전 관계 — 프론트가 연동중/보낸/받은 3분류).
 */
@Value
@Builder
public class RelationListResponse {
    List<RelationResult> relations;
}
