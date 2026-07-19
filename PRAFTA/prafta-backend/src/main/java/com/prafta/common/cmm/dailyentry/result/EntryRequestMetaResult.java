package com.prafta.common.cmm.dailyentry.result;

/**
 * 입장 승인요청 단건 메타 (판정/처리 가드용).
 *
 * <p>⚠️ MyBatis record 매핑 — SELECT 컬럼 순서와 컴포넌트 순서가 일치해야 한다.
 */
public record EntryRequestMetaResult(
    String reqId
    , String siteCd
    , String userCd
    , String reqType     // [SYS081] 01:신규가입 / 02:재입장
    , String reqStatus   // [SYS082] 01:대기 / 02:승인 / 03:거부 / 04:만료 / 05:소진
) {
}
