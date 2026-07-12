package com.prafta.common.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.zaxxer.hikari.HikariDataSource;

/**
 * RAG(AI 검색)용 2차 데이터소스(pgvector Postgres) 배선.
 *
 * <p>★ 회귀 방지 핵심:
 *   기존 프로젝트의 MySQL 데이터소스는 {@code DBConfig}(무 @Configuration = 비활성 사실상 사장 코드)가 아니라
 *   Spring Boot 자동설정(DataSourceAutoConfiguration + mybatis-spring-boot-starter)이 {@code spring.datasource.*}
 *   로부터 생성한다. 그런데 {@code DataSourceAutoConfiguration} 은 {@code @ConditionalOnMissingBean(DataSource.class)}
 *   이므로, 여기서 2차 DataSource 빈을 추가하면 자동설정이 통째로 back-off 되어 <b>MySQL 기본 데이터소스가 사라진다</b>.
 *   따라서 MySQL primary 데이터소스를 여기서 <b>명시적으로 재선언(@Primary)</b> 하여 기존 MyBatis 배선을 그대로 보존한다.
 *   (현재 Boot 기본과 동일하게 HikariCP + {@code spring.datasource.*} 바인딩 → 런타임 동작 동일.)
 *
 * <p>AI 데이터소스는 <b>읽기 전용 조회 전용</b>이며 트랜잭션 매니저를 공유하지 않는다
 *   (aiJdbcTemplate 로만 사용, @Transactional 은 primary(MySQL) 매니저가 담당).
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiDbConfig {

    // ---------------------------------------------------------------------
    // 1) 기존 MySQL primary 데이터소스(명시적 재선언 — 자동설정 back-off 대비)
    // ---------------------------------------------------------------------

    /** {@code spring.datasource.*} 바인딩(url/username/password/driver-class-name). */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * MySQL primary 데이터소스. 현재 Boot 기본과 동일하게 HikariCP 로 생성한다.
     * mybatis-spring-boot-starter 의 {@code @ConditionalOnSingleCandidate(DataSource.class)} 가
     * 본 @Primary 빈을 단일 후보로 인식하여 기존 SqlSessionFactory 자동설정이 그대로 성립한다.
     */
    @Bean
    @Primary
    public DataSource dataSource(@Qualifier("dataSourceProperties") DataSourceProperties props) {
        return props.initializeDataSourceBuilder().build();
    }

    // ---------------------------------------------------------------------
    // 2) AI 코퍼스(pgvector Postgres) 2차 데이터소스 — 읽기 전용
    // ---------------------------------------------------------------------

    /** {@code prafta.ai.datasource.*} 바인딩(url/username/password/driver-class-name). */
    @Bean
    @ConfigurationProperties("prafta.ai.datasource")
    public DataSourceProperties aiDataSourceProperties() {
        return new DataSourceProperties();
    }

    /** pgvector 코퍼스 조회용 HikariCP 데이터소스. primary 아님(트랜잭션 매니저 미공유). */
    @Bean
    public DataSource aiDataSource(@Qualifier("aiDataSourceProperties") DataSourceProperties props) {
        return props.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    /** pgvector 벡터 검색 전용 JdbcTemplate(파라미터 바인딩만 사용 — SQL 주입 차단). */
    @Bean
    public JdbcTemplate aiJdbcTemplate(@Qualifier("aiDataSource") DataSource aiDataSource) {
        return new JdbcTemplate(aiDataSource);
    }
}
