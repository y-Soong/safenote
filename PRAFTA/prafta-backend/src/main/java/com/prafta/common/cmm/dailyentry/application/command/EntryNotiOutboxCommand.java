package com.prafta.common.cmm.dailyentry.application.command;

/**
 * 입장 승인요청 발생 시 사업장 관리자 대상 푸시 outbox INSERT 커맨드 (tb_noti_outbox, 대상자별 1행).
 *
 * <p>prafta-031 outbox 인프라 재사용(nearmiss01 NotiOutboxCommand 패턴 미러).
 * NOTI_TYPE='DAILY_ENTRY_REQ', CHANNEL='PUSH', SEND_STATUS='PENDING'. DEDUP_KEY 로 중복발송 차단.
 * <p>NOTI_ID 는 매퍼에서 회사+당일 기준 채번('N'+YYYYMMDD+3자리)으로 산출한다.
 */
public record EntryNotiOutboxCommand(
    String cmpnyCd
    , String siteCd
    , String targetUserCd
    , String notiType
    , String title
    , String body
    , String dataPayload
    , String dedupKey
    , String insertNo
) {
}
