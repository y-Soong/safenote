package com.prafta.web.user.user01.result;

/**
 * 소속이동 불가 사유 1건(코드 + 사용자 친화 메시지) — PRAFTA-WEB_001-1.
 *
 * <p>eligibility 조회 응답(blockReasons 배열)과 등록 검증(첫 사유로 차단)에서 공통 사용한다.
 * code 는 {@link com.prafta.common.error.user.UserErrorCode} 의 name()(예: "USER_400_065").
 */
public record TransferBlockReason(
    String code
    , String message
) {}
