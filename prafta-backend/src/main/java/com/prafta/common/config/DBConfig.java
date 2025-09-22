package com.prafta.common.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.Configuration; // ★ 중요
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.JdbcType;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan; // ★ 권장
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@MapperScan(basePackages = "com.prafta.**.mapper") // ★ 매퍼 인터페이스 패키지 경로에 맞게 수정
public class DBConfig {

    @Value("${spring.datasource.url}")
    private String url;
    @Value("${spring.datasource.username}")
    private String username;
    @Value("${spring.datasource.password}")
    private String password;
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;

    @Bean
    public DataSource dataSource() {
        org.springframework.jdbc.datasource.DriverManagerDataSource ds =
                new org.springframework.jdbc.datasource.DriverManagerDataSource();
        ds.setDriverClassName(driverClassName);
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSource);

        // ★★★ MyBatis 전역 설정 직접 주입
        Configuration mybatisConfig = new Configuration();
        mybatisConfig.setMapUnderscoreToCamelCase(true); // SNAKE_CASE → camelCase 자동 매핑
        mybatisConfig.setJdbcTypeForNull(JdbcType.NULL); // (옵션) null 바인딩 안전장치
        // mybatisConfig.setCacheEnabled(true);          // (옵션) 2차 캐시 등 필요시
        factory.setConfiguration(mybatisConfig);

        // ★★★ 매퍼 XML 경로 지정 (properties의 mapper-locations 대신 수동 주입)
        factory.setMapperLocations(
            new PathMatchingResourcePatternResolver()
                .getResources("classpath*:mapper/**/*.xml")
        );

        // (옵션) Type Alias 패키지: DTO 패키지 전체를 별칭으로 쓰고 싶을 때
        // factory.setTypeAliasesPackage("com.prafta.web.**.dto");

        return factory.getObject();
    }

    @Bean
    public SqlSessionTemplate sqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
