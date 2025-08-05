package com.anker.sls.config;

import lombok.extern.slf4j.Slf4j;
import org.noear.solon.Solon;
import org.noear.solon.web.servlet.SolonServletFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 这个类独立一个目录，可以让 Solon 扫描范围最小化
 * */
@Slf4j
@Configuration
public class McpServerConfig {
    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @PostConstruct
    public void start() {
        log.info("[McpServerConfig] 方法=start 状态=启动 描述=Solon MCP服务启动");
        log.info("【启动MCP服务】准备启动，加载配置文件: mcpserver.yml");
        System.setProperty("server.contextPath", contextPath);
        Solon.start(McpServerConfig.class, new String[]{"--cfg=mcpserver.yml"});
        int port = 0;
        try {
            port = Integer.parseInt(System.getProperty("server.port", "0"));
        } catch (Exception e) {
            port = Solon.cfg().getInt("server.port", 0);
        }
        log.info("【MCP服务监听端口】当前端口号为:{}", port);
        log.info("[McpServerConfig] 方法=start 状态=完成 描述=Solon MCP服务启动完成");
    }

    @PreDestroy
    public void stop() {
        log.info("[McpServerConfig] 方法=stop 状态=准备 描述=Solon MCP服务准备关闭");
        if (Solon.app() != null) {
            Solon.stopBlock(false, Solon.cfg().stopDelay());
            log.info("【MCP服务已停止】");
        }
        log.info("[McpServerConfig] 方法=stop 状态=完成 描述=Solon MCP服务关闭完成");
    }

    @Bean
    public FilterRegistrationBean mcpServerFilter() {
        log.info("[McpServerConfig] 方法=mcpServerFilter 状态=初始化 描述=开始注册Solon Servlet过滤器");
        FilterRegistrationBean<SolonServletFilter> filter = new FilterRegistrationBean<>();
        filter.setName("SolonFilter");
        filter.addUrlPatterns("/mcp/*");
        filter.setFilter(new SolonServletFilter());
        log.info("[McpServerConfig] 方法=mcpServerFilter 状态=完成 描述=Solon Servlet过滤器已初始化");
        return filter;
    }
}