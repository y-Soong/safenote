package com.prafta;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = { "com.prafta.common", "com.prafta.app", "com.prafta.web" })
@MapperScan(basePackages = "com.prafta.**.**.mapper")
@ComponentScan(basePackages = "com.prafta")
@EnableAspectJAutoProxy
@EnableScheduling
@EnableAsync // PRAFTA-037-F6: @Async 비동기 워크로드(사용자 일괄 생성 잡) 활성화
public class MainApplication {
	public static void main(String[] args) {
		SpringApplication.run(MainApplication.class, args);
	}
}