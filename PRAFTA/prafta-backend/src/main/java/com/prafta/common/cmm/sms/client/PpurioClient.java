package com.prafta.common.cmm.sms.client;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prafta.common.cmm.sms.SmsFailureKind;
import com.prafta.common.cmm.sms.SmsSendRequest;
import com.prafta.common.cmm.sms.SmsSendResult;
import com.prafta.common.config.SmsProperties;
import com.prafta.common.security.normalize.Normalizers;

import lombok.extern.slf4j.Slf4j;

/**
 * 뿌리오(message.ppurio.com) 발송 클라이언트. 토큰 발급/캐시 + {@code POST /v1/message}.
 *
 * <p>API 계약(벤더 샘플 {@code .claude/refs/ppurio.md} 기준)
 * <ul>
 *   <li>{@code POST /v1/token} — {@code Authorization: Basic Base64(account:apiKey)} → 응답 {@code token}(TTL 24h)</li>
 *   <li>{@code POST /v1/message} — {@code Authorization: Bearer {token}}</li>
 * </ul>
 *
 * <p>★보안(§7-6 엄수)
 * <ul>
 *   <li>요청 바디(인증번호 포함)·응답 바디(휴대폰 포함)를 <b>통째로 로깅하지 않는다</b>.</li>
 *   <li>{@code Authorization}(Basic/Bearer)·{@code apiKey} 를 로그에 남기지 않는다.</li>
 *   <li>허용 로그 항목: refKey / HTTP status / messageKey / 벤더 code / 휴대폰 뒤 4자리.</li>
 *   <li>{@code RestClientResponseException.getResponseBodyAsString()} 을 그대로 로깅하지 않는다
 *       (파싱 후 code/description 만 추출).</li>
 * </ul>
 *
 * <p>★실패는 예외로 던지지 않고 {@link SmsSendResult} 로 감싼다(예외/결과 이중 채널 방지).
 */
@Slf4j
@Component
public class PpurioClient {

    /** 토큰 발급 경로. */
    private static final String TOKEN_PATH = "/v1/token";
    /** 발송 경로. */
    private static final String MESSAGE_PATH = "/v1/message";

    /**
     * 토큰 캐시 TTL(시간). 벤더 TTL 은 24h 이나 만료 경계 사고를 피하려 23h 로 여유를 둔다(§7-2).
     */
    private static final long TOKEN_CACHE_TTL_HOURS = 23L;

    /**
     * 발송 최대 시도 횟수. 1회차가 401/403(토큰 조기 무효화)이면 토큰을 강제 재발급하고 1회만 더 시도한다.
     * ★재귀 호출이 아니라 상한이 고정된 루프로 구현해 무한 재시도가 구조적으로 불가능하게 한다(§7-2).
     */
    private static final int MAX_SEND_ATTEMPTS = 2;

    /** 실패 코드 미상 시 사용할 내부 표기(벤더 코드가 없을 때만). */
    private static final String ERR_CD_UNKNOWN = "UNKNOWN";

    /**
     * [4차 / sec T-4] HTTP 2xx + 식별자 부재 + 벤더 실패코드까지 부재인 경우의 내부 표기.
     *
     * <p>★{@code VENDOR_REJECTED} 와 <b>반드시 구분</b>해야 한다. 벤더가 거절을 명시한 것이 아니라
     *    <b>우리가 응답을 해석하지 못한 것</b>이고, HTTP 2xx 인 이상 접수(=과금)됐을 개연성이 높다.
     *    따라서 분류는 {@link SmsFailureKind#TRANSPORT} 이고 상한 카운트에 <b>포함</b>된다
     *    (블랙리스트 밖 = 집계 대상 — {@code SmsSendPolicyMapper.xml countableSendStatus}).
     */
    private static final String ERR_CD_UNKNOWN_RESPONSE = "UNKNOWN_RESPONSE";

    /**
     * SMS2-D1(qa D-2 보강): {@link #send} 1회 호출의 전체 시간 예산(ms).
     *
     * <p>read-timeout 을 6초로 낮춰도 [토큰 발급 + 발송] 2왕복이 필요한 첫 발송은 최악 22초까지 걸릴 수 있고,
     *    401 재시도까지 겹치면 사용자 요청 스레드를 30~60초 붙잡는다. 그 사이 웹·앱 axios(10초)가 먼저 끊어
     *    설계한 SMS_502_* 메시지가 사용자에게 도달하지 못한다.
     *    각 HTTP 호출 직전에 예산 소진 여부를 확인해 재시도 루프가 시간을 무한정 먹지 않게 구조적으로 닫는다.
     */
    private static final long SEND_BUDGET_MS = 8000L;

    private final ObjectProvider<RestClient> ppurioRestClientProvider;
    private final SmsProperties smsProperties;
    private final ObjectMapper objectMapper;

    /** 발급 토큰 캐시(lock-free 읽기). 갱신은 {@link #tokenLock} 아래에서만. */
    private final AtomicReference<CachedToken> tokenCache = new AtomicReference<>();
    private final Object tokenLock = new Object();

    // ★ObjectProvider 지연 조회는 파라미터 이름을 한정자로 쓰지 않는다 — RestClient 빈이 2개(hcx/ppurio)인
    //   운영에서 @Qualifier 없이는 getIfAvailable() 이 호출 시점 NoUniqueBeanDefinitionException 으로 죽는다.
    public PpurioClient(@Qualifier("ppurioRestClient") ObjectProvider<RestClient> ppurioRestClientProvider,
                        SmsProperties smsProperties,
                        ObjectMapper objectMapper) {
        this.ppurioRestClientProvider = ppurioRestClientProvider;
        this.smsProperties = smsProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 게이트 ON(빈 존재 + 런타임 플래그) + 계정/인증키/발신번호 설정 완료 + base URL 이 https 인지.
     * SMS2-D1(sec M-4): https 가 아니면 실발송을 하지 않는다(부팅은 유지 — SKIPPED 로 흐른다).
     *
     * <p>★[3차 / sec N-9] {@code smsProperties.isEnabled()} 를 조건에 추가했다.
     *    {@code RestClient} 빈은 {@code @ConditionalOnProperty} 로 기동 시 한 번 결정되므로,
     *    기동 후에 게이트를 끌 방법이 없었다(플래그를 false 로 바꿔도 발송이 계속됐다).
     *    {@code SmsPolicyBootstrapValidator} 가 정책행 부재를 감지했을 때 실제로 발송을 멈추려면
     *    이 판정이 런타임 플래그를 봐야 한다.
     */
    public boolean isEnabled() {
        return smsProperties.isEnabled()
            && ppurioRestClientProvider.getIfAvailable() != null
            && smsProperties.getPpurio().isSendable();
    }

    /**
     * 문자 1건 발송.
     *
     * @param request 발송 요청(refKey / 수신번호 / 본문)
     * @return 발송 결과. 예외를 던지지 않는다.
     */
    public SmsSendResult send(SmsSendRequest request) {
        RestClient client = ppurioRestClientProvider.getIfAvailable();
        if (client == null || !isEnabled()) {
            // 상위(PpurioSmsSender)가 isEnabled() 로 이미 걸러내지만 방어적으로 한 번 더.
            return SmsSendResult.skipped();
        }

        String phoneLast4 = Normalizers.last4(request.toPhoneNorm());
        String requestJson;
        try {
            requestJson = objectMapper.writeValueAsString(buildSendBody(request));
        } catch (Exception e) {
            // ★직렬화 실패 메시지에 본문이 섞일 수 있으므로 예외 클래스명만 남긴다.
            log.error("뿌리오 발송 본문 직렬화 실패 - refKey={}, 예외={}", request.refKey(), e.getClass().getSimpleName());
            return SmsSendResult.failed(SmsFailureKind.TRANSPORT, "SERIALIZE_FAILED", "요청 본문 직렬화 실패");
        }

        // SMS2-D1: 전체 시간 예산. 각 HTTP 호출 직전에 확인한다.
        long deadlineNanos = System.nanoTime() + SEND_BUDGET_MS * 1_000_000L;

        SmsSendResult lastResult = null;
        for (int attempt = 1; attempt <= MAX_SEND_ATTEMPTS; attempt++) {

            if (System.nanoTime() > deadlineNanos) {
                log.error("뿌리오 발송 시간 예산 소진(토큰 단계) - refKey={}, 예산={}ms", request.refKey(), SEND_BUDGET_MS);
                return SmsSendResult.failed(SmsFailureKind.TRANSPORT, "BUDGET_EXCEEDED", "발송 시간 예산 소진");
            }

            String token;
            try {
                token = obtainToken(client, deadlineNanos);
            } catch (RestClientResponseException e) {
                // ★SMS2-D1(sec H-2): RestClientException 보다 반드시 먼저 잡는다.
                //   RestClientResponseException 은 RestClientException 의 하위 타입이라 순서가 바뀌면
                //   아래 catch 가 삼키고 safeReason() 이 "응답 바디 앞 200자"를 반환한다
                //   (뿌리오 401 바디에는 계정 정보가 섞일 수 있다).
                //   여기서는 HTTP status 만 노출한다.
                int status = e.getStatusCode().value();
                log.error("뿌리오 토큰 발급 실패 - refKey={}, status={}", request.refKey(), status);
                return SmsSendResult.failed(SmsFailureKind.TOKEN, "TOKEN_HTTP_" + status, "HTTP " + status);
            } catch (RestClientException e) {
                log.error("뿌리오 토큰 발급 실패 - refKey={}, 원인={}", request.refKey(), safeReason(e));
                return SmsSendResult.failed(SmsFailureKind.TOKEN, "TOKEN_ISSUE_FAILED", safeReason(e));
            } catch (TokenResponseException e) {
                log.error("뿌리오 토큰 응답 이상 - refKey={}, 사유={}", request.refKey(), e.getMessage());
                return SmsSendResult.failed(SmsFailureKind.TOKEN, "TOKEN_RESPONSE_INVALID", e.getMessage());
            }

            // ★[3차 / sec N-13] obtainToken 이 null 이면 tokenLock 대기 중 예산이 소진된 것이다.
            //   TOKEN 계열이 아니라 BUDGET_EXCEEDED 로 분류해야 상한 카운트에 포함된다(sec N-14 규칙).
            if (token == null) {
                log.error("뿌리오 발송 시간 예산 소진(토큰 락 대기) - refKey={}, 예산={}ms",
                    request.refKey(), SEND_BUDGET_MS);
                return SmsSendResult.failed(SmsFailureKind.TRANSPORT, "BUDGET_EXCEEDED", "발송 시간 예산 소진");
            }

            if (System.nanoTime() > deadlineNanos) {
                log.error("뿌리오 발송 시간 예산 소진(발송 단계) - refKey={}, 예산={}ms", request.refKey(), SEND_BUDGET_MS);
                return SmsSendResult.failed(SmsFailureKind.TRANSPORT, "BUDGET_EXCEEDED", "발송 시간 예산 소진");
            }

            try {
                String responseBody = client.post()
                    .uri(MESSAGE_PATH)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .body(requestJson)
                    .retrieve()
                    .body(String.class);

                // ★HTTP 2xx 여도 body 에 실패코드가 올 수 있음을 전제로 판정한다(요청서 §3-1 #1).
                return parseSendResponse(responseBody, request.refKey(), phoneLast4);

            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                boolean authFailure = (status == 401 || status == 403);

                if (authFailure && attempt < MAX_SEND_ATTEMPTS) {
                    // 토큰 조기 무효화로 판단 → 캐시 폐기 후 정확히 1회만 재발급·재시도.
                    log.warn("뿌리오 발송 인증 실패 - refKey={}, status={} → 토큰 재발급 후 1회 재시도", request.refKey(), status);
                    tokenCache.set(null);
                    lastResult = vendorErrorResult(e, request.refKey(), status);
                    continue;
                }

                lastResult = vendorErrorResult(e, request.refKey(), status);
                return lastResult;

            } catch (RestClientException e) {
                // 연결 실패/타임아웃/네트워크 단절. ★원문 응답 바디는 로깅하지 않는다.
                log.error("뿌리오 발송 호출 실패 - refKey={}, mblLast4={}, 원인={}",
                    request.refKey(), phoneLast4, safeReason(e));
                return SmsSendResult.failed(SmsFailureKind.TRANSPORT, "CALL_FAILED", safeReason(e));
            }
        }

        // 재시도 상한 소진(2회차도 401/403).
        return lastResult != null
            ? lastResult
            : SmsSendResult.failed(SmsFailureKind.TRANSPORT, "RETRY_EXHAUSTED", "발송 재시도 상한 소진");
    }

    // ------------------------------------------------------------------
    // 토큰
    // ------------------------------------------------------------------

    /**
     * 캐시된 토큰 반환(유효하면 lock-free). 만료/부재 시에만 동기화 블록에서 재검사 후 발급한다.
     * → 동시 요청 N 건이 {@code /v1/token} 을 N 번 호출하지 않는다(§9-4).
     *
     * <p>★[3차 / sec N-13] {@code tokenLock} <b>대기 시간</b>은 {@link #SEND_BUDGET_MS} 밖이었다.
     *    락 안에서 HTTP 호출이 일어나므로, 앞선 스레드가 토큰 발급에 read-timeout(6초)을 다 쓰면
     *    뒤에 줄 선 스레드는 예산을 이미 초과한 상태로 발송 단계에 진입했다.
     *    → 락 획득 직후 deadline 을 재검사한다.
     *
     * @param deadlineNanos {@code System.nanoTime()} 기준 전체 예산 만료 시각
     * @return 액세스 토큰. <b>락 대기 중 예산이 소진되면 null</b>(호출부가 BUDGET_EXCEEDED 로 처리)
     */
    private String obtainToken(RestClient client, long deadlineNanos) {
        CachedToken cached = tokenCache.get();
        if (cached != null && cached.isValid()) {
            return cached.token();
        }
        synchronized (tokenLock) {
            // double-checked: 대기 중 다른 스레드가 이미 발급했을 수 있다.
            CachedToken recheck = tokenCache.get();
            if (recheck != null && recheck.isValid()) {
                return recheck.token();
            }
            // ★락 대기 시간까지 포함해 예산을 재확인한다(sec N-13). 초과면 발급을 시도하지 않는다.
            if (System.nanoTime() > deadlineNanos) {
                return null;
            }
            String issued = issueToken(client);
            tokenCache.set(new CachedToken(issued, Instant.now().plus(TOKEN_CACHE_TTL_HOURS, ChronoUnit.HOURS)));
            log.info("뿌리오 액세스 토큰 발급 완료 - 캐시 유효 {}시간", TOKEN_CACHE_TTL_HOURS);
            return issued;
        }
    }

    /** {@code POST /v1/token} 호출. ★Basic 인증 헤더 값과 응답 본문은 로그 금지. */
    private String issueToken(RestClient client) {
        SmsProperties.Ppurio ppurio = smsProperties.getPpurio();
        String basic = Base64.getEncoder().encodeToString(
            (ppurio.getAccount() + ":" + ppurio.getApiKey()).getBytes(StandardCharsets.UTF_8));

        String responseBody = client.post()
            .uri(TOKEN_PATH)
            .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
            .retrieve()
            .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            throw new TokenResponseException("토큰 응답 본문이 비어 있음");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            throw new TokenResponseException("토큰 응답 JSON 파싱 실패");
        }

        // TODO(developer): 뿌리오 실응답 실측 후 확정 — 토큰 필드명은 벤더 샘플의 tokenResponse.get("token") 근거.
        //  accessToken 등 다른 필드명일 가능성에 대비해 후보를 순차 탐색한다.
        String token = firstNonBlank(root, "token", "accessToken", "access_token");
        if (token == null) {
            throw new TokenResponseException("토큰 응답에 token 필드 없음");
        }
        return token;
    }

    // ------------------------------------------------------------------
    // 요청 조립 / 응답 파싱
    // ------------------------------------------------------------------

    /**
     * {@code /v1/message} 요청 바디 조립(요청서 §7-5 확정안).
     *
     * <p>{@code rejectType} 은 <b>키 자체를 넣지 않는다</b> — 인증번호는 정보성 메시지이므로
     *    광고 수신거부 문구를 붙이지 않는다. {@code files}(MMS)·치환자({@code name}/{@code changeWord})도 미사용.
     */
    private Map<String, Object> buildSendBody(SmsSendRequest request) {
        SmsProperties.Ppurio ppurio = smsProperties.getPpurio();

        Map<String, Object> target = new LinkedHashMap<>();
        target.put("to", request.toPhoneNorm());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("account", ppurio.getAccount());
        body.put("messageType", "SMS");            // LMS/MMS 미사용(본문 90byte 이내 설계로 자동전환 회피)
        body.put("from", ppurio.getFrom());        // 사전등록 발신번호(하이픈 없이)
        body.put("content", request.content());    // ★고정 템플릿 + 서버 생성 인증번호만
        body.put("targetCount", 1);
        body.put("targets", List.of(target));
        body.put("refKey", request.refKey());      // 32자 이내
        body.put("duplicateFlag", "Y");
        return body;
    }

    /**
     * 발송 응답 파싱. 성공 판정 = {@code HTTP 2xx} <b>AND</b> messageKey 가 비어있지 않을 것.
     *
     * <p>★★[4차 / sec T-4] messageKey 가 없을 때의 분류를 둘로 나눈다.
     * <ul>
     *   <li><b>벤더 실패코드가 실제로 있다</b> → {@link SmsFailureKind#VENDOR_REJECTED} + 그 코드.
     *       벤더가 "받았지만 거절했다" 고 명시한 경우다.</li>
     *   <li><b>실패코드조차 없다</b>(2xx + 식별자 부재 + 벤더코드 부재) → {@link SmsFailureKind#TRANSPORT} +
     *       {@link #ERR_CD_UNKNOWN_RESPONSE}. <b>우리가 응답을 해석하지 못한 것</b>이지 벤더가 거절한 것이 아니다.
     *       HTTP 2xx 는 접수(=과금)를 강하게 시사하므로 상한 카운트에 <b>반드시 포함</b>되어야 한다.</li>
     * </ul>
     * 3차는 둘을 모두 {@code VENDOR_REJECTED} 로 뭉뚱그렸고, 상한 카운트가 화이트리스트라 전부 빠졌다.
     * {@code messageKey} 필드명이 미확정인 상태에서 이 조합은 <b>첫 실연동에 전건 발동 → 5축 전부 0</b>
     * 이라는 최악의 경로였다(1차 C-1 재현).
     */
    private SmsSendResult parseSendResponse(String responseBody, String refKey, String phoneLast4) {
        if (responseBody == null || responseBody.isBlank()) {
            log.error("뿌리오 발송 응답 본문이 비어 있음 - refKey={}", refKey);
            return SmsSendResult.failed(SmsFailureKind.TRANSPORT, "EMPTY_RESPONSE", "응답 본문 없음");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception e) {
            // ★응답 원문은 로깅 금지(휴대폰 포함 가능).
            log.error("뿌리오 발송 응답 JSON 파싱 실패 - refKey={}", refKey);
            return SmsSendResult.failed(SmsFailureKind.TRANSPORT, "PARSE_FAILED", "응답 JSON 파싱 실패");
        }

        // TODO(developer): 뿌리오 실응답 실측 후 확정 — messageKey 필드명이 미확인이라 후보를 순차 탐색한다.
        //  실측 시 값은 로깅하지 말고 필드명(키 목록)만 1회 확인한 뒤 확정하고 후보 탐색을 정리할 것(요청서 §3-1).
        String messageKey = firstNonBlank(root, "messageKey", "msgKey");
        if (messageKey == null) {
            messageKey = blankToNull(root.path("data").path("messageKey").asText(""));
        }

        if (messageKey != null) {
            log.info("뿌리오 발송 성공 - refKey={}, mblLast4={}, msgKey={}", refKey, phoneLast4, messageKey);
            return SmsSendResult.sent(messageKey);
        }

        // HTTP 2xx 이지만 식별자가 없다.
        String errCd = resolveVendorCode(root);
        String errMsg = resolveVendorMessage(root);

        if (ERR_CD_UNKNOWN.equals(errCd)) {
            // ★[4차 / sec T-4] 벤더 실패코드조차 없다 = 우리가 응답을 해석하지 못한 것이다.
            //   벤더 거절로 단정하면 안 되고, 2xx 인 이상 접수·과금됐을 개연성이 높으므로 집계 대상으로 둔다.
            log.error("뿌리오 발송 응답 해석 불가(2xx·식별자/실패코드 모두 부재) - refKey={}, mblLast4={}."
                    + " 응답 스키마 확정(messageKey 필드명)이 필요하다. ★상한 카운트에는 포함된다",
                    refKey, phoneLast4);
            return SmsSendResult.failed(SmsFailureKind.TRANSPORT, ERR_CD_UNKNOWN_RESPONSE, "응답 해석 불가(2xx)");
        }

        log.warn("뿌리오 발송 거절(2xx·식별자 부재) - refKey={}, mblLast4={}, code={}", refKey, phoneLast4, errCd);
        return SmsSendResult.failed(SmsFailureKind.VENDOR_REJECTED, errCd, errMsg);
    }

    /**
     * HTTP 4xx/5xx 응답을 실패 결과로 변환.
     * ★{@code getResponseBodyAsString()} 을 그대로 로깅하지 않고 파싱해 code/description 만 추출한다.
     */
    private SmsSendResult vendorErrorResult(RestClientResponseException e, String refKey, int status) {
        String errCd = "HTTP_" + status;
        String errMsg = null;
        SmsFailureKind kind = SmsFailureKind.TRANSPORT;

        try {
            String rawBody = e.getResponseBodyAsString();
            if (rawBody != null && !rawBody.isBlank()) {
                JsonNode root = objectMapper.readTree(rawBody);
                String vendorCode = resolveVendorCode(root);
                String vendorMsg = resolveVendorMessage(root);
                if (vendorCode != null && !ERR_CD_UNKNOWN.equals(vendorCode)) {
                    // 벤더가 자체 실패코드를 명시 → 거절로 분류.
                    errCd = vendorCode;
                    kind = SmsFailureKind.VENDOR_REJECTED;
                }
                errMsg = vendorMsg;
            }
        } catch (Exception ignore) {
            // 파싱 실패는 무시(HTTP status 만으로 기록). ★원문 로깅 금지.
        }

        if (errMsg == null) {
            errMsg = "HTTP " + status;
        }
        log.error("뿌리오 발송 오류 응답 - refKey={}, status={}, code={}", refKey, status, errCd);
        return SmsSendResult.failed(kind, errCd, errMsg);
    }

    /** 벤더 실패코드 후보 탐색. */
    private String resolveVendorCode(JsonNode root) {
        // TODO(developer): 뿌리오 실응답 실측 후 확정 — 에러코드 체계 미상(요청서 §3-1 #4). 원문을 그대로 적재한다.
        String code = firstNonBlank(root, "code", "resultCode", "errorCode", "status");
        return code != null ? code : ERR_CD_UNKNOWN;
    }

    /** 벤더 실패사유 후보 탐색. */
    private String resolveVendorMessage(JsonNode root) {
        // TODO(developer): 뿌리오 실응답 실측 후 확정 — 사유 필드명 미상. 원문을 그대로 적재한다.
        return firstNonBlank(root, "description", "message", "resultMessage", "errorMessage");
    }

    /** 후보 필드명을 순서대로 훑어 첫 비공백 텍스트를 반환(NPE 안전). 없으면 null. */
    private String firstNonBlank(JsonNode root, String... fieldNames) {
        for (String name : fieldNames) {
            String v = blankToNull(root.path(name).asText(""));
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v.trim();
    }

    /**
     * 예외 사유 축약. ★예외 메시지에 URL/헤더가 섞일 수 있으므로 원문 대신 클래스명 + 메시지 앞부분만 남긴다.
     * (baseUrl 에는 키가 없고 Authorization 은 헤더라 메시지에 포함되지 않지만 보수적으로 절단한다.)
     *
     * <p>★SMS2-D1(sec H-2): {@link RestClientResponseException} 은 {@code getMessage()} 에
     *    <b>응답 바디 프리뷰</b>가 들어간다. 메서드 이름이 {@code safeReason} 이라 후속 리뷰가 안전하다고
     *    오인하기 쉬우므로, 호출부의 catch 순서와 별개로 여기서도 방어한다(2중 방어 — 신규 호출부가
     *    실수로 이 메서드에 응답 예외를 넘겨도 바디가 새지 않는다).
     */
    private String safeReason(Exception e) {
        if (e instanceof RestClientResponseException rre) {
            return e.getClass().getSimpleName() + ": HTTP " + rre.getStatusCode().value();
        }
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) {
            return e.getClass().getSimpleName();
        }
        String trimmed = msg.length() > 200 ? msg.substring(0, 200) : msg;
        return e.getClass().getSimpleName() + ": " + trimmed;
    }

    // ------------------------------------------------------------------
    // 내부 타입
    // ------------------------------------------------------------------

    /** 캐시된 액세스 토큰. */
    private record CachedToken(String token, Instant expiresAt) {
        boolean isValid() {
            return token != null && !token.isBlank() && Instant.now().isBefore(expiresAt);
        }
    }

    /** 토큰 응답 형식 이상(2xx 이지만 token 필드 부재 등). */
    private static class TokenResponseException extends RuntimeException {
        TokenResponseException(String message) {
            super(message);
        }
    }
}
