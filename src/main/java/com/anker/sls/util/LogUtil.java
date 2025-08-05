package com.anker.sls.util;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 日志工具类，提供对象转JSON字符串等通用方法
 */
public class LogUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }

    // 已移除getTraceId方法

    public static String summarizeResult(Object result) {
        if (result == null) return "null";
        if (result instanceof java.util.Collection) return "Collection(size=" + ((java.util.Collection<?>) result).size() + ")";
        if (result instanceof java.util.Map) return "Map(size=" + ((java.util.Map<?, ?>) result).size() + ")";
        if (result.getClass().isArray()) return "Array(length=" + java.lang.reflect.Array.getLength(result) + ")";
        if (result instanceof String) return "String(length=" + ((String) result).length() + ")";
        return result.getClass().getSimpleName();
    }
} 