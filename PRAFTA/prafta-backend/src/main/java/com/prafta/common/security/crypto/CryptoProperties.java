package com.prafta.common.security.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 암호 관련 설정 값 바인딩용 Properties.
 *
 * - application.properties / application.yml 어느 쪽이든 동일하게 바인딩 가능
 * - 키(pepper)는 반드시 환경변수로 주입되는 값을 받도록 구성하는 것을 권장
 */
@ConfigurationProperties(prefix = "crypto")
public record CryptoProperties(
		String hmacPepper,
        String aesKey
) {}