package com.prafta.common.config;

import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class ApiPrefixConfig implements WebMvcRegistrations, WebMvcConfigurer {

    /** ✅ CORS 전역 허용 설정 */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    /** ✅ Prefix 자동 등록 핸들러 매핑 */
    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        ApiPrefixHandlerMapping mapping = new ApiPrefixHandlerMapping();
        // Boot 2.6+에서 PathPattern 파서 비활성화(ant_path_matcher 강제)
        mapping.setPatternParser(null);
        return mapping;
    }

    static class ApiPrefixHandlerMapping extends RequestMappingHandlerMapping {
        private static final String WEB_PACKAGE = "com.prafta.web";
        private static final String APP_PACKAGE = "com.prafta.app";
        private static final String COM_PACKAGE = "com.prafta.common";

        private static final String WEB_API_PREFIX = "/prafta/webApi";
        private static final String APP_API_PREFIX = "/prafta/appApi";
        private static final String COM_API_PREFIX = "/prafta/comApi";

        @Override
        protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
            Class<?> beanType = method.getDeclaringClass();
            String packageName = beanType.getPackageName();

            String prefix = null;
            if (packageName.startsWith(WEB_PACKAGE)) {
                prefix = WEB_API_PREFIX;
            } else if (packageName.startsWith(APP_PACKAGE)) {
                prefix = APP_API_PREFIX;
            } else if (packageName.startsWith(COM_PACKAGE)) {
                prefix = COM_API_PREFIX;
            }

            if (prefix != null && mapping != null && mapping.getPatternsCondition() != null) {
                Set<String> originalPatterns = mapping.getPatternsCondition().getPatterns();
                final String finalPrefix = prefix; // ✅ 람다에서 사용하기 위해 final 선언

                Set<String> updatedPatterns = originalPatterns.stream()
                        .map(url -> finalPrefix + (url.startsWith("/") ? url : "/" + url))
                        .collect(Collectors.toSet());

                // 2) 각 조건들을 String/배열로 안전 변환
                // methods
                RequestMethod[] methods = mapping.getMethodsCondition().getMethods()
                        .toArray(new RequestMethod[0]);

                // params / headers: NameValueExpression -> String
                String[] params = mapping.getParamsCondition().getExpressions().stream()
                        .map(Object::toString)
                        .toArray(String[]::new);

                String[] headers = mapping.getHeadersCondition().getExpressions().stream()
                        .map(Object::toString)
                        .toArray(String[]::new);

                // consumes / produces: MediaType -> String (e.g., "application/json")
                String[] consumes = mapping.getConsumesCondition().getConsumableMediaTypes().stream()
                        .map(mt -> mt.toString())
                        .toArray(String[]::new);

                String[] produces = mapping.getProducesCondition().getProducibleMediaTypes().stream()
                        .map(mt -> mt.toString())
                        .toArray(String[]::new);

                // 3) 새로운 RequestMappingInfo 생성
                RequestMappingInfo.Builder builder = RequestMappingInfo
                        .paths(updatedPatterns.toArray(new String[0]))
                        .methods(methods)
                        .params(params)
                        .headers(headers);

                if (consumes.length > 0) {
                    builder.consumes(consumes);
                }
                if (produces.length > 0) {
                    builder.produces(produces);
                }
                if (mapping.getName() != null) {
                    builder.mappingName(mapping.getName());
                }
                if (mapping.getCustomCondition() != null) {
                    builder.customCondition(mapping.getCustomCondition());
                }

                mapping = builder.build();
            }

            super.registerHandlerMethod(handler, method, mapping);
        }
    }
}
