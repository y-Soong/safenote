package com.prafta.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import com.prafta.common.security.FileServingFilter;
import com.prafta.common.security.FileUrlSigner;

/**
 * 업로드 파일 서빙 보안 필터 등록.
 *
 * <p>{@code /uploads/*} 요청을 정적 ResourceHandler(DispatcherServlet) 보다 앞서 가로채
 * 서명 검증 + 보안 헤더를 적용한다(서블릿 필터는 DispatcherServlet 보다 먼저 실행됨).
 */
@Configuration
public class FileServingSecurityConfig {

    /** 강제 모드 여부(기본 false=관대). 이번 라운드는 false 유지. */
    @Value("${file.sign.enforce:false}")
    private boolean enforce;

    @Bean
    public FilterRegistrationBean<FileServingFilter> fileServingFilter(FileUrlSigner fileUrlSigner) {
        FilterRegistrationBean<FileServingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new FileServingFilter(fileUrlSigner, enforce));
        registration.addUrlPatterns("/uploads/*");
        // 정적 리소스 서빙(ResourceHandler)보다 앞서 검증/헤더가 적용되도록 높은 우선순위.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("fileServingFilter");
        return registration;
    }
}
