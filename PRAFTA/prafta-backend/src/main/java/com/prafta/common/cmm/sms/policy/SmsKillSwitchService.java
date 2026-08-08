package com.prafta.common.cmm.sms.policy;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.audit.AuditActionType;
import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.common.cmm.audit.AuditResourceType;
import com.prafta.common.cmm.audit.command.AuditLogCommand;
import com.prafta.common.cmm.audit.service.AuditLogService;
import com.prafta.common.cmm.sms.policy.mapper.SmsSendPolicyMapper;
import com.prafta.platform.common.PlatformConstants;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SMS 전역 상한 초과 시 킬스위치를 발동시키는 전용 빈(SMS2-B3).
 *
 * <p>★★<b>[3차 / sec N-1 · qa Q-1] 트랜잭션 전파를 {@code REQUIRES_NEW} → 기본 {@code REQUIRED} 로 되돌렸다.</b>
 *    2차 코드는 {@link SmsRateLimitGuard} 가 정책행을 {@code SELECT ... FOR UPDATE} 로 배타 잠금한
 *    <b>바로 그 트랜잭션 안에서</b> 이 메서드를 {@code REQUIRES_NEW} 로 호출했다.
 *    {@code REQUIRES_NEW} 는 <b>별도 커넥션</b>을 잡으므로, 부모가 쥐고 있는 같은 행을 UPDATE 하려다
 *    자기 자신의 잠금을 기다리는 <b>자기 교착</b>이 된다.
 *    InnoDB 데드락 감지는 트랜잭션 간 wait-for 사이클을 찾는데 부모는 "대기 중" 이 아니라
 *    애플리케이션 스레드에서 자식 반환을 기다리는 상태다 → 사이클이 없어 1213 이 아니라
 *    {@code innodb_lock_wait_timeout}(기본 50초) 만료 후 <b>1205</b> 로 실패했다.
 *    결과: 킬스위치는 영원히 발동하지 않고, 요청당 커넥션 2~3개를 50초 점유해
 *    과금 폭주 국면에서 오히려 전 서비스를 무너뜨렸다.
 *
 * <p>★<b>절대 규칙</b>: {@code SmsRateLimitGuard} 가 정책행 잠금을 보유한 동안,
 *    <b>같은 행을 다른 커넥션으로 UPDATE 하지 않는다</b>. 잠금 보유자 본인이 UPDATE 해야 한다.
 *    이 클래스에 {@code REQUIRES_NEW}(또는 새 커넥션을 잡는 어떤 장치든)를 다시 붙이지 말 것.
 *
 * <p>★{@code REQUIRES_NEW} 의 원래 근거였던 "호출자 롤백 시 발동 기록이 사라진다" 는
 *    일반론으로는 옳지만 <b>이 경우엔 성립하지 않는다</b>.
 *    호출자가 롤백되면 그 요청의 인증코드 INSERT 도 함께 사라져 <b>발송 자체가 없었던 것</b>이 되기 때문이다.
 *    남길 기록도 막을 발송도 없다.
 *
 * <p>★{@link SmsRateLimitGuard} 안의 메서드로 두지 않고 별도 빈으로 유지하는 이유:
 *    ①발동 판정/로그/감사로그라는 별개 책임이고 ②{@link com.prafta.common.cmm.audit.service.AuditLogService}
 *    가 자체적으로 {@code REQUIRES_NEW} 라 감사로그만 독립 커밋되는 구조를 그대로 쓸 수 있다
 *    (감사로그는 {@code TB_AUDIT_LOG} 라 정책행과 잠금 충돌이 없다).
 *
 * <p>★<b>자동 해제 금지</b>. 이 클래스는 물론 코드 어디에도 {@code KILL_SWITCH_YN='N'} 으로 되돌리는 경로를
 *    만들지 않는다. 원인을 확인하지 않은 자동 재개는 과금 재폭발이다.
 *    해제는 Platform_05 화면의 전용 API(운영자 수동)만 가능하다.
 *    재기동 내성은 상태를 DB 에 두는 것으로 자동 충족된다(메모리 상태였다면 재기동으로 풀린다).
 *
 * <p>★운영 알림 채널은 신설하지 않았다(워크스페이스에 운영자 알림 채널이 없다 — FCM 은 앱 사용자 대상).
 *    대신 grep 가능한 고정 프리픽스 {@code [SMS킬스위치]} 의 ERROR 로그 + 감사로그 1건 +
 *    Platform_05 화면 최상단 경고 배너로 가시화한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmsKillSwitchService {

    private final SmsSendPolicyMapper smsSendPolicyMapper;
    private final AuditLogService auditLogService;

    /**
     * 전역 시간당 상한 초과로 킬스위치를 발동시킨다.
     *
     * <p>멱등: 이미 발동 상태면 UPDATE 가 0행이라 로그·감사로그를 남기지 않는다
     * (초당 다발 발동 시 로그 폭주 방지).
     *
     * <p>★전파는 기본 {@code REQUIRED} 다 — 호출자({@link SmsRateLimitGuard})가 정책행을 이미
     *    배타 잠금하고 있으므로 <b>같은 트랜잭션/같은 커넥션</b>에서 UPDATE 해야 한다.
     *    사유 전체는 클래스 주석의 [3차 / sec N-1] 절 참조. {@code REQUIRES_NEW} 로 되돌리지 말 것.
     *
     * @param cnt   최근 1시간 실발송 건수
     * @param limit 전역 시간당 상한
     */
    @Transactional(rollbackFor = Exception.class)
    public void fire(int cnt, int limit) {

        // ★사유 문자열에 PII 를 넣지 않는다(건수와 상한만).
        String reason = "전역 시간당 상한 초과(" + cnt + "/" + limit + ")";

        int updated = smsSendPolicyMapper.fireKillSwitch(reason);
        if (updated == 0) {
            // 이미 발동 상태 — 정상 경로다. 조용히 반환한다.
            return;
        }

        log.error("[SMS킬스위치] 발동 - 최근 1시간 {}건 / 상한 {}건. 발송을 전면 중지한다."
                + " 해제는 Platform_05(SMS 발송 관리) 화면에서 수동으로만 가능하다.", cnt, limit);

        // 과금 직결 상태 변화이므로 감사로그를 남긴다(공통 정책서 §11.1·§11.3).
        // 자동 발동이라 행위자(userCd)는 없다. 적재 실패는 AuditLogServiceImpl 내부에서 흡수된다.
        // ★AuditLogServiceImpl.record 는 자체적으로 REQUIRES_NEW 라 별도 커넥션을 잡는다.
        //   대상이 TB_AUDIT_LOG 이므로 위에서 잡고 있는 TB_SMS_SEND_POLICY 잠금과 충돌하지 않는다.
        //   ★향후 이 안쪽에서 TB_SMS_SEND_POLICY 를 읽거나 쓰면 N-1 자기 교착이 그대로 재발한다.
        auditLogService.record(
                AuditLogCommand.builder()
                        .cmpnyCd(PlatformConstants.PLATFORM_CMPNY_CD)
                        .userCd(null)
                        .actionType(AuditActionType.SETTING_CHANGE)
                        .resourceType(AuditResourceType.SMS_KILL_SWITCH)
                        .resourceKey("FIRE")
                        .detailJson("{\"cnt\":" + cnt + ",\"limit\":" + limit + "}")
                        .build(),
                new AuditContext(null, null)
        );
    }
}
