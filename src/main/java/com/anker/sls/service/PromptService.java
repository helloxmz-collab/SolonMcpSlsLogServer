package com.anker.sls.service;

import org.noear.solon.ai.chat.message.ChatMessage;
import org.noear.solon.ai.chat.prompt.ChatPrompt;

import java.util.Collection;
import java.util.List;

public interface PromptService {
    Collection<ChatPrompt> askQuestion(String topic);
    Collection<ChatPrompt> getSlsPrompt();
} 