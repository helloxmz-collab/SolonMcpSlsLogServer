package com.anker.sls.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 * 配置分页插件等
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 配置 MyBatis-Plus 插件
     * 包含分页插件等
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 添加分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 设置最大单页限制数量，默认 500 条，-1 不受限制
        paginationInterceptor.setMaxLimit(1000L);
        // 设置请求的页面大于最大页后操作，true调回到首页，false继续请求
        paginationInterceptor.setOverflow(false);
        
        interceptor.addInnerInterceptor(paginationInterceptor);
        
        return interceptor;
    }
} 