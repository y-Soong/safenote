package com.prafta.platform.sms.dto.response;

import com.prafta.platform.sms.application.result.SmsPolicyResult;
import com.prafta.platform.sms.application.result.SmsSendStatResult;

import lombok.Builder;
import lombok.Getter;

/**
 * Platform_05(SMS 발송 관리) 화면 조회 응답 — 현황·상태·임계값을 한 번에 담는다.
 *
 * <p>★PII 노출 금지: <b>이 응답</b>에는 개별 발송 이력을 담지 않는다. <b>집계와 설정값만</b>이다.
 *
 * <p>★★<b>[방침 변경 / 2026-08-16]</b> 개별 발송 이력은 <b>플랫폼 운영자 콘솔에 한해</b>
 *    별도 응답({@link SmsHistoryListResponse}, {@code POST /platformApi/sms/send-histories/search})으로 노출한다
 *    — 휴대폰은 서버에서 복호 후 마스킹한다.
 *    다만 <b>인증번호({@code AUTH_CD}) · {@code MBL_NO_HMAC} · {@code SEND_IP_HASH} · refKey 는 여전히 제외</b> 다.
 *    ({@code TB_SMS_AUTH_CODE} 에 {@code CMPNY_CD} 가 없어 테넌트 경계를 걸 수 없으므로,
 *     개별 행 조회는 {@code /platformApi} 게이트 뒤에서만 성립한다.)
 *
 * <p>★{@code gateEnabled} 는 읽기전용 표시용이다(서버 secrets {@code PPURIO_ENABLED} 소관).
 *    화면에서 토글하지 않는다 — 진실 원천을 하나로 유지하기 위함.
 */
@Getter
@Builder
public class SmsConsoleResponse {

    /**
     * 발송 게이트(prafta.sms.enabled) <b>설정값</b> — 읽기전용 표시. "설정상 ON" 인지만 나타낸다.
     * ★[4차 / sec T-7] 런타임 강제 OFF 는 여기에 반영하지 않는다({@link #gateForcedOff} 로 분리).
     */
    private final boolean gateEnabled;

    /**
     * 런타임 <b>강제 OFF</b> 여부. [4차 / sec T-7]
     *
     * <p>정책행 부재/조회 실패를 만나면 시스템이 발송을 스스로 끈다(fail-open 의 결과를
     * "상한 없이 발송" 이 아니라 "발송 안 함" 으로 만들기 위함).
     * <p>★설정값과 분리하지 않으면 화면이 "설정을 OFF 로 둔 것" 과 "시스템이 사고를 감지해 내린 것" 을
     *    구분하지 못해 운영자가 원인을 잘못 짚는다(07-31 {@code PUSH_WORKER_ENABLED} 오판 계열).
     */
    private final boolean gateForcedOff;

    /** 강제 OFF 사유(운영자 안내용). 강제 OFF 가 아니면 null. ★PII·키를 담지 않는다. */
    private final String gateForcedOffReason;

    /**
     * <b>실제로 발송이 나갈 수 있는 상태인지</b>. [3차 / qa Q-7]
     *
     * <p>{@code gateEnabled} 는 토글 값일 뿐이다. 실제 발송 조건은
     * 계정/인증키/발신번호가 모두 주입되고 {@code PPURIO_BASE_URL} 이 https 여야 성립한다
     * ({@code SmsProperties.Ppurio.isSendable()}).
     * 하나라도 어긋나면 화면은 "ON" 인데 전 흐름이 조용히 SKIPPED 로 흐른다.
     *
     * <p>★07-31 {@code PUSH_WORKER_ENABLED} 오판 사고와 같은 계열이다 —
     *    "토글은 켜져 있는데 실제로는 안 나가는" 상태를 화면이 숨기면 안 된다.
     *    그래서 두 값을 <b>분리해서</b> 내려보내고 화면이 "발송 가능" 과 "설정상 ON" 을 구분해 표시한다.
     */
    private final boolean sendable;

    /** 정책 + 킬스위치 상태. 정책행이 없으면 null(화면이 안내 문구 + 저장 비활성으로 처리). */
    private final SmsPolicyResult policy;

    /** 최근 1시간 발송 상태 분포. */
    private final SmsSendStatResult stat1h;

    /** 최근 24시간 발송 상태 분포. */
    private final SmsSendStatResult stat24h;

    /**
     * 전역 상한 소진 건수.
     * ★킬스위치 판정과 <b>완전히 같은 쿼리</b>({@code SmsSendPolicyMapper.selectGlobalSentCnt})를 쓴다.
     *   규칙이 갈리면 "화면은 여유 있다는데 킬스위치가 걸린" 상황이 생긴다.
     */
    private final int globalUsedCnt;

    /** 전역 시간당 상한(소진율 분모). 0 이하면 무제한. */
    private final int globalHourLimit;
}
