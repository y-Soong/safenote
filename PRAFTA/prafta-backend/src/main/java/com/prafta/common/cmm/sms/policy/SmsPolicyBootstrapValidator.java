package com.prafta.common.cmm.sms.policy;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.prafta.common.cmm.sms.policy.mapper.SmsSendPolicyMapper;
import com.prafta.common.cmm.sms.policy.mapper.SmsVerifyLimitMapper;
import com.prafta.common.config.SmsProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 기동 시 SMS 상한 정책행/검증 스키마를 1회 검증한다 — 3차 / sec N-9, 4차 / qa R-4b · sec T-7.
 *
 * <h3>★왜 필요한가</h3>
 * {@link SmsRateLimitGuard} 는 정책행이 없으면 <b>차단하지 않고 통과</b>시킨다(fail-open).
 * 시드 누락으로 5개 인증 흐름이 통째로 죽는 사고를 막기 위한 의도적 선택이지만,
 * 그 결과가 "발송 상한이 전면 무력화된 상태로 조용히 운영되는 것" 이다.
 * 이 워크스페이스에는 <b>"마이그레이션을 개발에만 적용하고 운영을 빠뜨리는" 선례가 실제로 있다</b>
 * (메모리 {@code feedback_db_migration_apply_both_envs}). 실수 하나로 1차 Critical 이 그대로 재현된다.
 *
 * <h3>★어떻게 처리하는가</h3>
 * 정책행이 없으면 발송 게이트({@code prafta.sms.enabled})를 <b>강제로 내린다</b>
 * ({@code SmsProperties.forceDisable} — 설정값은 보존하고 별도 플래그만 세운다. 4차 / sec T-7).
 * fail-open 의 결과가 "상한 없이 발송" 이 아니라 <b>"발송 안 함(SKIPPED)"</b> 이 되어 안전 방향으로 무너진다.
 * <p>★<b>부팅은 깨뜨리지 않는다.</b> {@code SmsClientConfig} 의 https 미충족 처리와 동일한 패턴이다
 * (요청서 1차 §4-2 — 키 미설정 환경에서 부팅이 깨지면 안 된다).
 *
 * <h3>★★[4차 / qa R-4b] 3차 검증의 두 구멍을 메웠다</h3>
 * <ol>
 *   <li><b>검증(대입) 방어 스키마를 보지 않았다.</b> {@code selectPolicyNoLock} 은 {@code VERIFY_*} 컬럼을
 *       하나도 읽지 않고 {@code TB_SMS_VERIFY_ATTEMPT} 존재도 확인하지 않아, <b>2차 DDL 만 적용하고
 *       3차를 빠뜨린 환경이 부팅 검증을 정상 통과</b>했다. 실제 증상은 첫 검증 요청에서 1054/1146 → 500
 *       (회원가입 본인인증 / 계정찾기 / 비밀번호재설정 / 일용직 셀프가입 / 앱 휴대폰변경 전부).</li>
 *   <li><b>게이트 OFF 면 아예 실행되지 않았다.</b> 현재 운영이 {@code PPURIO_ENABLED=false} 라
 *       N-9 의 보호가 사실상 꺼져 있었다. 게다가 <b>대입 방어는 게이트와 무관하게 항상 동작</b>하므로
 *       검증 스키마 확인은 게이트 상태와 아무 상관이 없다.</li>
 * </ol>
 * → 검증 스키마 확인은 <b>항상</b> 수행하고, 발송 정책행 확인만 게이트 ON 일 때 의미를 갖는다
 *   (게이트 OFF 여도 확인은 하되 강제 OFF 는 무의미하므로 로그만 남긴다).
 *
 * <h3>★{@code ApplicationReadyEvent} 인 이유 / 그 한계</h3>
 * {@code @PostConstruct} 시점에는 DataSource·MyBatis 배선이 끝났다고 보장할 수 없다.
 * 준비 완료 후 1회만 조회하며, 실패하더라도 예외를 밖으로 던지지 않는다(기동 차단 금지).
 * <p>★한계(sec T-7): 이 이벤트는 <b>요청 수신이 시작된 뒤</b> 발화하므로 그 사이의 짧은 창에서는
 * 검증이 아직 안 끝난 상태로 발송이 나갈 수 있다. 또 기동 1회뿐이라 <b>운영 중</b> 정책행이 사라지는
 * 경우는 잡지 못한다. 두 구멍은 발송 경로({@code SmsRateLimitGuard})가 정책행 null 을 만나는 즉시
 * 스스로 게이트를 내리는 것으로 메웠다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsPolicyBootstrapValidator {

    private final SmsSendPolicyMapper smsSendPolicyMapper;
    private final SmsVerifyLimitMapper smsVerifyLimitMapper;
    private final SmsProperties smsProperties;

    @EventListener(ApplicationReadyEvent.class)
    public void validateOnStartup() {
        // ★순서 주의: 검증(대입) 방어 스키마가 먼저다. 게이트 OFF 여도 반드시 확인해야 한다.
        validateVerifySchema();
        validateSendPolicy();
    }

    /**
     * 검증(대입) 방어 스키마 확인. [4차 / qa R-4b]
     *
     * <p>★게이트 상태와 무관하게 <b>항상</b> 실행한다 — 대입 방어는 문자를 보내지 않아도 동작하는 경로다.
     * <p>★여기서 발송 게이트를 내리지 <b>않는다</b>. 발송을 꺼도 검증 EP 는 그대로 열려 있어 아무 도움이 안 되고,
     *    가드가 폴백 임계값으로 계속 방어하므로 "시끄럽게 알리고 계속 동작" 이 올바른 선택이다.
     */
    private void validateVerifySchema() {

        SmsVerifyPolicy verifyPolicy;
        try {
            verifyPolicy = smsVerifyLimitMapper.selectVerifyPolicy();
        } catch (RuntimeException e) {
            log.error("[SMS검증상한] VERIFY_* 컬럼 조회 실패 - 대입 방어가 폴백 임계값({}/{}/{})으로 동작한다."
                    + " prafta-sms-ppurio-3(VERIFY_FAIL_LIMIT/VERIFY_LOCK_SEC/VERIFY_HOUR_LIMIT)"
                    + " 마이그레이션이 이 환경에 적용됐는지 확인할 것. 예외={}",
                    SmsVerifyPolicy.DEFAULT_FAIL_LIMIT, SmsVerifyPolicy.DEFAULT_LOCK_SEC,
                    SmsVerifyPolicy.DEFAULT_HOUR_LIMIT, e.getClass().getSimpleName());
            verifyPolicy = null;
        }

        if (verifyPolicy == null) {
            log.error("[SMS검증상한] 검증 방어 임계값을 DB 에서 읽지 못했다 - 폴백으로 동작 중."
                    + " 정책행 시드(prafta-sms-ppurio-2) 및 VERIFY_* 컬럼(prafta-sms-ppurio-3) 을 확인할 것");
        } else {
            log.info("[SMS검증상한] 검증 방어 임계값 확인 - 실패허용={}회, 잠금={}초, 시간당 실패시도 상한={}건",
                    verifyPolicy.verifyFailLimit(), verifyPolicy.effectiveLockSec(), verifyPolicy.verifyHourLimit());
        }

        try {
            smsVerifyLimitMapper.probeVerifyAttemptTable();
            log.info("[SMS검증상한] TB_SMS_VERIFY_ATTEMPT 확인 완료");
        } catch (RuntimeException e) {
            // ★1146(테이블 부재) / 1054(컬럼 부재). 3차까지는 이 상태가 기동 시 전혀 드러나지 않고
            //   첫 검증 요청에서 500 으로 터졌다(qa R-4).
            log.error("[SMS검증상한] TB_SMS_VERIFY_ATTEMPT 접근 실패 - 시간당 실패 시도 상한이 동작하지 않는다."
                    + " (가드는 조회 실패를 '상한 초과' 로 보수적 처리하므로, 오답을 낸 요청은 전부 차단된다.)"
                    + " prafta-sms-ppurio-3 마이그레이션 적용 여부를 확인할 것. 예외={}",
                    e.getClass().getSimpleName());
        }
    }

    /** 발송 상한 정책행 확인 + 부재 시 발송 게이트 강제 OFF(sec N-9). */
    private void validateSendPolicy() {

        SmsSendPolicy policy;
        try {
            policy = smsSendPolicyMapper.selectPolicyNoLock();
        } catch (RuntimeException e) {
            // 테이블 자체가 없는 경우(1146)도 여기로 온다 = 마이그레이션 미적용.
            // ★기동을 막지 않는다. 대신 발송만 끈다.
            forceDisableSend("SMS 상한 정책행 조회 실패(" + e.getClass().getSimpleName() + ")");
            log.error("[SMS상한] 정책행 조회 실패 - 발송 게이트를 강제 OFF 한다(전 흐름 SKIPPED)."
                    + " prafta-sms-ppurio-2/3 마이그레이션이 이 환경에 적용됐는지 확인할 것. 예외={}",
                    e.getClass().getSimpleName());
            return;
        }

        if (policy == null) {
            forceDisableSend("SMS 상한 정책행 부재(POLICY_ID='DEFAULT')");
            log.error("[SMS상한] 정책행 부재(TB_SMS_SEND_POLICY.POLICY_ID='DEFAULT')"
                    + " - 발송 게이트를 강제 OFF 한다(전 흐름 SKIPPED)."
                    + " 상한 없이 발송되는 것보다 발송하지 않는 쪽이 안전하다."
                    + " prafta-sms-ppurio-2-rate-limit-policy.sql 의 시드 INSERT 를 적용한 뒤 재기동할 것");
            return;
        }

        if (policy.globalHourLimit() <= 0) {
            // ★차단하지는 않는다(운영자가 의도적으로 무제한을 택했을 수 있다). 다만 킬스위치가 무력화된 상태다.
            log.error("[SMS상한] GLOBAL_HOUR_LIMIT={} (무제한) - 킬스위치가 영구 무력화된 상태다."
                    + " Platform_05 화면에서 1 이상으로 교정할 것", policy.globalHourLimit());
        }

        log.info("[SMS상한] 정책행 확인 - 창={}초, 번호={}/{}, IP축={}({}/{}), 사용자={}/{}, 전역={}, 킬스위치={}",
                policy.phoneWindowSec(), policy.phoneHourLimit(), policy.phoneDayLimit(),
                policy.ipAxisEnabledYn(), policy.ipHourLimit(), policy.ipDayLimit(),
                policy.userHourLimit(), policy.userDayLimit(),
                policy.globalHourLimit(), policy.killSwitchYn());
    }

    /**
     * 발송 게이트 강제 OFF. [4차 / sec T-7]
     *
     * <p>★게이트가 이미 설정상 OFF 여도 플래그는 세운다. Platform_05 가
     *    "설정 OFF" 와 "시스템 강제 OFF" 를 구분해 보여줘야 운영자가 원인을 바로 짚을 수 있기 때문이다
     *    (07-31 {@code PUSH_WORKER_ENABLED} 오판 계열 — 화면이 원인을 숨기면 안 된다).
     */
    private void forceDisableSend(String reason) {
        smsProperties.forceDisable(reason);
    }
}
