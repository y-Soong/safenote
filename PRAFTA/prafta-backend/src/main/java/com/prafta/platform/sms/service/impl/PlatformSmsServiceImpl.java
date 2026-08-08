package com.prafta.platform.sms.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.prafta.common.cmm.audit.AuditActionType;
import com.prafta.common.cmm.audit.AuditContext;
import com.prafta.common.cmm.audit.AuditResourceType;
import com.prafta.common.cmm.audit.command.AuditLogCommand;
import com.prafta.common.cmm.audit.service.AuditLogService;
import com.prafta.common.cmm.sms.policy.mapper.SmsSendPolicyMapper;
import com.prafta.common.config.SmsProperties;
import com.prafta.common.error.platform.PlatformErrorCode;
import com.prafta.common.exception.ApiException;
import com.prafta.platform.common.PlatformConstants;
import com.prafta.platform.sms.application.param.SmsPolicyUpdateParam;
import com.prafta.platform.sms.application.result.SmsPolicyResult;
import com.prafta.platform.sms.dto.response.SmsConsoleResponse;
import com.prafta.platform.sms.mapper.PlatformSmsMapper;
import com.prafta.platform.sms.service.PlatformSmsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Platform_05(SMS 발송 관리) 서비스 구현.
 *
 * <p>★캐시를 두지 않는다(요청서 2차 C-3 택일 결과 = "캐시 미도입 = 수정 즉시 반영").
 *    ①차단 판정 경로가 어차피 정책행을 잠금 조회하므로 캐시로 절약되는 쿼리가 0이고
 *    ②발송 볼륨이 극도로 낮으며(전 기간 누적 272행, 전역 상한도 시간당 500건)
 *    ③프로젝트 선례가 무캐시이고({@code AiQuotaServiceImpl.isExceeded()} 는 호출마다 직접 읽는다)
 *    ④"즉시 반영" 이 이 화면의 존재 이유다.
 *    무캐시라 다중 인스턴스로 스케일아웃해도 정합성이 자동 유지된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformSmsServiceImpl implements PlatformSmsService {

    private final PlatformSmsMapper platformSmsMapper;

    /**
     * ★전역 소진 건수는 판정 경로와 <b>같은 쿼리</b>를 재사용한다.
     *   화면과 킬스위치가 다른 규칙으로 세면 "화면은 여유 있다는데 발송이 막힌" 상황이 생긴다.
     */
    private final SmsSendPolicyMapper smsSendPolicyMapper;

    /** 발송 게이트 현재값(읽기전용 표시용). ★화면에서 바꾸지 않는다 — secrets 소관. */
    private final SmsProperties smsProperties;

    private final AuditLogService auditLogService;

    /** 현황 카드 집계 구간(시간). */
    private static final int STAT_HOURS_1H = 1;
    private static final int STAT_HOURS_24H = 24;

    @Override
    public SmsConsoleResponse selectConsole() {

        SmsPolicyResult policy = platformSmsMapper.selectPolicy();

        return SmsConsoleResponse.builder()
                // ★[4차 / sec T-7] "설정값" 을 그대로 내려보낸다(강제 OFF 미반영).
                //   화면이 "설정 OFF" 와 "시스템이 강제로 내린 OFF" 를 구분해야 운영자가 원인을 바로 짚는다.
                .gateEnabled(smsProperties.isConfiguredEnabled())
                .gateForcedOff(smsProperties.isForcedOff())
                .gateForcedOffReason(smsProperties.getForcedOffReason())
                // ★[3차 / qa Q-7] 게이트 토글과 "실제 발송 가능" 을 분리해 내려보낸다.
                //   키 미주입 / base URL 이 http 면 화면은 ON 인데 전 흐름이 SKIPPED 다(PUSH_WORKER_ENABLED 오판 계열).
                //   ★sendable 은 "실효" 게이트(isEnabled = 설정 ON && 강제 OFF 아님)를 본다.
                .sendable(smsProperties.isEnabled() && smsProperties.getPpurio().isSendable())
                .policy(policy)
                .stat1h(platformSmsMapper.selectSendStat(STAT_HOURS_1H))
                .stat24h(platformSmsMapper.selectSendStat(STAT_HOURS_24H))
                .globalUsedCnt(smsSendPolicyMapper.selectGlobalSentCnt())
                .globalHourLimit(policy == null ? 0 : policy.globalHourLimit())
                .build();
    }

    /**
     * 임계값 수정.
     *
     * <p>★변경 전 값을 감사 로그에 담아야 하므로 UPDATE 와 같은 트랜잭션에서 SELECT 를 먼저 한다.
     *    (임계값은 과금 직결 설정이라 "누가 언제 무엇을 어떻게 바꿨는지" 가 남아야 한다 — 공통 정책서 §11.1·§11.3)
     *
     * <p>★★[4차 / sec T-9] {@code before} 를 <b>{@code FOR UPDATE} 로</b> 읽는다.
     *    3차는 비잠금 SELECT 라 "킬스위치 발동 중 상향 거부" 판정과 UPDATE 사이가 TOCTOU 였다 —
     *    저장과 발동이 겹치면 상향이 통과해 킬스위치가 무력화될 수 있었다.
     *    발동 측({@code SmsRateLimitGuard})도 같은 행을 {@code FOR UPDATE} 로 잡으므로 이제 완전 직렬화된다.
     *    <p>★잠금 보유 중 같은 행을 <b>다른 커넥션</b>으로 건드리는 호출이 없는지 재확인 완료(sec N-1 규칙):
     *    이 트랜잭션이 잠금 보유 중 호출하는 것은 {@code auditLogService.record}({@code REQUIRES_NEW})뿐이고
     *    대상 테이블이 {@code TB_AUDIT_LOG} 라 충돌하지 않는다. 새 호출을 추가할 때 반드시 다시 확인할 것.
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePolicy(SmsPolicyUpdateParam param, AuditContext auditContext) {

        SmsPolicyResult before = platformSmsMapper.selectPolicyForUpdate();
        if (before == null) {
            // 정책행 부재 = 마이그레이션 시드 누락. 새로 INSERT 하지 않는다(시드는 마이그레이션 소관).
            log.error("[SMS상한] 정책행 부재로 임계값 수정 불가 - prafta-sms-ppurio-2-rate-limit-policy.sql 시드를 적용할 것");
            throw new ApiException(PlatformErrorCode.PLATFORM_400_016);
        }

        // ★[3차 / sec N-7] 킬스위치 발동 중에는 전역 상한 상향을 거부한다.
        //   "발동 → 상한을 크게 올림 → 해제" 순서로 조작하면 킬스위치가 사실상 무력화된다.
        //   ★하향/유지는 허용한다 — 사고 대응 중 더 조이는 것을 막을 이유가 없다.
        if ("Y".equals(before.killSwitchYn()) && param.globalHourLimit() > before.globalHourLimit()) {
            log.warn("[SMS킬스위치] 발동 중 전역 상한 상향 시도 거부 - 운영자={}, before={}, after={}",
                    param.operatorUserCd(), before.globalHourLimit(), param.globalHourLimit());
            throw new ApiException(PlatformErrorCode.PLATFORM_400_018);
        }

        int updated = platformSmsMapper.updatePolicy(param);
        if (updated == 0) {
            throw new ApiException(PlatformErrorCode.PLATFORM_400_016);
        }

        auditLogService.record(
                AuditLogCommand.builder()
                        .cmpnyCd(PlatformConstants.PLATFORM_CMPNY_CD)
                        .userCd(param.operatorUserCd())
                        .actionType(AuditActionType.SETTING_CHANGE)
                        .resourceType(AuditResourceType.SMS_SEND_POLICY)
                        .resourceKey("DEFAULT")
                        .detailJson(buildPolicyDiffJson(before, param))
                        .build(),
                auditContext
        );

        log.info("SMS 발송 임계값 변경 - 운영자={}, 창={}초, 번호={}/{}, IP축={}({}/{}), 사용자={}/{}, 전역={}",
                param.operatorUserCd(), param.phoneWindowSec(),
                param.phoneHourLimit(), param.phoneDayLimit(),
                param.ipAxisEnabledYn(), param.ipHourLimit(), param.ipDayLimit(),
                param.userHourLimit(), param.userDayLimit(), param.globalHourLimit());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseKillSwitch(String operatorUserCd, AuditContext auditContext) {

        int updated = platformSmsMapper.releaseKillSwitch(operatorUserCd);
        if (updated == 0) {
            // 이미 해제 상태(동시 클릭 포함).
            throw new ApiException(PlatformErrorCode.PLATFORM_400_017);
        }

        auditLogService.record(
                AuditLogCommand.builder()
                        .cmpnyCd(PlatformConstants.PLATFORM_CMPNY_CD)
                        .userCd(operatorUserCd)
                        .actionType(AuditActionType.SETTING_CHANGE)
                        .resourceType(AuditResourceType.SMS_KILL_SWITCH)
                        .resourceKey("RELEASE")
                        .detailJson("{\"killSwitchYn\":{\"before\":\"Y\",\"after\":\"N\"}}")
                        .build(),
                auditContext
        );

        log.warn("[SMS킬스위치] 수동 해제 - 운영자={}. 발송이 즉시 재개된다.", operatorUserCd);
    }

    /**
     * 변경 전·후 값 쌍 JSON 조립. ★PII 없음(전부 정수/YN 설정값).
     * 값이 실제로 바뀐 항목만 담아 감사 로그가 불필요하게 커지지 않게 한다.
     */
    private String buildPolicyDiffJson(SmsPolicyResult before, SmsPolicyUpdateParam after) {
        StringBuilder sb = new StringBuilder("{");
        appendIntDiff(sb, "phoneWindowSec", before.phoneWindowSec(), after.phoneWindowSec());
        appendIntDiff(sb, "phoneHourLimit", before.phoneHourLimit(), after.phoneHourLimit());
        appendIntDiff(sb, "phoneDayLimit", before.phoneDayLimit(), after.phoneDayLimit());
        appendStringDiff(sb, "ipAxisEnabledYn", before.ipAxisEnabledYn(), after.ipAxisEnabledYn());
        appendIntDiff(sb, "ipHourLimit", before.ipHourLimit(), after.ipHourLimit());
        appendIntDiff(sb, "ipDayLimit", before.ipDayLimit(), after.ipDayLimit());
        appendIntDiff(sb, "userHourLimit", before.userHourLimit(), after.userHourLimit());
        appendIntDiff(sb, "userDayLimit", before.userDayLimit(), after.userDayLimit());
        appendIntDiff(sb, "globalHourLimit", before.globalHourLimit(), after.globalHourLimit());
        sb.append("}");
        return sb.toString();
    }

    private void appendIntDiff(StringBuilder sb, String field, int before, int after) {
        if (before == after) {
            return;
        }
        appendComma(sb);
        sb.append("\"").append(field).append("\":{\"before\":").append(before)
          .append(",\"after\":").append(after).append("}");
    }

    private void appendStringDiff(StringBuilder sb, String field, String before, String after) {
        if (before != null && before.equals(after)) {
            return;
        }
        appendComma(sb);
        sb.append("\"").append(field).append("\":{\"before\":\"").append(before)
          .append("\",\"after\":\"").append(after).append("\"}");
    }

    private void appendComma(StringBuilder sb) {
        if (sb.length() > 1) {
            sb.append(",");
        }
    }
}
