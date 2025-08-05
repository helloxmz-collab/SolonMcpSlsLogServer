package com.anker.sls.controller;

import org.noear.solon.ai.annotation.ResourceMapping;
import org.noear.solon.annotation.Param;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.noear.solon.ai.mcp.server.annotation.McpServerEndpoint;

@Controller
@McpServerEndpoint(version = "1.0", sseEndpoint = "/mcp/sse/resource")
public class ResourceController {
    @ResourceMapping(uri = "config://app-version", description = "获取应用版本号")
    public String getAppVersion() {
        return "v3.2.0";
    }

    @ResourceMapping(uri = "db://users/{user_id}/email", description = "根据用户ID查询邮箱")
    public String getEmail(@Param(description = "用户Id") @PathVariable String user_id) {
        return user_id + "@example.com";
    }
} 