package com.anker.sls.controller;

import org.noear.solon.ai.annotation.ToolMapping;
import org.noear.solon.annotation.Param;
import org.springframework.stereotype.Controller;
import org.noear.solon.ai.mcp.server.annotation.McpServerEndpoint;

@Controller
@McpServerEndpoint(version = "1.0", sseEndpoint = "/mcp/sse/tool")
public class ToolController {
    public String hello(String name) {
        return "hello world: " + name;
    }

    public String hello2(String name) throws Exception {
        Thread.sleep(10);
        return "hello world: " + name;
    }

    @ToolMapping(description = "加法计算器，返回两个数字的和")
    public int add(@Param(description = "第一个数字") int a, @Param(description = "第二个数字") int b) {
        return a + b;
    }

    @ToolMapping(description = "字符串反转，返回反转后的字符串")
    public String reverseString(@Param(description = "要反转的字符串") String input) {
        return new StringBuilder(input).reverse().toString();
    }

    @ToolMapping(description = "获取当前时间，返回时间字符串")
    public String getCurrentTime() {
        return java.time.LocalDateTime.now().toString();
    }

    @ToolMapping(description = "判断是否为偶数，返回true或false")
    public boolean isEven(@Param(description = "要判断的数字") int number) {
        return number % 2 == 0;
    }
} 