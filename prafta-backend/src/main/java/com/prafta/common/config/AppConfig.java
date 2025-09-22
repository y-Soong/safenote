package com.prafta.common.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true) // true 시 CGLIB 기반 프록시, false 시 JDK 동적 프록시
public class AppConfig {
    // AOP 외에도 공통적인 설정을 여기에서 추가할 수 있음
}