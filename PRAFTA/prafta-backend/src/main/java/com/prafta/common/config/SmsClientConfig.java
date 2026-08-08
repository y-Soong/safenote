package com.prafta.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.prafta.common.security.normalize.Normalizers;

import lombok.extern.slf4j.Slf4j;

/**
 * 뿌리오 SMS REST 클라이언트 배선.
 *
 * <p>★게이트: {@code prafta.sms.enabled=true} 일 때만 본 설정이 활성화되어 {@code ppurioRestClient} 빈이 생성된다.
 *    게이트 OFF(기본) 시 빈이 존재하지 않으며, 호출 계층({@code PpurioSmsSender})은
 *    {@code ObjectProvider} 로 빈 부재를 감지해 발송을 SKIPPED 처리한다(예외 없음 — 무회귀).
 *
 * <p>★프로퍼티 바인딩({@code @EnableConfigurationProperties})은 여기가 아니라 상시 설정 {@link SmsConfig} 에 있다.
 *    여기에 붙이면 게이트 OFF 시 {@code SmsProperties} 빈까지 사라져 부팅이 깨진다(plan §3 T2).
 *
 * <p>★{@code Authorization} 은 기본 헤더로 걸지 않는다. 뿌리오는 토큰 발급(Basic)과 발송(Bearer)의
 *    인증 스킴이 다르므로 호출 시점에 부여한다.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "prafta.sms.enabled", havingValue = "true")
public class SmsClientConfig {

    /**
     * 뿌리오 REST 클라이언트 빈. base URL + 타임아웃 + JSON 기본 헤더.
     *
     * <p>★로그에는 계정/인증키 원문을 남기지 않는다. 계정은 설정 여부(true/false)만,
     *    발신번호는 뒤 4자리만 출력한다(§7-6).
     */
    @Bean
    public RestClient ppurioRestClient(SmsProperties smsProperties) {
        SmsProperties.Ppurio ppurio = smsProperties.getPpurio();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(ppurio.getConnectTimeoutMs());
        requestFactory.setReadTimeout(ppurio.getReadTimeoutMs());

        RestClient client = RestClient.builder()
            .baseUrl(ppurio.getBaseUrl())
            .requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();

        boolean accountSet = ppurio.getAccount() != null && !ppurio.getAccount().isBlank();
        boolean apiKeySet = ppurio.getApiKey() != null && !ppurio.getApiKey().isBlank();
        String fromLast4 = Normalizers.last4(Normalizers.normalizePhone(ppurio.getFrom()));

        // SMS2-D1(sec M-4): https 가 아니면 API 키(Basic 헤더)와 인증번호가 평문으로 나간다.
        // ★부팅을 깨뜨리지 않고(요청서 1차 §4-2) 실발송만 비활성화한다 — PpurioClient.isEnabled() 가 false 가 되어
        //   전 흐름이 SEND_STATUS='SKIPPED' 로 흐른다. 운영자가 알아채도록 error 로 1회 남긴다.
        if (!ppurio.isSecureBaseUrl()) {
            log.error("[SMS] PPURIO_BASE_URL 이 https 가 아님 — 실발송 비활성(SKIPPED 로 흐름). 환경변수를 https 로 교정할 것");
        }

        if (ppurio.isSendable()) {
            log.info("뿌리오 SMS 클라이언트 빈 생성(게이트 ON) - baseUrl={}, connectTimeout={}ms, readTimeout={}ms, 발신번호(뒤4)={}",
                ppurio.getBaseUrl(), ppurio.getConnectTimeoutMs(), ppurio.getReadTimeoutMs(), fromLast4);
        } else {
            // 게이트 ON 이지만 키/발신번호 미주입 — 빈은 만들되 발송은 SKIPPED 로 흐른다(부팅 차단 없음).
            log.warn("뿌리오 SMS 클라이언트 빈 생성(게이트 ON) - 설정 미완으로 실발송 비활성. account설정={}, apiKey설정={}, from설정={}",
                accountSet, apiKeySet, fromLast4 != null);
        }
        return client;
    }
}
