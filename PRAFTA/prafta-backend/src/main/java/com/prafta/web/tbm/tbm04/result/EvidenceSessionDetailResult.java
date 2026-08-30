package com.prafta.web.tbm.tbm04.result;

/**
 * TBM 증빙 교육일지(건별) 세션 개요 1행 (시트3+ 헤더 영역).
 *
 * <p>contentBody 는 리치 HTML 원문 — 화면(엑셀 빌더)이 텍스트로 변환해 싣는다.
 * ⚠️ SELECT 컬럼 순서 = record 컴포넌트 순서(위치 매핑).
 */
public record EvidenceSessionDetailResult(
    String sessionCd
    , String title
    , String siteNm            // 개설 사업장명
    , String hostCmpnyNm       // 개설사명(자사 세션이면 null)
    , String startedAt         // yyyy-MM-dd HH:mm (KST)
    , String endedAt
    , Integer eduMinutes
    , String managerUserNm
    , String gpsVerifyTypeCd   // AUTO | MANUAL | DISABLED
    , Integer gpsVerifyRadiusM
    , String contentBody       // 교육 내용(리치 HTML)
    , String ownerYn           // 'Y'=자사 개설
){
}
