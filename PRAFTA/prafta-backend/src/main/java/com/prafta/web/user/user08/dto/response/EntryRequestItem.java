package com.prafta.web.user.user08.dto.response;

/**
 * 입장 승인요청 목록 항목 (User_08 탭1 테이블 행).
 * 휴대폰은 서버에서 복호화한 평문을 하이픈 포맷("010-XXXX-XXXX")으로 노출한다
 * (2026-07-17 마스킹 해제 결정 — User_05 관리 화면 전례 미러).
 * 거부 사유는 내부 기록용으로 관리 화면에만 노출한다(일용직 미노출).
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
) {
}
