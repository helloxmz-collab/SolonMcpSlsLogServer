package com.anker.sls.config;

import org.noear.solon.annotation.Component;
import org.noear.solon.core.handle.Context;
import org.noear.solon.core.handle.Filter;
import org.noear.solon.core.handle.FilterChain;

import lombok.extern.slf4j.Slf4j;

/**
 * 仅为示例（也可以用 Servlet 过滤器）
 */
@Slf4j
@Component
public class McpServerAuth implements Filter {
    @Override
    public void doFilter(Context ctx, FilterChain chain) throws Throwable {
       String authStr = ctx.param("user");
       boolean authSuccess = !"no".equals(authStr);
       // 鉴权逻辑统一
       if (!authSuccess) {
           ctx.status(401);
           ctx.setHandled(true);
           log.info("鉴权失败，用户名: {}", authStr);
           return;
       }
        chain.doFilter(ctx);
    }
}
