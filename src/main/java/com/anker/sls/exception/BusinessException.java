package com.anker.sls.exception;

/**
 * 业务异常类，用于统一业务异常抛出和处理
 */
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message, int code) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
} 