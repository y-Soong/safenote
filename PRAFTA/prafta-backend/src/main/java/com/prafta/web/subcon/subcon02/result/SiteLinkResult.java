package com.prafta.web.subcon.subcon02.result;

/**
 * 사업장 연동 링크 목록 1행(자사 관점 — PRAFTA-SUBCON-T2 §5-1).
 *
 * <p>체인 출처 표시는 직상위 제공사까지만(otherCmpny* = 직상위/직하위 상대 — 조상 정보 응답 금지).
 * direction: SENT(자사=제공측) / RECEIVED(자사=수신측) — SQL CASE 산출.
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record SiteLinkResult(
    Long linkId
    , String direction
    , String otherCmpnyCd
    , String otherCmpnyNm
    , String srcSiteCd
    , String srcSiteNm
    , String dstSiteCd
    , String dstSiteNm
    , String status
    , String insertDate
    , String processDtime
    , String processComment
    // PRAFTA-SUBCON-T6-02: 점검 구성 연동 상태(NONE/ACTIVE) + 실행/해제 일시.
    //   record 위치 매핑이라 신규 필드는 반드시 말미에 추가하고 SELECT 말미에도 같은 순서로 추가한다.
    , String chkptLinkStatus
    , String chkptLinkDtime
){
}
