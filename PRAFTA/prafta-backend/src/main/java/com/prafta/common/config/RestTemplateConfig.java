package com.prafta.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

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
}