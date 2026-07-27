package com.prafta.common.cmm.dailyentry.result;

/**
 * 입장 승인요청 목록 행 (웹 User_08 / 앱 관리자 공용).
 *
 * <p>휴대폰: 앱은 LAST4 파생값으로 마스킹("***-****-XXXX") 노출,
 * 웹 User_08 은 ENC 를 복호화해 평문 노출한다(2026-07-17 결정 — User_05 관리 화면 전례 미러).
 * 평문 자체는 조회/로그 금지.
 * <p>⚠️ MyBatis record 매핑 — SELECT 컬럼 순서와 컴포넌트 순서가 일치해야 한다.
 */
public record EntryRequestRow(
    String reqId
    , String siteCd
    , String userCd
    , String userNm
    , String mblNoLast4
    , String mblNoEnc      // AES-GCM 암호문(v1.base64url) — 웹 관리 화면 평문 표시용, 계정 삭제 시 NULL
    , String reqType       // [SYS081] 01:신규가입 / 02:재입장
    , String reqStatus     // [SYS082] 01:대기 / 02:승인 / 03:거부 / 04:만료 / 05:소진
    , String reqDtime      // YYYY-MM-DD HH:mm
    , String procUserCd
    , String procUserNm
    , String procDtime     // YYYY-MM-DD HH:mm
    , String rejectReason  // 내부 기록용(관리 화면 한정 노출 — 일용직에게 미노출)
    // ── 확정 계약서 표시(승인시점 버전확정 T4 / K9) — 신규 컴포넌트는 반드시 맨 뒤에 append.
    //    pin 은 관리자의 확정 행위이므로, 무엇을 확정하는지 보지 않고 승인하게 두면 안 된다.
    , Integer pinnedContractVer      // 자기 행 pin(승인/소진 행에서 유효). ★NULL=레거시 / 0=승인 시점 미등록
                                     //   — 두 값은 화면에서 다르게 표기되므로 정규화(한쪽으로 뭉개기) 금지
    , String pinnedFormatType        // pin 버전 형식 'PDF'|'IMG'(pin 버전 행이 없으면 null)
    , Integer activeContractVer      // 사업장 활성 버전(대기 행이 승인되면 확정될 버전). 미등록이면 null
    , String activeFormatType        // 활성 버전 형식 'PDF'|'IMG'. 미등록이면 null
    , Integer lastSignedContractVer  // 해당 계정의 최종 서명 버전(없으면 null) — "재서명 대상" 판정 재료
) {
}
