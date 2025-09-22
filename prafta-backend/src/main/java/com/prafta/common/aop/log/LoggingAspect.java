package com.prafta.common.aop.log;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* com.example.project.controller..*(..))")
    public Object logRequestAndResponseTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        String method = joinPoint.getSignature().toShortString();
        log.info("[Request] ¡æ {}", method);

        Object result = joinPoint.proceed();

        long duration = System.currentTimeMillis() - start;
        log.info("[Response] ¡ç {} ({}ms)", method, duration);
        return result;
    }
    
    @Pointcut("execution(* com.prafta..controller..*(..))")
    public void controllerMethods() {}

    @Before("controllerMethods()")
    public void logBefore(JoinPoint joinPoint) {
        log.info("[Before] Method: {}", joinPoint.getSignature().toShortString());
    }

    @AfterReturning(pointcut = "controllerMethods()", returning = "result")
    public void logAfter(JoinPoint joinPoint, Object result) {
        log.info("[After] Method: {}, Return: {}", joinPoint.getSignature().toShortString(), result);
    }
}