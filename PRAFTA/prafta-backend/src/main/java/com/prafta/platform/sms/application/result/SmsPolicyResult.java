package com.prafta.platform.sms.application.result;

/**
 * SMS 발송 정책 + 킬스위치 상태(Platform_05 화면 조회용).
 *
 * <p>{@code common.cmm.sms.policy.SmsSendPolicy}(판정 전용, 잠금 조회)와 별도로 둔 이유:
 * 화면은 킬스위치 발동/해제 이력까지 보여줘야 하고, 판정 경로는 그 필드가 필요 없다.
 * 판정용 record 에 화면 전용 필드를 끼워 넣으면 잠금 구간의 SELECT 가 불필요하게 넓어진다.
 *
 * <p>★record 매핑은 SELECT 컬럼 순서에 의존한다 — {@code PlatformSmsMapper.xml} 의
 * {@code selectPolicy} SELECT 순서와 함께 유지할 것.
 *
 * <p>★[2026-08-30] 킬스위치 시각 2종은 epoch 초({@code Long})다. 운영 DB 시계가 UTC 라
 * DATE_FORMAT 문자열 그대로는 KST-9h 로 표시됐다. 이 record 는 {@code SmsConsoleResponse} 에
 * 그대로 임베드되므로 화면({@code Platform_05.vue})이 Asia/Seoul 로 포맷한다.
 */
public record SmsPolicyResult(
    int phoneWindowSec
    , int phoneHourLimit
    , int phoneDayLimit
    , String ipAxisEnabledYn
    , int ipHourLimit
    , int ipDayLimit
    , int userHourLimit
    , int userDayLimit
    , int globalHourLimit
    , String killSwitchYn
    , Long killSwitchAtEpoch
    , String killSwitchReason
    , Long killSwitchReleaseAtEpoch
    , String killSwitchReleaseNo
) {
}
