package com.prafta;

import java.util.TimeZone;

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
		// 서버 기본 TimeZone 을 Asia/Seoul 로 고정.
		//   LocalDate.now() / Spring Scheduling cron / 대시보드 "오늘" 산출이 모두 JVM 기본 TZ 를
		//   KST 로 가정하고 있으나 그동안 강제되지 않았다. 배포 서버가 UTC 면 자정~오전 9시 구간에
		//   날짜가 하루 어긋난다. SpringApplication.run 이전에 설정해 이후 모든 초기화에 반영한다.
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		SpringApplication.run(MainApplication.class, args);
	}
}