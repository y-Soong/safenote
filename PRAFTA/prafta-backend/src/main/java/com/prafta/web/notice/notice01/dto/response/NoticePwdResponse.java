package com.prafta.web.notice.notice01.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * 공지 비밀번호 검증 응답.
 * verified=true 면 수정 모드 진입 가능. boolean is/verified Jackson 키 고정.
 */
@Getter
@Builder
public class NoticePwdResponse {
    @JsonProperty("verified")
    private boolean verified;
}
