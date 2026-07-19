package com.prafta.web.subcon.subcon03.result;

/**
 * 공유 요청 후보 — 선택한 제공사와 사업장 연동 체인이 있는 <b>내</b> 사업장 1건(PRAFTA-SUBCON-T3 §5-2).
 *
 * <p>제공측 대응 사업장 코드(TARGET_SITE_CD)는 응답하지 않는다 — 요청 생성 시 서버가 재해석한다
 * (제공사 사업장 목록 비노출 + 클라 입력 불신).
 * chainDirection: PROVIDE(내가 원본 → 제공사가 미러) / RECEIVE(제공사가 원본 → 내가 미러).
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record ChainSiteResult(
    String siteCd
    , String siteNm
    , String chainDirection
){
}
