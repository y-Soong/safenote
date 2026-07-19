package com.prafta.common.cmm.dailyentry.application.command;

/**
 * 일용직 입장 승인요청 INSERT 커맨드 (TB_DAILY_ENTRY_REQUEST).
 *
 * <p>REQ_STATUS 는 '01'(대기) 고정으로 XML 에서 적재한다. 비로그인(가입/로그인 시도) 흐름에서
 * 생성되므로 INSERT_NO 는 대상 일용직 USER_CD 를 사용한다(dailyjoin 관례 미러).
 */
public record EntryRequestInsertCommand(
    String cmpnyCd
    , String reqId
    , String siteCd
    , String userCd
    , String reqType    // [SYS081] 01:신규가입 / 02:재입장
) {
}
