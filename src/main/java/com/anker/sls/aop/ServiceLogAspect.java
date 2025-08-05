package com.anker.sls.aop;

import com.anker.sls.util.LogUtil;
import com.anker.sls.util.SlsConfigUtil;

import cn.hutool.core.map.MapUtil;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.anker.sls.model.McpServiceLog;
import org.springframework.beans.factory.annotation.Autowired;

import com.anker.sls.config.AliyunSLSConfig;
import com.anker.sls.mapper.ServiceLogMapper;
import java.util.Date;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class ServiceLogAspect {
    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);

    @Autowired
    private ServiceLogMapper serviceLogMapper;

    @Autowired
    private AliyunSLSConfig aliyunSLSConfig;

    @Around("execution(public * com.anker.sls.controller..*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String method = signature.getName();
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();

        Map<String, Object> paramMap = new HashMap<>();
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                paramMap.put(paramNames[i], paramValues[i]);
            }
        }

        // 尝试提取常用参数
        String systemName = MapUtil.getStr(paramMap, "systemName");
        String logstore = MapUtil.getStr(paramMap, "logstore");
       

        long start = System.currentTimeMillis();
        McpServiceLog log = new McpServiceLog();
        log.setQueryParam(LogUtil.toJson(paramMap));
        log.setSystemName(systemName);
        log.setMethod(method);
        log.setLogstore(logstore);
        log.setCreateTime(new Date());
        log.setUpdateTime(new Date());
        log.setIsDeleted(false);
        if (systemName != null) {
            String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
            log.setEndpoint(ep[0]);
            log.setProject(ep[1]);
        }

        logger.info("[AOP-START] 方法={} 参数={}", method, LogUtil.toJson(paramMap));
        try {
            Object result = joinPoint.proceed();
            long cost = System.currentTimeMillis() - start;
            log.setDuration(cost);
            log.setResult("SUCCESS");
            logger.info("[AOP-END] 方法={} 耗时={}ms", method, cost);
            if (systemName != null) {
                serviceLogMapper.insert(log);
            }
            return result;
        } catch (Throwable ex) {
            long cost = System.currentTimeMillis() - start;
            log.setDuration(cost);
            log.setResult("FAIL");
            log.setErrorMsg(ex.getMessage());
            logger.error("[AOP-ERROR] 方法={} 耗时={}ms", method, cost);
            if (systemName != null) {
                serviceLogMapper.insert(log);
            }
            throw ex;
        }
    }
} 