package com.prafta.app.entryadmin.entryadmin01.dto.request;

import java.util.List;

/**
 * 입장 승인 처리 요청 바디 (일괄 승인 D9 — reqIds 복수).
 * 처리자 식별(cmpny/user/auth)은 JWT 클레임에서만 도출하며 바디로 받지 않는다(IDOR 차단).
 */
public record EntryApproveRequest(
    List<String> reqIds
) {
}
