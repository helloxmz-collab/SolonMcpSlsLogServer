package com.anker.sls.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 阿里云SLS配置类
 */
@Component
@ConfigurationProperties(prefix = "aliyun.sls")
public class AliyunSLSConfig {
    
    private String accessKeyId;
    private String accessKeySecret;
    private List<Map<String, String>> slsPrompts;
    
    public String getAccessKeyId() {
        return accessKeyId;
    }
    
    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }
    
    public String getAccessKeySecret() {
        return accessKeySecret;
    }
    
    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public List<Map<String, String>> getSlsPrompts() {
        return slsPrompts;
    }

    public void setSlsPrompts(List<Map<String, String>> slsPrompts) {
        this.slsPrompts = slsPrompts;
    }
} 