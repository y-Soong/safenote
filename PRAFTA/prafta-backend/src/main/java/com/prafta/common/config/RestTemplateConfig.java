package com.prafta.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.prafta.common.config.AiProperties;
import com.prafta.common.schedule.holiday.client.HolidayApiProperties;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate holidayRestTemplate(HolidayApiProperties props) {
        // Spring 버전에 따라 requestFactory로 timeout 설정 방식이 다릅니다.
        // 가장 호환 좋은 방식은 HttpComponentsClientHttpRequestFactory인데,
        // 의존성 추가가 필요할 수 있어 일단 기본 RestTemplate로 시작하고,
        // timeout은 추후 확정(Spring Boot 버전 확인 후) 추천.
        return new RestTemplate();
    }

    /**
     * TEI(BGE-m3) 임베딩 서버 호출 전용 RestTemplate.
     * 임베딩은 다소 오래 걸릴 수 있어 연결/읽기 타임아웃을 설정값(prafta.ai.tei.timeout-ms)으로 부여한다.
     * holidayRestTemplate 재사용 대신 별도 빈으로 분리(타임아웃 정책이 다르고 용도가 명확히 구분됨).
     */
    @Bean
    public RestTemplate aiTeiRestTemplate(AiProperties aiProperties) {
        int timeoutMs = aiProperties.getTei().getTimeoutMs();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }
}