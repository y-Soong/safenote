package com.prafta.app.entryadmin.entryadmin01.dto.response;

/**
 * 앱 관리자 입장 승인 대기 목록 항목 (UI-DC-03 카드).
 * 휴대폰은 서버에서 last4 마스킹("***-****-XXXX")된 문자열만 노출한다(평문/ENC 금지).
 *
 * <p>확정 계약서 필드(승인시점 버전확정 T4 / K9 — 웹 User_08 과 동일 기준): 앱 대기 목록은 '01' 만
 * 노출하므로 실질적으로 {@code activeContractVer}(승인 시 확정될 버전)가 표시 대상이고
 * {@code pinnedContractVer} 는 항상 null 이다. 웹과 정보 비대칭이 생기지 않게 같은 필드를 내려 준다.
 * 앱 화면 반영은 후속 라운드(AAB 재빌드 필요) — 구버전 앱은 미사용 필드를 무시하므로 무해하다.
 */
public record EntryPendingItem(
    String reqId
    , String userNm
    , String mblNo        // 마스킹 문자열
    , String reqType      // [SYS081] 01:신규가입 / 02:재입장
    , String reqDtime     // YYYY-MM-DD HH:mm
    , Integer pinnedContractVer      // 대기 행이므로 통상 null. ★null(레거시)과 0(미등록)을 뭉개지 않는다
    , String pinnedFormatType        // 'PDF' | 'IMG'
    , Integer activeContractVer      // 사업장 활성 버전(미등록이면 null → 화면 "미등록")
    , String activeFormatType        // 'PDF' | 'IMG'
    , Integer lastSignedContractVer  // 최종 서명 버전(미서명이면 null)
) {
}
