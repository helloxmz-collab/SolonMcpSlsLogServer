package com.anker.sls.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * HTTP响应处理工具类
 * 统一处理SLS API响应的解析和错误处理逻辑
 */
@Slf4j
public class ResponseUtil {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 处理通用的HTTP响应，返回Map格式结果
     * 
     * @param response HTTP响应对象
     * @param errorPrefix 错误信息前缀
     * @return 处理后的结果Map
     */
    public static Map<String, Object> processResponse(Map<String, Object> response, String errorPrefix) {
        Map<String, Object> result = new HashMap<>();
        
        if (response == null) {
            result.put("error", errorPrefix + ": 响应为空");
            return result;
        }
        
        if (response.containsKey("error")) {
            result.put("error", errorPrefix + ": " + response.get("error"));
            return result;
        }
        
        if (!response.containsKey("body")) {
            result.put("error", errorPrefix + ": 响应中不包含body");
            return result;
        }
        
        String responseBody = SafeMapUtil.getResponseBody(response);
        int statusCode = SafeMapUtil.getStatusCode(response, 0);
        
        if (statusCode >= 200 && statusCode < 300) {
            try {
                Map<String, Object> bodyMap = objectMapper.readValue(responseBody, Map.class);
                result.putAll(bodyMap);
            } catch (Exception e) {
                log.warn("解析响应体JSON失败，返回原始内容: {}", e.getMessage());
                result.put("raw", responseBody);
            }
        } else {
            result.put("error", responseBody);
        }
        
        return result;
    }
    
    /**
     * 处理List类型的响应
     * 
     * @param response HTTP响应对象
     * @param errorPrefix 错误信息前缀
     * @return 处理后的List结果
     */
    public static List<Map<String, Object>> processListResponse(Map<String, Object> response, String errorPrefix) {
        if (response == null || !response.containsKey("body")) {
            log.error("{}: 响应为空或不包含body", errorPrefix);
            return Collections.emptyList();
        }
        
        String responseBody = SafeMapUtil.getResponseBody(response);
        int statusCode = SafeMapUtil.getStatusCode(response, 0);
        
        if (statusCode >= 200 && statusCode < 300) {
            try {
                List<Map<String, Object>> result = objectMapper.readValue(responseBody, List.class);
                return result != null ? result : Collections.emptyList();
            } catch (Exception e) {
                log.error("{}: 解析List响应失败: {}", errorPrefix, e.getMessage());
                return Collections.emptyList();
            }
        } else {
            log.error("{}: 请求失败，状态码: {}, 响应: {}", errorPrefix, statusCode, responseBody);
            return Collections.emptyList();
        }
    }
    
    /**
     * 处理对象类型的响应，支持data包装
     * 
     * @param response HTTP响应对象
     * @param errorPrefix 错误信息前缀
     * @return 处理后的结果Map
     */
    public static Map<String, Object> processDataResponse(Map<String, Object> response, String errorPrefix) {
        Map<String, Object> result = new HashMap<>();
        
        if (response == null || !response.containsKey("body")) {
            result.put("error", errorPrefix + ": 响应为空或不包含body");
            return result;
        }
        
        String responseBody = SafeMapUtil.getResponseBody(response);
        int statusCode = SafeMapUtil.getStatusCode(response, 0);
        
        if (statusCode >= 200 && statusCode < 300) {
            try {
                Object bodyObj = objectMapper.readValue(responseBody, Object.class);
                result.put("data", bodyObj);
            } catch (Exception e) {
                log.warn("解析数据响应JSON失败，返回原始内容: {}", e.getMessage());
                result.put("raw", responseBody);
            }
        } else {
            result.put("error", responseBody);
        }
        
        return result;
    }
    
    /**
     * 安全地获取字符串类型的响应内容
     * 
     * @param map 源Map
     * @param key 键名
     * @param defaultValue 默认值
     * @return 字符串值或默认值
     */
    public static String getString(Map<String, Object> map, String key, String defaultValue) {
        if (map == null || !map.containsKey(key)) {
            return defaultValue;
        }
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * 安全地获取整数类型的响应内容
     * 
     * @param map 源Map
     * @param key 键名
     * @param defaultValue 默认值
     * @return 整数值或默认值
     */
    public static Integer getInteger(Map<String, Object> map, String key, Integer defaultValue) {
        if (map == null || !map.containsKey(key)) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("无法解析整数值: {}", value);
                return defaultValue;
            }
        }
        return defaultValue;
    }
    
    /**
     * 检查响应是否成功
     * 
     * @param response HTTP响应对象
     * @return 是否成功
     */
    public static boolean isSuccessResponse(Map<String, Object> response) {
        return SafeMapUtil.isSuccessResponse(response);
    }
} 