package com.markethub.common.util;

import com.markethub.common.exception.R;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/** 操作审计日志切面：记录请求入参、耗时和结果。 */
@Slf4j
@Aspect
@Component
public class AuditLogAspect {

    @Around("execution(* com.markethub..controller.*.*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String args = Arrays.toString(joinPoint.getArgs());

        log.info("[审计日志] 请求: {}.{} 参数: {}", className, methodName, args.length() > 200 ? args.substring(0, 200) + "..." : args);

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Exception e) {
            log.error("[审计日志] 异常: {}.{} 异常: {}", className, methodName, e.getMessage());
            throw e;
        }

        long cost = System.currentTimeMillis() - start;
        if (result instanceof R) {
            R<?> r = (R<?>) result;
            log.info("[审计日志] 响应: {}.{} 耗时:{}ms code:{} msg:{}",
                    className, methodName, cost, r.getCode(), r.getMessage());
        } else {
            log.info("[审计日志] 响应: {}.{} 耗时:{}ms", className, methodName, cost);
        }

        return result;
    }
}
