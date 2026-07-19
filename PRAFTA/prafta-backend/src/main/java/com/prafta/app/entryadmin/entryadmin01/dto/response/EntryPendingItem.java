package com.prafta.app.entryadmin.entryadmin01.dto.response;

/**
 * 앱 관리자 입장 승인 대기 목록 항목 (UI-DC-03 카드).
 * 휴대폰은 서버에서 last4 마스킹("***-****-XXXX")된 문자열만 노출한다(평문/ENC 금지).
 */
public record EntryPendingItem(
    String reqId
    , String userNm
    , String mblNo        // 마스킹 문자열
    , String reqType      // [SYS081] 01:신규가입 / 02:재입장
    , String reqDtime     // YYYY-MM-DD HH:mm
) {
}
