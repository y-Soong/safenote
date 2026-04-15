package com.prafta.common.config;

import org.mybatis.spring.boot.autoconfigure.ConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.prafta.common.security.crypto.AesGcmCrypto;
import com.prafta.common.security.crypto.mybatis.AesGcmDecryptTypeHandler;

@Configuration
public class MybatisTypeHandlerConfig {

    @Bean
    public ConfigurationCustomizer registerCryptoHandlers(AesGcmCrypto crypto) {
        return configuration -> configuration.getTypeHandlerRegistry()
                .register(new AesGcmDecryptTypeHandler(crypto));
    }
}