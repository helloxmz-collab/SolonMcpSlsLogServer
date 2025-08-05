package com.anker.sls.config;

import org.noear.solon.ai.chat.tool.MethodToolProvider;
import org.noear.solon.ai.mcp.server.McpServerEndpointProvider;
import org.noear.solon.ai.mcp.server.annotation.McpServerEndpoint;
import org.noear.solon.ai.mcp.server.prompt.MethodPromptProvider;
import org.noear.solon.ai.mcp.server.resource.MethodResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;


import org.springframework.core.annotation.AnnotationUtils;

@Component
public class McpEndpointRegistrar implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger log = LoggerFactory.getLogger(McpEndpointRegistrar.class);
    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("================ MCP端点注册器启动 =================");
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        int count = 0;
        for (String beanName : beanNames) {
            Object bean = applicationContext.getBean(beanName);
            Class<?> beanClass = AopUtils.getTargetClass(bean);
            McpServerEndpoint anno = AnnotationUtils.findAnnotation(beanClass, McpServerEndpoint.class);
            if (anno == null) {
                continue;
            }
            log.info("【注册MCP端点】类名: {}, 端点名: {}", beanClass.getName(), anno.name());
            McpServerEndpointProvider serverEndpointProvider = McpServerEndpointProvider.builder()
                    .from(beanClass, anno)
                    .build();
            serverEndpointProvider.addTool(new MethodToolProvider(beanClass, bean));
            serverEndpointProvider.addResource(new MethodResourceProvider(beanClass, bean));
            serverEndpointProvider.addPrompt(new MethodPromptProvider(beanClass, bean));
            serverEndpointProvider.postStart();
            count++;
        }
        log.info("[状态=完成] 描述=MCP端点注册器初始化完成 注册数量={}", count);
        log.info("================ MCP端点注册器完成 ==================\n");
    }
} 