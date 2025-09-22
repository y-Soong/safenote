package com.prafta;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(scanBasePackages = {
	    "com.prafta.common",
	    "com.prafta.app",
	    "com.prafta.web"
	})
@EnableAspectJAutoProxy
@ComponentScan(basePackages = "com.prafta")
@MapperScan(basePackages = "com.prafta.**.**.mapper")
public class MainApplication {
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}