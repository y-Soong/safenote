package com.prafta.web.notice.notice01.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.prafta.web.notice.notice01.result.NoticeScopeResult;

import lombok.Builder;
import lombok.Getter;

/**
 * 발행자 대상선택 트리 응답.
 * canSelectAll=true 면 전사(ALL) 스코프 선택 가능(master 등 전사 권한).
 */
@Getter
@Builder
public class NoticeScopeResponse {
    @JsonProperty("canSelectAll")
    private boolean canSelectAll;
    private List<NoticeScopeResult> scopeList;
}
