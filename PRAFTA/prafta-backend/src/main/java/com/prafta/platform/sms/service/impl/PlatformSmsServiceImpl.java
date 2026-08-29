package com.prafta.platform.sms.service.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.HmacSigner;
import com.prafta.platform.common.PlatformConstants;
import com.prafta.platform.sms.application.param.SmsHistoryListParam;
import com.prafta.platform.sms.application.param.SmsPolicyUpdateParam;
import com.prafta.platform.sms.application.query.SmsHistoryListQuery;
import com.prafta.platform.sms.application.result.SmsHistoryRowResult;
import com.prafta.platform.sms.application.result.SmsPolicyResult;
import com.prafta.platform.sms.dto.response.SmsConsoleResponse;
import com.prafta.platform.sms.dto.response.SmsHistoryListResponse;
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

    /** 발송 이력의 휴대폰 복호(마스킹 전 단계). ★복호 결과는 마스킹 이후 즉시 버린다. */
    private final AesGcmCrypto aesGcmCrypto;

    /**
     * 휴대폰 검색 입력을 {@code MBL_NO_HMAC} 로 변환한다.
     *
     * <p>★적재 경로({@code BaseinfoServiceImpl} 등)와 <b>동일한 유틸·동일한 정규화</b>를 재사용한다.
     *    키/알고리즘/도메인 구분자가 어긋나면 예외 없이 조용히 0건이 된다(직접 구현 금지).
     */
    private final HmacSigner hmacSigner;

    /** 현황 카드 집계 구간(시간). */
    private static final int STAT_HOURS_1H = 1;
    private static final int STAT_HOURS_24H = 24;

    /** 마스킹 불가(복호 실패/암호문 결측) 시 표기. ★TB_SMS_AUTH_CODE 에는 LAST4 컬럼이 없어 폴백이 없다. */
    private static final String MASK_UNAVAILABLE = "-";

    /**
     * 발송 이력 시각 표기 타임존/포맷. [2026-08-30]
     * ★서버(JVM/DB) 기본 타임존에 기대지 않고 KST 를 명시한다 — 운영 EC2/RDS 시스템시각이 UTC 라
     *   DB DATE_FORMAT 문자열을 그대로 내리면 KST-9h 로 표시됐다. 매퍼가 epoch 초(타임존 무관
     *   절대시각)로 내리고 여기서 Asia/Seoul 로 포맷한다(개발 KST·운영 UTC 모두 정확).
     */
    private static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter KST_DTM_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
     * 발송 이력 목록 조회(기간 필터 + 서버 페이징, 최신순).
     *
     * <p>★★<b>인가 논증</b> — {@code TB_SMS_AUTH_CODE} 에는 {@code CMPNY_CD} 컬럼이 없어
     * 테넌트 술어를 걸 수단이 없다. 따라서 이 조회는 {@code PlatformOperatorGateInterceptor}
     * ({@code /prafta/platformApi/**} 전 경로에 {@code gv_cmpnyCd == prafta_system_admin} 강제)
     * 뒤에서만 성립한다. 게이트는 <b>어노테이션이 아니라 경로 기반</b>이므로,
     * 이 메서드를 {@code com.prafta.web.*} / {@code com.prafta.app.*} 컨트롤러에서 호출하면
     * 회사 경계 없이 전 고객사 휴대폰이 새어나간다.
     *
     * <p>★휴대폰은 서버에서 복호 후 마스킹한다. HMAC·IP 해시는 응답에 담지 않는다.
     *    인증번호는 [2026-08-30 방침 변경] 미발송(SKIPPED) 행에 한해 담는다(매퍼 CASE 게이트).
     * <p>★감사 로그(§11.3) 대상이 아니다(마스킹 목록 조회) — 서버 로그만 남긴다.
     *    ※엑셀 다운로드를 추가하면 §11.3 "다운로드" 에 해당해 감사 적재가 <b>필수</b>가 된다.
     * <p>{@code @Transactional} 을 두지 않는다(읽기 전용 단순 조회 — {@code selectConsole} 과 동일).
     */
    @Override
    public SmsHistoryListResponse selectSendHistory(SmsHistoryListParam param) {

        // 검색 입력 평문은 여기서 HMAC 으로만 변환하고 이후 어디에도 남기지 않는다.
        String mblNoHmac = (param.mblNo() == null) ? null : hmacSigner.hmacSha256Base64Url(param.mblNo());

        SmsHistoryListQuery query = SmsHistoryListQuery.from(param, mblNoHmac);

        // 페이저 기준 건수를 먼저 구하고, 0건이면 목록 쿼리를 생략한다.
        int totalCount = platformSmsMapper.selectSendHistoryCount(query);

        List<SmsHistoryListResponse.Row> list = new ArrayList<>();
        if (totalCount > 0) {
            List<SmsHistoryRowResult> rows = platformSmsMapper.selectSendHistoryList(query);
            if (rows != null) {
                for (SmsHistoryRowResult row : rows) {
                    list.add(SmsHistoryListResponse.Row.builder()
                            .smsId(row.smsId())
                            // [2026-08-30] 매퍼가 epoch 초로 내리고 여기서 KST 로 포맷한다
                            //   (운영 DB 시계=UTC 라 DATE_FORMAT 문자열 그대로는 KST-9h 표시였다).
                            .insertDate(formatEpochKst(row.insertEpoch()))
                            .mblNo(maskMblNo(decryptMblNo(row.mblNoEnc())))
                            .purposeCd(row.purposeCd())
                            .sendStatus(row.sendStatus())
                            .sendDate(formatEpochKst(row.sendEpoch()))
                            .verifiedYn(row.verifiedYn())
                            .failCnt(row.failCnt())
                            .sendErrCd(row.sendErrCd())
                            .sendErrMsg(row.sendErrMsg())
                            .sendUserCd(row.sendUserCd())
                            // [2026-08-30 방침 변경] 미발송(SKIPPED) 행만 매퍼 CASE 게이트로 값이 온다.
                            .authCd(row.authCdSkipped())
                            .build());
                }
            }
        }

        // ★로그에는 운영자 + 조건 요약 + 건수만 남긴다.
        //   휴대폰 평문·검색 입력 번호·인증번호·sendErrMsg 본문은 절대 남기지 않는다
        //   (벤더 원문에 수신번호가 섞일 수 있다). 검색 사용 여부만 Y/N 으로 남긴다.
        log.info("Platform_05 SMS 발송 이력 조회 - 운영자={}, 기간={}~{}, 목적={}, 상태={}, 번호검색={}, page={}, {}건/전체 {}건",
                param.gvUserCd(), param.startDate(), param.endDate(), param.purposeCd(), param.sendStatus(),
                (mblNoHmac == null ? "N" : "Y"), param.page(), list.size(), totalCount);

        return SmsHistoryListResponse.builder()
                .historyList(list)
                .totalCount(totalCount)
                .build();
    }

    /**
     * epoch 초 → {@code yyyy-MM-dd HH:mm:ss}(Asia/Seoul) 포맷. null 이면 null(화면이 "-" 처리).
     */
    private String formatEpochKst(Long epochSec) {

        if (epochSec == null) {
            return null;
        }
        return Instant.ofEpochSecond(epochSec).atZone(ZONE_KST).format(KST_DTM_FORMATTER);
    }

    /**
     * 휴대폰 암호문 복호. 실패해도 목록 조회를 막지 않는다(해당 셀만 {@code "-"}).
     *
     * <p>★예외 메시지에 평문이 섞이지 않도록 {@code e.getMessage()} 만 남긴다(스택트레이스·암호문 금지).
     */
    private String decryptMblNo(String mblNoEnc) {

        if (mblNoEnc == null || mblNoEnc.isBlank()) {
            return null;
        }
        try {
            return aesGcmCrypto.decrypt(mblNoEnc);
        } catch (Exception e) {
            log.warn("Platform_05 발송 이력 휴대폰 복호화 실패(마스킹 불가로 표기 대체) - {}", e.getMessage());
            return null;
        }
    }

    /**
     * 휴대폰 가운데 마스킹 — User_09 와 <b>동일 규칙</b>(11자리 앞3-****-뒤4 / 10자리 앞3-***-뒤4).
     *
     * <p>★{@code TB_SMS_AUTH_CODE} 에는 {@code MBL_NO_LAST4} 컬럼이 없다. 그래서 User_09 의 LAST4 폴백이
     *    여기에는 없고, 복호 실패 시 폴백은 {@code "-"} 다.
     *    <b>LAST4 를 만들려고 {@code MBL_NO_HMAC} 등을 끌어오지 말 것</b>(상관·역추적 재료 노출).
     * <p>공통 유틸로 빼지 않는 것은 의도적이다 — {@code common.util} 로 올리면 web/app 어디서나
     *    호출 가능해져 PII 마스킹 규칙의 검토 범위가 넓어진다(User_09 와 동일 판단).
     */
    private String maskMblNo(String digits) {

        if (digits != null && !digits.isBlank()) {
            String d = digits.replaceAll("\\D", "");
            if (d.length() == 11) {
                return d.substring(0, 3) + "-****-" + d.substring(7);
            }
            if (d.length() == 10) {
                return d.substring(0, 3) + "-***-" + d.substring(6);
            }
        }
        return MASK_UNAVAILABLE;
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
