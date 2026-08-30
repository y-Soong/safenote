package com.prafta.web.user.user05.result;

/**
 * 일일사용자 계약서 서명 이력 1행 (TB_DAILY_CONTRACT_SIGN 기준, User_05 계약이력 팝업).
 *
 * <p>contractNm 은 서명 시점 버전(TB_DAILY_CONTRACT LEFT JOIN)의 계약서명 — 양식이 삭제된 적 없는
 * 설계(교체 시 USE_YN='N')라 통상 존재하나, 방어적으로 null 허용(화면 '-').
 * ⚠️ SELECT 컬럼 순서 = record 컴포넌트 순서(위치 매핑).
 */
public record DailyContractSignHisResult(
    String signId
    , Integer contractVer
    , String contractNm
    , String signDtime
    , String firstWorkDate
    , String reqId
){
}
