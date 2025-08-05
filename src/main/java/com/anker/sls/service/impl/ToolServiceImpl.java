package com.anker.sls.service.impl;

import com.anker.sls.service.ToolService;
import org.springframework.stereotype.Service;

@Service
public class ToolServiceImpl implements ToolService {
    @Override
    public String hello(String name) {
        return "hello world: " + name;
    }

    @Override
    public String hello2(String name) throws Exception {
        Thread.sleep(10);
        return "hello world: " + name;
    }

    @Override
    public int add(int a, int b) {
        return a + b;
    }

    @Override
    public String reverseString(String input) {
        return new StringBuilder(input).reverse().toString();
    }

    @Override
    public String getCurrentTime() {
        return java.time.LocalDateTime.now().toString();
    }

    @Override
    public boolean isEven(int number) {
        return number % 2 == 0;
    }
} 