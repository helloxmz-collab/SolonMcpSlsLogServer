package com.anker.sls.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 安全的Map操作工具类
 * 提供空值安全的类型转换和值获取方法
 */
@Slf4j
public class SafeMapUtil {
    
    /**
     * 安全地从Map中获取String值
     * 
     * @param map 源Map
     * @param key 键名
     * @param defaultValue 默认值
     * @return 字符串值或默认值
     */
    public static String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null || key == null || !map.containsKey(key)) {
            return defaultValue;
        }
        
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        return value.toString();
    }
    
    /**
     * 安全地从Map中获取Integer值
     * 
     * @param map 源Map
     * @param key 键名
     * @param defaultValue 默认值
     * @return 整数值或默认值
     */
    public static Integer getIntegerValue(Map<String, Object> map, String key, Integer defaultValue) {
        if (map == null || key == null || !map.containsKey(key)) {
            return defaultValue;
        }
        
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        if (value instanceof Integer) {
            return (Integer) value;
        }
        
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("无法解析整数值: {} for key: {}", value, key);
                return defaultValue;
            }
        }
        
        log.warn("无法转换为Integer类型: {} for key: {}", value.getClass().getSimpleName(), key);
        return defaultValue;
    }
    
    /**
     * 安全地从Map中获取Boolean值
     * 
     * @param map 源Map
     * @param key 键名
     * @param defaultValue 默认值
     * @return 布尔值或默认值
     */
    public static Boolean getBooleanValue(Map<String, Object> map, String key, Boolean defaultValue) {
        if (map == null || key == null || !map.containsKey(key)) {
            return defaultValue;
        }
        
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        
        if (value instanceof String) {
            String strValue = ((String) value).toLowerCase().trim();
            if ("true".equals(strValue) || "1".equals(strValue) || "yes".equals(strValue)) {
                return true;
            }
            if ("false".equals(strValue) || "0".equals(strValue) || "no".equals(strValue)) {
                return false;
            }
        }
        
        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }
        
        log.warn("无法转换为Boolean类型: {} for key: {}", value.getClass().getSimpleName(), key);
        return defaultValue;
    }
    
    /**
     * 安全地检查Map中是否包含指定键且值不为null
     * 
     * @param map 源Map
     * @param key 键名
     * @return 是否包含且值不为null
     */
    public static boolean hasNonNullValue(Map<String, Object> map, String key) {
        return map != null && key != null && map.containsKey(key) && map.get(key) != null;
    }
    
    /**
     * 安全地检查Map是否为空或null
     * 
     * @param map 源Map
     * @return 是否为空或null
     */
    public static boolean isNullOrEmpty(Map<String, Object> map) {
        return map == null || map.isEmpty();
    }
    
    /**
     * 安全地从响应Map中获取状态码
     * 
     * @param response 响应Map
     * @param defaultStatusCode 默认状态码
     * @return 状态码
     */
    public static int getStatusCode(Map<String, Object> response, int defaultStatusCode) {
        return getIntegerValue(response, "statusCode", defaultStatusCode);
    }
    
    /**
     * 安全地从响应Map中获取响应体
     * 
     * @param response 响应Map
     * @return 响应体字符串，可能为null
     */
    public static String getResponseBody(Map<String, Object> response) {
        return getStringValue(response, "body", null);
    }
    
    /**
     * 检查响应是否成功（状态码200-299）
     * 
     * @param response 响应Map
     * @return 是否成功
     */
    public static boolean isSuccessResponse(Map<String, Object> response) {
        if (response == null) {
            return false;
        }
        
        if (response.containsKey("error")) {
            return false;
        }
        
        int statusCode = getStatusCode(response, 0);
        return statusCode >= 200 && statusCode < 300;
    }
} 