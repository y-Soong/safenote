package com.prafta.common.cmm.dailyentry.result;

/**
 * 일용직 로그인 시 입장 승인 판정 결과.
 *
 * <p>{@code reqId} 는 {@link EntryLoginDecisionType#APPROVED} 일 때만 유효(소진 대상 승인요청 ID).
 * 그 외 유형에서는 null 일 수 있다.
 */
public record EntryLoginDecision(
    EntryLoginDecisionType type
    , String reqId
) {
}
