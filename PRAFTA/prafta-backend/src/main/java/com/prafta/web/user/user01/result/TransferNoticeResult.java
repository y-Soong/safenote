package com.prafta.web.user.user01.result;

/**
 * 소속이동 안내(로그인 팝업/PUSH) 표시 정보 — PRAFTA-WEB_001-3.
 *
 * <p>예약 레코드 + 회사 스코프 명칭 조인 결과(사업장명/부서명/기본근무타입명).
 * moveDate 는 저장 원본 YYYYMMDD(서비스에서 표시용으로 포맷). defaultSchNm 은 일용직이면 null.
 */
public record TransferNoticeResult(
    String reservationId
    , String moveDate
    , String toSiteNm
    , String toNodeNm
    , String defaultSchNm
    , String moveReason
) {}
