package com.prafta.app.nearmiss.nearmiss01.application.command;

/**
 * 푸시 알림 outbox INSERT Command (tb_noti_outbox, 대상자별 1행).
 *
 * <p>prafta-031 outbox 인프라 재사용. NOTI_TYPE='NEAR_MISS_REPORTED', CHANNEL='PUSH',
 *    SEND_STATUS='PENDING'(consumer 미구현 → 적재까지만). DEDUP_KEY 로 중복발송 차단.
 * <p>NOTI_ID 는 매퍼에서 회사+당일 기준 채번('N'+YYYYMMDD+3자리)으로 산출한다.
 * <p>dataPayload 는 서비스에서 Jackson ObjectMapper 로 직렬화한 JSON 문자열(수동 조립 금지).
 */
public record NotiOutboxCommand(
    String cmpnyCd
    , String siteCd
    , String targetUserCd
    , String notiType
    , String title
    , String body
    , String dataPayload
    , String dedupKey
    , String insertNo
){
}
