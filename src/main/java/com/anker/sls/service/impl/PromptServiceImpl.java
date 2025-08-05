package com.anker.sls.service.impl;

import com.anker.sls.config.SlsPromptsConfig;
import com.anker.sls.service.PromptService;
import org.noear.solon.ai.chat.message.ChatMessage;
import org.noear.solon.ai.chat.prompt.ChatPrompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
public class PromptServiceImpl implements PromptService {

    @Autowired
    private SlsPromptsConfig slsPromptsConfig;

    @Override
    public Collection<ChatPrompt> askQuestion(String topic) {
        return Arrays.asList(
                ChatPrompt.of(ChatMessage.ofUser("请解释一下'" + topic + "'的概念？"))
        );
    }

    @Override
    public Collection<ChatPrompt> getSlsPrompt() {
        List<ChatPrompt> messages = new ArrayList<>();

        // 从配置文件读取系统角色提示词
        messages.add(ChatPrompt.of(ChatMessage.ofSystem(slsPromptsConfig.getSystemRole())));

        // 从配置文件读取用户引导提示词
        messages.add(ChatPrompt.of(ChatMessage.ofUser(slsPromptsConfig.getUserGuide())));

        return messages;
    }
} 