package com.anker.sls.model;

import java.util.Map;

/**
 * 日志查询参数对象
 */
public class LogQuery {
    /**
     * HTTP请求方法，GET 或 POST
     */
    private String method = "GET";
    
    /**
     * API路径，例如 "/logstores/my-logstore"
     */
    private String path;
    
    /**
     * URL查询参数
     */
    private Map<String, String> params;
    
    /**
     * POST请求体内容，仅当method为POST时有效
     */
    private String body;
    
    /**
     * 日志库名称
     */
    private String logstore;
    
    /**
     * 日志查询起始时间戳（秒）
     */
    private Long from;
    
    /**
     * 日志查询结束时间戳（秒）
     */
    private Long to;
    
    /**
     * 返回日志行数
     */
    private Integer line;
    
    /**
     * 是否倒序返回
     */
    private Boolean reverse = false;
    
    /**
     * 查询表达式
     */
    private String query;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getLogstore() {
        return logstore;
    }

    public void setLogstore(String logstore) {
        this.logstore = logstore;
    }

    public Long getFrom() {
        return from;
    }

    public void setFrom(Long from) {
        this.from = from;
    }

    public Long getTo() {
        return to;
    }

    public void setTo(Long to) {
        this.to = to;
    }

    public Integer getLine() {
        return line;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public Boolean getReverse() {
        return reverse;
    }

    public void setReverse(Boolean reverse) {
        this.reverse = reverse;
    }
    
    /**
     * 获取是否倒序返回，适配boolean类型的getter命名
     */
    public boolean isReverse() {
        return reverse != null ? reverse : false;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    @Override
    public String toString() {
        return "LogQuery{" +
                "logstore='" + logstore + '\'' +
                ", query='" + query + '\'' +
                ", from=" + from +
                ", to=" + to +
                ", line=" + line +
                ", reverse=" + reverse +
                '}';
    }
} 