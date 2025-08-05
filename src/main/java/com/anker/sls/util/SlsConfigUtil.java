package com.anker.sls.util;

import com.anker.sls.config.AliyunSLSConfig;
import com.anker.sls.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;

public class SlsConfigUtil {
    private static final Logger log = LoggerFactory.getLogger(SlsConfigUtil.class);

    /**
     * 根据系统名称获取endpoint和project
     * @param systemName 系统名称
     * @param aliyunSLSConfig 配置bean
     * @return [endpoint, project]
     */
    public static String[] resolveEndpointAndProject(String systemName, AliyunSLSConfig aliyunSLSConfig) {
        if (systemName == null || systemName.trim().isEmpty()) {
            throw new BusinessException("系统名称不能为空", 400);
        }
        if (aliyunSLSConfig == null || aliyunSLSConfig.getSlsPrompts() == null) {
            log.error("SLS配置未初始化或配置为空");
            throw new BusinessException("SLS配置未初始化，请检查配置文件", 500);
        }
        List<Map<String, String>> prompts = aliyunSLSConfig.getSlsPrompts();
        for (Map<String, String> item : prompts) {
            if (item != null && item.get("keyword") != null && item.get("keyword").equals(systemName)) {
                String endpoint = item.get("endpoint");
                String project = item.get("project");
                if (endpoint == null || endpoint.trim().isEmpty()) {
                    log.error("系统 {} 的endpoint配置为空", systemName);
                    throw new BusinessException("系统" + systemName + "的endpoint配置为空", 500);
                }
                if (project == null || project.trim().isEmpty()) {
                    log.error("系统 {} 的project配置为空", systemName);
                    throw new BusinessException("系统" + systemName + "的project配置为空", 500);
                }
                return new String[]{endpoint.trim(), project.trim()};
            }
        }
        log.error("不支持的系统名称: {}", systemName);
        throw new BusinessException("不支持的系统名称: " + systemName + "，请检查系统名称是否正确", 400);
    }
} 