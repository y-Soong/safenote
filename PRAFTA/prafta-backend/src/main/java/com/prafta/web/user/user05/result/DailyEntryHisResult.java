package com.prafta.web.user.user05.result;

/**
 * 일일사용자 입장 승인요청/로그인 이력 1행 (TB_DAILY_ENTRY_REQUEST 기준, User_05 계약이력 팝업).
 *
 * <p>contractVer 는 승인 시점 pin — {@code 0}="승인 시점 활성 계약서 미등록"(K4 센티넬),
 * {@code null}=pin 도입 전 레거시 승인. consumeDtime 이 있고 pin=0 이며 연결 서명이 없으면
 * "계약서 미등록 상태 로그인"이다(요구: 계약서 없이 로그인된 경우도 이력 노출).
 * signYn 은 이 요청(REQ_ID)에 연결된 서명본 존재 여부('Y'/'N').
 * ⚠️ SELECT 컬럼 순서 = record 컴포넌트 순서(위치 매핑).
 */
public record DailyEntryHisResult(
    String reqId
    , String reqType
    , String reqStatus
    , String reqDtime
    , String procDtime
    , String consumeDtime
    , Integer contractVer
    , String signYn
){
}
