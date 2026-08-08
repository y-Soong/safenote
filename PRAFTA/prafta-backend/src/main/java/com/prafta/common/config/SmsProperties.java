package com.prafta.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SMS 발송(뿌리오) 설정 바인딩. prefix = "prafta.sms".
 *
 * <p>{@code AiProperties} 미러(중첩 static class + getter/setter).
 *
 * <p>★Lombok {@code @Data}/{@code @ToString} 을 절대 붙이지 않는다.
 *    프로퍼티 덤프/에러 로그에 {@code api-key} 원문이 그대로 노출된다(plan §3 T12).
 *    {@link #toString()} 도 재정의하지 않는다(기본 구현은 필드값을 찍지 않는다).
 */
@ConfigurationProperties(prefix = "prafta.sms")
public class SmsProperties {

    /**
     * 실발송 게이트(<b>설정값</b>). 기본 false.
     * ★키 미설정 환경(타 개발자 로컬/CI)에서 부팅이 깨지지 않아야 하므로 기본값은 반드시 false 다.
     * 운영/개발 서버는 secrets/platform-bootstrap.properties 의 PPURIO_ENABLED 가 이 값을 덮는다.
     *
     * <p>★★[4차 / sec T-7] {@code volatile} 이다. 이 값은 바인딩 스레드(기동)와
     *    {@code SmsPolicyBootstrapValidator}(ApplicationReadyEvent 스레드),
     *    그리고 발송 경로(요청 스레드 N개)가 <b>서로 다른 스레드에서</b> 읽고 쓴다.
     *    non-volatile 이면 강제 OFF 가 일부 요청 스레드에 영원히 보이지 않을 수 있다
     *    (JMM 상 가시성 보장이 없다 — "대부분 보인다" 는 우연이지 계약이 아니다).
     */
    private volatile boolean enabled = false;

    /**
     * 런타임 강제 OFF 여부. [4차 / sec T-7]
     *
     * <p>설정값({@link #enabled})과 <b>분리</b>한다. 3차까지는 강제 OFF 가 {@code setEnabled(false)} 로
     * 설정값 자체를 덮어써서, Platform_05 화면이 "설정 OFF" 와 "시스템이 강제로 내린 OFF" 를 구분하지 못했다
     * (07-31 {@code PUSH_WORKER_ENABLED} 오판 계열 — 화면만 보고 원인을 잘못 짚게 된다).
     */
    private volatile boolean forcedOff = false;

    /** 강제 OFF 사유(운영자 안내용 짧은 한국어 문구). 강제 OFF 가 아니면 null. */
    private volatile String forcedOffReason = null;

    /** 뿌리오 벤더 설정. */
    private final Ppurio ppurio = new Ppurio();

    /**
     * 실발송 게이트의 <b>실효값</b>. 설정값이 ON 이어도 강제 OFF 가 걸려 있으면 false 다.
     * 발송 판정 경로는 반드시 이 메서드를 본다.
     */
    public boolean isEnabled() {
        return enabled && !forcedOff;
    }

    /**
     * ★설정값 그대로(강제 OFF 미반영). 화면이 "설정 OFF" 와 "강제 OFF" 를 구분해 표시하는 용도로만 쓴다.
     *    발송 판정에 이 값을 쓰지 말 것.
     */
    public boolean isConfiguredEnabled() {
        return enabled;
    }

    /** 스프링 프로퍼티 바인딩 전용. ★런타임에 게이트를 내릴 때는 {@link #forceDisable(String)} 을 쓸 것. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 런타임 강제 OFF. [4차 / sec T-7]
     *
     * <p>설정값을 덮어쓰지 않고 별도 플래그만 세운다. 멱등이며(이미 강제 OFF 면 사유를 갱신하지 않는다)
     * 되돌리는 경로는 <b>재기동뿐</b>이다 — 원인 미확인 상태의 자동 재개는 킬스위치 자동 해제와 같은 위험이다.
     *
     * @param reason 화면·로그에 노출할 짧은 사유(PII·키 금지)
     * @return 이번 호출로 상태가 바뀌었으면 true(로그를 1회만 남기기 위한 반환값)
     */
    public boolean forceDisable(String reason) {
        if (forcedOff) {
            return false;
        }
        this.forcedOffReason = reason;
        this.forcedOff = true;
        return true;
    }

    /** 런타임 강제 OFF 상태인지. */
    public boolean isForcedOff() {
        return forcedOff;
    }

    /** 강제 OFF 사유(없으면 null). */
    public String getForcedOffReason() {
        return forcedOffReason;
    }

    public Ppurio getPpurio() {
        return ppurio;
    }

    /** 뿌리오(message.ppurio.com) 연동 설정. */
    public static class Ppurio {

        /** API Base URL. */
        private String baseUrl = "https://message.ppurio.com";
        /** 뿌리오 계정 ID. */
        private String account = "";
        /** 연동 개발 인증키. ★어떤 로그에도 출력하지 않는다. */
        private String apiKey = "";
        /** 사전등록 발신번호(하이픈 없이). 미등록 번호는 전기통신사업법상 전량 실패한다. */
        private String from = "";
        /** 연결 타임아웃(ms). */
        private int connectTimeoutMs = 5000;
        /** 읽기 타임아웃(ms). 발송은 사용자 요청 스레드에서 동기 수행되므로 과도하게 늘리지 않는다. */
        private int readTimeoutMs = 10000;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getAccount() {
            return account;
        }

        public void setAccount(String account) {
            this.account = account;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        /** 계정/인증키/발신번호가 모두 채워졌는지(발송 가능 최소 조건). */
        public boolean hasCredentials() {
            return isNotBlank(account) && isNotBlank(apiKey) && isNotBlank(from);
        }

        /**
         * SMS2-D1(sec M-4): base URL 이 https 인지.
         *
         * <p>http 로 잘못 주입되면 토큰 발급의 {@code Authorization: Basic Base64(account:apiKey)} 와
         *    발송 본문(인증번호·수신번호)이 평문으로 나간다. 환경변수 오타 한 번으로 API 키가 유출되므로
         *    "동작은 하는데 안전하지 않은" 상태를 허용하지 않는다.
         *
         * <p>★단 여기서 예외를 던져 부팅을 깨뜨리지는 않는다(요청서 1차 §4-2 — 키 미설정 환경에서
         *    부팅이 깨지면 안 된다). 발송만 비활성화되고 SKIPPED 로 흐른다.
         */
        public boolean isSecureBaseUrl() {
            return baseUrl != null && baseUrl.startsWith("https://");
        }

        /** 실발송 가능 조건 전체(계정/키/발신번호 + https). 게이트(enabled)는 별도 계층이다. */
        public boolean isSendable() {
            return hasCredentials() && isSecureBaseUrl();
        }

        private static boolean isNotBlank(String v) {
            return v != null && !v.isBlank();
        }
    }
}
