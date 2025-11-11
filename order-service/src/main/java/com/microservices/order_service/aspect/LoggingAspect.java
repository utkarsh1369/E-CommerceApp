package com.microservices.order_service.aspect;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("execution(* com.microservices.order_service.service..*.*(..))")
    public Object logAroundService(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        long startTime = System.currentTimeMillis();

        log.info("Starting service method {} with args {}", methodName, Arrays.toString(args));
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("Completed {} | Duration: {} ms", methodName, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed {} | Duration: {} ms | Exception: {}", methodName, duration, e.getMessage());
            throw e;
        }
    }

    @Around("execution(* com.microservices.order_service.controller..*.*(..))")
    public Object logAroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        log.info("Starting API method {}", methodName);
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("Completed API {} | Duration: {} ms", methodName, duration);
            return result;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Failed API {} | Duration: {} ms | Exception: {}", methodName, duration, ex.getMessage());
            throw ex;
        }
    }
}
