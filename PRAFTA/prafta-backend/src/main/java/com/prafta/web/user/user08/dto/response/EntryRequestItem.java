package com.prafta.web.user.user08.dto.response;

/**
 * 입장 승인요청 목록 항목 (User_08 탭1 테이블 행).
 * 휴대폰은 서버에서 복호화한 평문을 하이픈 포맷("010-XXXX-XXXX")으로 노출한다
 * (2026-07-17 마스킹 해제 결정 — User_05 관리 화면 전례 미러).
 * 거부 사유는 내부 기록용으로 관리 화면에만 노출한다(일용직 미노출).
 *
 * <p>확정 계약서 5필드(승인시점 버전확정 T4 / K9): 대기 행은 {@code activeContractVer}(승인 시 확정될 버전),
 * 승인/소진 행은 {@code pinnedContractVer}(이미 확정된 버전)를 표시한다. {@code lastSignedContractVer} 와
 * 다르거나 미서명이면 화면에서 "재서명 대상" 배지를 붙인다(판정은 화면 책임 — 서버는 값만 준다).
 * {@code FILE_MGMT_CD} 등 내부 식별자는 포함하지 않는다.
 */
public record EntryRequestItem(
    String reqId
    , String siteCd
    , String userCd
    , String userNm
    , String mblNo         // 평문 하이픈 포맷(복호화 실패/없음 시 "-")
    , String reqType       // [SYS081] 01:신규가입 / 02:재입장
    , String reqStatus     // [SYS082] 01:대기 / 02:승인 / 03:거부 / 04:만료 / 05:소진
    , String reqDtime      // YYYY-MM-DD HH:mm
    , String procUserNm
    , String procDtime     // YYYY-MM-DD HH:mm
    , String rejectReason
    , Integer pinnedContractVer      // 승인 시점 확정 버전. ★null=레거시(pin 도입 전) / 0=승인 시 미등록
    , String pinnedFormatType        // 'PDF' | 'IMG' (pin 버전 행 없으면 null)
    , Integer activeContractVer      // 사업장 활성 버전(미등록이면 null → 화면 "미등록")
    , String activeFormatType        // 'PDF' | 'IMG' (미등록이면 null)
    , Integer lastSignedContractVer  // 최종 서명 버전(미서명이면 null)
) {
}
