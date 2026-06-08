package com.prafta.web.notice.notice02.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

/**
 * 자료실 비밀번호 검증 응답. verified=true 면 수정 모드 진입 가능.
 * boolean is- 접두 Jackson 탈락 방지를 위해 @JsonProperty 로 키 고정.
 */
@Getter
@Builder
public class ArchivePwdResponse {
    @JsonProperty("verified")
    private boolean verified;
}
