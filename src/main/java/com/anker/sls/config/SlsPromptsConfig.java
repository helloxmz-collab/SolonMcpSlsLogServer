package com.anker.sls.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SLS提示词配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "sls.prompts")
public class SlsPromptsConfig {
    
    /**
     * 系统角色提示词
     */
    private String systemRole;
    
    /**
     * 用户引导提示词
     */
    private String userGuide;
} 