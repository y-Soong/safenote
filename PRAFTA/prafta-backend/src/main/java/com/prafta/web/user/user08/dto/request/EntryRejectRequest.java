package com.prafta.web.user.user08.dto.request;

/**
 * 입장 거부 처리 요청 바디 (D10 — 사유 필수, 200자 이하).
 * 처리자 식별(cmpny/user/auth)은 JWT 클레임에서만 도출하며 바디로 받지 않는다(IDOR 차단).
 */
public record EntryRejectRequest(
    String reqId
    , String reason
) {
}
