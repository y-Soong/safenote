package com.prafta.web.subcon.subcon01.dto.response;

import java.util.List;

import com.prafta.web.subcon.subcon01.result.RelationHistResult;

import lombok.Builder;
import lombok.Value;

/**
 * 연동 관계 이력 응답(당사자 회사만 조회 가능 — 비당사자 404).
 */
@Value
@Builder
public class RelationHistResponse {
    List<RelationHistResult> hists;
}
