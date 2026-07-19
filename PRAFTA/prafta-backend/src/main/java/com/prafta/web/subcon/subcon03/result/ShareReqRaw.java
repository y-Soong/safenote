package com.prafta.web.subcon.subcon03.result;

/**
 * 공유 요청 원시행(내부 전용 — 승인 트랜잭션/사전정보 조회의 서버 판단 근거).
 *
 * <p>호출 전 당사자 검증(조건부 UPDATE 선점 또는 PRV_CMPNY_CD=gv 조건)을 거친 경로에서만 사용한다.
 * record 매핑은 SELECT 컬럼 순서와 일치해야 한다.
 */
public record ShareReqRaw(
    Long shareReqId
    , Long relationId
    , String reqCmpnyCd
    , String reqSiteCd
    , String prvCmpnyCd
    , String targetSiteCd
    , String dataType
    , String periodStr
    , String periodEnd
    , String closedOnlyYn
    , String purpose
    , String status
){
}
