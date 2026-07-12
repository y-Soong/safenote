package com.prafta.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import lombok.extern.slf4j.Slf4j;

/**
 * HyperCLOVA X(HCX-005) LLM 클라이언트 배선(ai01/answer, Phase 2 + 위험성평가 AI 도출).
 *
 * <p>★게이트: {@code prafta.ai.llm.enabled=true} 일 때만 이 설정이 활성화되어 HCX REST 클라이언트 빈이 생성된다.
 *    게이트 OFF(기본) 시 본 설정은 back-off 되어 빈이 존재하지 않으며,
 *    호출 계층({@code LlmAnswerClient})은 클라이언트 주입 부재를 감지해 AI_503_001(기능 비활성)로 응답한다(부분 배포 안전).
 *
 * <p>★비밀 처리: 키 원문은 repo(application*.properties)에 하드코딩하지 않는다.
 *    {@code ${CLOVA_STUDIO_API_KEY:}} 플레이스홀더로 주입받아 Authorization 기본 헤더로 싣는다.
 *    이 플레이스홀더는 OS 환경변수 또는 외부 시크릿 파일({@code secrets/platform-bootstrap.properties})의
 *    {@code CLOVA_STUDIO_API_KEY} 에서 해석된다(DB_PASSWORD/JWT_SECRET 등 다른 시크릿과 동일 방식).
 *    키가 없으면(빈 값) 첫 호출 시 401 → AI_502_003 로 흐른다.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "prafta.ai.llm.enabled", havingValue = "true")
public class AiLlmConfig {

    /** CLOVA Studio API 키 플레이스홀더 소스명(로그·안내용). */
    private static final String CLOVA_API_KEY_NAME = "CLOVA_STUDIO_API_KEY";

    /** 요청 타임아웃(ms, 비스트리밍 근거답변 기준). 느린 응답이 서블릿 스레드를 장시간 점유하지 않도록 60초 상한. */
    private static final int REQUEST_TIMEOUT_MS = 60_000;

    /**
     * HCX REST 클라이언트 빈. base host + 타임아웃 + JSON 기본 헤더 + Authorization(플레이스홀더 키).
     * 요청별 헤더(X-NCP-CLOVASTUDIO-REQUEST-ID)와 본문은 {@code LlmAnswerClient} 가 호출 시점에 부여한다.
     *
     * @param apiKey {@code ${CLOVA_STUDIO_API_KEY:}} — env/시크릿 파일에서 해석. 미설정 시 빈 문자열.
     */
    @Bean
    public RestClient hcxRestClient(AiProperties aiProperties,
                                    @Value("${CLOVA_STUDIO_API_KEY:}") String apiKey) {
        String host = aiProperties.getLlm().getHost();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(REQUEST_TIMEOUT_MS);
        requestFactory.setReadTimeout(REQUEST_TIMEOUT_MS);

        RestClient.Builder builder = RestClient.builder()
            .baseUrl(host)
            .requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            log.info("HCX LLM 클라이언트 빈 생성(게이트 ON) - host={}, 요청 타임아웃={}s, API 키는 {} 에서 로드",
                host, REQUEST_TIMEOUT_MS / 1000, CLOVA_API_KEY_NAME);
        } else {
            // 게이트 ON 이지만 키 미주입 — 빈은 만들되 첫 호출 시 401(AI_502_003)로 흐른다.
            log.warn("HCX LLM 클라이언트 빈 생성(게이트 ON) - host={} 이나 {} 미설정 → 첫 호출 시 401(AI_502_003) 예상",
                host, CLOVA_API_KEY_NAME);
        }
        return builder.build();
    }
}
