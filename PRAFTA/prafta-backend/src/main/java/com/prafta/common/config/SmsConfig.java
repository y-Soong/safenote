package com.prafta.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SMS 발송 설정 바인딩 활성화(★게이트와 무관한 상시 설정).
 *
 * <p>★함정 T2: {@code @EnableConfigurationProperties(SmsProperties.class)} 를
 *    {@code @ConditionalOnProperty} 가 붙은 {@link SmsClientConfig} 에 부착하면,
 *    게이트 OFF 시 {@code SmsProperties} 빈까지 함께 사라져 이를 주입받는 컴포넌트
 *    (PpurioSmsSender / PpurioClient)의 배선이 깨지고 <b>부팅이 실패</b>한다.
 *    (요청서 §4-2 "키 미설정 환경에서 부팅이 깨지지 않아야 한다" 정면 위반)
 *
 * <p>따라서 {@code AiDbConfig}(상시) ↔ {@code AiLlmConfig}(조건부) 분리 선례 그대로,
 *    프로퍼티 바인딩은 본 상시 설정에, 조건부 RestClient 빈은 {@link SmsClientConfig} 에 둔다.
 */
@Configuration
@EnableConfigurationProperties(SmsProperties.class)
public class SmsConfig {
}
