package com.anker.sls.controller;

import org.noear.solon.ai.annotation.ToolMapping;
import org.noear.solon.annotation.Param;
import org.springframework.stereotype.Controller;
import org.noear.solon.ai.mcp.server.annotation.McpServerEndpoint;

@Controller
@McpServerEndpoint(version = "1.0", sseEndpoint = "/mcp/sse/weather")
public class WeatherController {
    @ToolMapping(description = "查询天气预报")
    public String getWeather(@Param(description = "城市位置") String location) {
        return "晴，14度";
    }
} 