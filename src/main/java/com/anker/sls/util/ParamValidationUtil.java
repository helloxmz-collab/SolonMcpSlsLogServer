package com.anker.sls.util;

import com.anker.sls.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 参数验证工具类
 * 统一处理SLS API参数的验证和默认值设置逻辑
 */
@Slf4j
public class ParamValidationUtil {
    
    // 支持的系统名称集合
    private static final Set<String> SUPPORTED_SYSTEMS = new HashSet<>(Arrays.asList(
        "广告", "标签", "标签平台", "AMDP", "目录标签", "目录标签平台", "红旗", "红旗系统"
    ));
    
    /**
     * 验证必需的字符串参数
     * 
     * @param value 参数值
     * @param paramName 参数名称
     * @throws BusinessException 参数无效时抛出
     */
    public static void validateRequired(String value, String paramName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(paramName + "不能为空", 400);
        }
    }
    
    /**
     * 验证系统名称是否支持
     * 
     * @param systemName 系统名称
     * @throws BusinessException 系统名称不支持时抛出
     */
    public static void validateSystemName(String systemName) {
        validateRequired(systemName, "系统名称");
        
        if (!SUPPORTED_SYSTEMS.contains(systemName)) {
            throw new BusinessException(
                "不支持的系统名称: " + systemName + 
                "，支持的系统: " + SUPPORTED_SYSTEMS, 400);
        }
    }
    
    /**
     * 验证时间戳参数
     * 
     * @param timestamp 时间戳
     * @param paramName 参数名称
     * @throws BusinessException 时间戳无效时抛出
     */
    public static void validateTimestamp(Long timestamp, String paramName) {
        if (timestamp == null || timestamp <= 0) {
            throw new BusinessException(paramName + "必须为正数", 400);
        }
        
        // 检查时间戳是否合理（不能太早或太晚）
        long currentTime = System.currentTimeMillis() / 1000;
        long oneYearAgo = currentTime - 365 * 24 * 60 * 60; // 一年前
        long oneYearLater = currentTime + 365 * 24 * 60 * 60; // 一年后
        
        if (timestamp < oneYearAgo || timestamp > oneYearLater) {
            log.warn("时间戳可能不合理: {} (当前时间: {})", timestamp, currentTime);
        }
    }
    
    /**
     * 验证时间范围参数
     * 
     * @param from 开始时间
     * @param to 结束时间
     * @throws BusinessException 时间范围无效时抛出
     */
    public static void validateTimeRange(Long from, Long to) {
        validateTimestamp(from, "开始时间");
        validateTimestamp(to, "结束时间");
        
        if (from >= to) {
            throw new BusinessException("开始时间必须小于结束时间", 400);
        }
        
        // 检查时间范围是否过大（超过30天可能会导致查询超时）
        long diffDays = (to - from) / (24 * 60 * 60);
        if (diffDays > 30) {
            log.warn("查询时间范围较大: {} 天，可能影响性能", diffDays);
        }
    }
    
    /**
     * 验证分页参数
     * 
     * @param offset 偏移量
     * @param size 页大小
     */
    public static void validatePaging(Integer offset, Integer size) {
        if (offset != null && offset < 0) {
            throw new BusinessException("偏移量不能为负数", 400);
        }
        
        if (size != null && (size <= 0 || size > 1000)) {
            throw new BusinessException("页大小必须在1到1000之间", 400);
        }
    }
    
    /**
     * 获取带默认值的整数参数
     * 
     * @param value 原始值
     * @param defaultValue 默认值
     * @param min 最小值
     * @param max 最大值
     * @param paramName 参数名称
     * @return 验证后的值
     */
    public static Integer getIntWithDefault(Integer value, Integer defaultValue, 
                                          Integer min, Integer max, String paramName) {
        Integer result = value != null ? value : defaultValue;
        
        if (result != null) {
            if (min != null && result < min) {
                throw new BusinessException(paramName + "不能小于" + min, 400);
            }
            if (max != null && result > max) {
                throw new BusinessException(paramName + "不能大于" + max, 400);
            }
        }
        
        return result;
    }
    
    /**
     * 获取带默认值的字符串参数
     * 
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 处理后的值
     */
    public static String getStringWithDefault(String value, String defaultValue) {
        return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
    }
    
    /**
     * 获取带默认值的布尔参数
     * 
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 处理后的值
     */
    public static Boolean getBooleanWithDefault(Boolean value, Boolean defaultValue) {
        return value != null ? value : defaultValue;
    }
    
    /**
     * 验证查询条件
     * 
     * @param query 查询条件
     * @param maxLength 最大长度
     */
    public static void validateQuery(String query, int maxLength) {
        if (query != null && query.length() > maxLength) {
            throw new BusinessException(
                "查询条件过长，最大允许" + maxLength + "个字符", 400);
        }
    }
    
    /**
     * 验证ID格式（用于traceId等）
     * 
     * @param id ID值
     * @param paramName 参数名称
     */
    public static void validateId(String id, String paramName) {
        validateRequired(id, paramName);
        
        // 简单的ID格式验证：只允许字母数字和连字符
        if (!id.matches("^[a-zA-Z0-9_-]+$")) {
            throw new BusinessException(paramName + "格式不正确，只允许字母、数字、下划线和连字符", 400);
        }
        
        if (id.length() < 8 || id.length() > 64) {
            throw new BusinessException(paramName + "长度必须在8到64个字符之间", 400);
        }
    }
    
    /**
     * 标准化时间参数处理
     * 
     * @param from 开始时间字符串
     * @param to 结束时间字符串
     * @return 时间戳数组 [fromTs, toTs]
     */
    public static long[] processTimeParams(String from, String to) {
        // 如果时间参数为空，使用默认时间范围（最近一个月）
        if ((from == null || from.trim().isEmpty()) && (to == null || to.trim().isEmpty())) {
            String defaultFrom = DateUtil.getBeforeOneMonthStr();
            String defaultTo = DateUtil.getNowStr();
            log.info("时间参数为空，自动设置查询范围：{} 到 {}", defaultFrom, defaultTo);
            return new long[]{
                DateUtil.parseDateToTimestamp(defaultFrom),
                DateUtil.parseDateToTimestamp(defaultTo)
            };
        }
        
        long fromTs = DateUtil.parseDateToTimestamp(from);
        long toTs = DateUtil.parseDateToTimestamp(to);
        
        validateTimeRange(fromTs, toTs);
        
        return new long[]{fromTs, toTs};
    }
} 