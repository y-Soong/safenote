package com.prafta.web.user.user06.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 블랙리스트 해제 요청.
 *
 * <p>blacklistId 만 받는다. 회사 스코프(cmpnyCd)는 서버 JWT 클레임에서 강제(IDOR 방지).
 */
@Getter
@Setter
@NoArgsConstructor
public class BlacklistReleaseRequest {
    private String blacklistId;
}
