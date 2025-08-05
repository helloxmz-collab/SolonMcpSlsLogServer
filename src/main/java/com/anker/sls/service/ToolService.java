package com.anker.sls.service;

public interface ToolService {
    String hello(String name);
    String hello2(String name) throws Exception;
    int add(int a, int b);
    String reverseString(String input);
    String getCurrentTime();
    boolean isEven(int number);
} 