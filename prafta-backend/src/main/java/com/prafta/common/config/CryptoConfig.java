package com.prafta.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.prafta.common.security.crypto.CryptoProperties;

/**
 * CryptoProperties를 Spring 컨테이너에 등록.
 *
 * - @ConfigurationProperties는 이 EnableConfigurationProperties 등록이 필요(또는 @ConfigurationPropertiesScan 사용)
 */
@Configuration
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoConfig {
}