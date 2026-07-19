package com.prafta.web.subcon.subcon03.result;

/**
 * 데이터 공유 요청 목록 1행(자사 관점 — PRAFTA-SUBCON-T3 §5-1).
 *
 * <p>상대사 행위자 인명/사용자코드는 응답에 포함하지 않는다(T1 Q4 승계).
 * siteNm 은 <b>항상 자기 테넌트 사업장명</b>이다(보낸 요청 = REQ_SITE_CD, 받은 요청 = TARGET_SITE_CD).
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record ShareReqResult(
    Long shareReqId
    , String direction
    , String otherCmpnyCd
    , String otherCmpnyNm
    , String dataType
    , String siteNm
    , String periodStr
    , String periodEnd
    , String closedOnlyYn
    , String purpose
    , String status
    , String insertDate
    , String processDtime
    , String processComment
    , Integer snapshotVersion
){
}
