package com.prafta.web.subcon.subcon01.application.command;

/**
 * 연동 관계 이력(HIST) INSERT 커맨드.
 *
 * <p>모든 상태 전이는 본 이력과 동일 트랜잭션으로 기록된다(plan §7-1 공통 규칙).
 * actionCmpnyCd 는 토큰 gv_cmpnyCd 만 주입(Q1 승인 — 감사 추적용 행위자 소속 회사).
 */
public record RelationHistInsertCommand(
    Long relationId
    , String actionType     // REQUEST / ACCEPT / REJECT / CANCEL / TERMINATE
    , String actionCmpnyCd
    , String actionUserCd
    , String actionDesc
    , String insertNo
){
}
