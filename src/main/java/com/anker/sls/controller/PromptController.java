package com.anker.sls.controller;

import com.anker.sls.service.PromptService;
import com.anker.sls.service.impl.PromptServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.noear.solon.ai.annotation.PromptMapping;
import org.noear.solon.ai.chat.message.ChatMessage;
import org.noear.solon.ai.chat.prompt.ChatPrompt;
import org.noear.solon.annotation.Param;
import java.util.*;
import org.noear.solon.ai.mcp.server.annotation.McpServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@McpServerEndpoint(version = "1.0", sseEndpoint = "/mcp/sse/prompt")
public class PromptController {

    @Autowired
    private PromptService promptService;

    @GetMapping("/askQuestion")
    @PromptMapping(description = "生成关于某个主题的提问")
    public Collection<ChatPrompt> askQuestion(@Param(description = "主题") String topic) {
        return promptService.askQuestion(topic);
    }

    @GetMapping("/getSlsPrompt")
    @PromptMapping(description = "SLS日志分析助手系统提示词，详细说明日志分析三大分支（traceId/ID、上下文、关键字/时间），参数补全规范、日志库映射、工具调用流程与强制分析要求，指导大模型如何与用户进行日志分析相关多轮对话。")
    public Collection<ChatPrompt> getSlsPrompt() {
        log.info("已提供系统提示词");
        return promptService.getSlsPrompt();
    }
} 