package com.prafta.common.cmm.sms.policy;

/**
 * SMS 발송 상한 정책 스냅샷(TB_SMS_SEND_POLICY 단일행 'DEFAULT').
 *
 * <p>★임계값은 전부 DB 값이다. 코드 어디에도 하드코딩하지 않는다(요청서 2차 §1-B "전부 DB 설정값").
 *    확정 수치의 유일한 출처는 {@code prafta-sms-ppurio-2-rate-limit-policy.sql} 의 컬럼 DEFAULT 이고,
 *    운영 중 변경은 Platform_05 화면이 담당한다.
 *
 * <p>★record 매핑은 SELECT 컬럼 순서에 의존한다(메모리 {@code feedback_mybatis_record_column_order}).
 *    {@code SmsSendPolicyMapper.xml} 의 {@code selectPolicyForUpdate} SELECT 순서와
 *    아래 컴포넌트 순서를 <b>항상 함께</b> 유지할 것.
 *
 * @param phoneWindowSec  번호별 연속 발송 최소 간격(초)
 * @param phoneHourLimit  번호별 시간당 상한(0 이하 = 무제한)
 * @param phoneDayLimit   번호별 일별 상한(0 이하 = 무제한)
 * @param ipAxisEnabledYn IP축 실차단 여부(Y = 차단 / N = 관측만). ★1단계는 'N'
 * @param ipHourLimit     IP별 시간당 상한(0 이하 = 무제한)
 * @param ipDayLimit      IP별 일별 상한(0 이하 = 무제한)
 * @param userHourLimit   사용자별 시간당 상한(0 이하 = 무제한)
 * @param userDayLimit    사용자별 일별 상한(0 이하 = 무제한)
 * @param globalHourLimit 전역 시간당 실발송 상한(0 이하 = 무제한). 초과 시 킬스위치 자동 발동
 * @param killSwitchYn    킬스위치 상태(Y = 발동 = 발송 전면중지)
 */
public record SmsSendPolicy(
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
) {

    /** 킬스위치 발동 상태 여부. */
    public boolean isKillSwitchFired() {
        return "Y".equals(killSwitchYn);
    }

    /**
     * IP 축이 실제 차단으로 동작하는지.
     *
     * <p>★기본은 false(관측 전용)다. 운영 nginx 의 X-Forwarded-For 구성(누적인지 덮어쓰기인지, 홉이 몇 개인지)을
     *    레포에서 정적으로 확인할 수 없기 때문이다. 홉 수를 잘못 잡은 채 차단하면
     *    ①전 사용자가 같은 버킷으로 묶여 서비스가 마비되거나 ②공격자 위조분이 채택되어 축이 무의미해진다.
     *    운영 진단 로그({@code [SMS상한:IP축진단]})로 실제 구조를 확인한 뒤 화면에서 켠다.
     */
    public boolean isIpAxisEnforced() {
        return "Y".equals(ipAxisEnabledYn);
    }
}
