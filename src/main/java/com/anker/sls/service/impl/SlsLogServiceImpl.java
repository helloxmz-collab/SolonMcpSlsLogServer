package com.anker.sls.service.impl;

import com.anker.sls.service.SlsLogService;
import com.anker.sls.util.JavaHttpUtil;
import com.anker.sls.util.ResponseUtil;
import com.anker.sls.util.SafeMapUtil;
import com.anker.sls.util.SlsConfigUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import com.anker.sls.exception.BusinessException;
import com.anker.sls.mapper.ServiceLogMapper;
import com.anker.sls.config.AliyunSLSConfig;
import com.anker.sls.config.SlsPromptsConfig;
import org.noear.solon.ai.chat.message.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import com.anker.sls.model.McpServiceLog;

/**
 * SlsLogServiceImpl
 * 日志服务实现，负责与阿里云SLS日志服务交互，提供日志查询、索引、项目、机器组等相关操作。
 * 统一异常抛出BusinessException，日志输出统一格式。
 */
@Slf4j
@Service("SlsLogServiceImpl")
public class SlsLogServiceImpl implements SlsLogService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private JavaHttpUtil httpUtil;

    @Autowired
    private AliyunSLSConfig aliyunSLSConfig;

    @Autowired
    private ServiceLogMapper mcpServiceLogMapper;
    
    // 私有方法：验证基础参数
    private void validateBasicParams(String logstore, String endpoint, String project) {
        if (logstore == null || logstore.trim().isEmpty()) {
            throw new BusinessException("logstore不能为空", 400);
        }
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new BusinessException("endpoint配置不能为空", 500);
        }
        if (project == null || project.trim().isEmpty()) {
            throw new BusinessException("project配置不能为空", 500);
        }
    }

    /**
     * 获取logstore列表
     * @return 返回logstore列表
     * @throws BusinessException 获取失败时抛出
     */
    @Override
    public List<Map<String, Object>> getLogstoreList(String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        try {
            String path = "/logstores";
            
            Map<String, Object> response = httpUtil.doGet(path, null, endpoint, project);
            
            List<Map<String, Object>> result = new ArrayList<>();
            
            if (SafeMapUtil.isSuccessResponse(response)) {
                String responseBody = SafeMapUtil.getResponseBody(response);
                if (responseBody != null) {
                    try {
                        Map<String, Object> bodyMap = objectMapper.readValue(responseBody, Map.class);
                        
                        if (bodyMap != null && bodyMap.containsKey("logstores")) {
                            Object logstoresObj = bodyMap.get("logstores");
                            if (logstoresObj instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<String> logstores = (List<String>) logstoresObj;
                                for (String store : logstores) {
                                    if (store != null && !store.trim().isEmpty()) {
                                        Map<String, Object> storeMap = new HashMap<>();
                                        storeMap.put("logstoreName", store.trim());
                                        result.add(storeMap);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("解析logstore列表响应失败: {}", e.getMessage());
                        throw new BusinessException("解析日志库列表响应失败: " + e.getMessage(), 500);
                    }
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException("获取日志库列表失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 查询日志
     * @param logstore 日志库名称
     * @param query 查询表达式
     * @param from 开始时间，毫秒数
     * @param to 结束时间，毫秒数
     * @param line 返回行数
     * @param reverse 是否倒序
     * @return 日志结果
     * @throws BusinessException 查询失败时抛出
     */
    @Override
    public Map<String, Object> getLogs(String logstore, String query, int from, int to, int line, boolean reverse, String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        try {
            // 参数验证
            if (logstore == null || logstore.trim().isEmpty()) {
                throw new BusinessException("logstore不能为空", 400);
            }
            if (endpoint == null || endpoint.trim().isEmpty()) {
                throw new BusinessException("endpoint不能为空", 500);
            }
            if (project == null || project.trim().isEmpty()) {
                throw new BusinessException("project不能为空", 500);
            }
            if (line <= 0) {
                throw new BusinessException("返回行数必须大于0", 400);
            }

            // 自动拼接 with_pack_meta
            String finalQuery;
            if (query == null || query.trim().isEmpty()) {
                finalQuery = "* | with_pack_meta";
            } else if (query.contains("with_pack_meta")) {
                finalQuery = query;
            } else {
                finalQuery = query + " | with_pack_meta";
            }
            
            // 使用GET请求获取日志
            String path = "/logstores/" + logstore;
            
            // 构建查询参数
            Map<String, String> params = new HashMap<>();
            params.put("type", "log");  // 必须参数
            params.put("from", String.valueOf(from));
            params.put("to", String.valueOf(to));
            params.put("query", finalQuery);
            params.put("line", String.valueOf(line));
            params.put("offset", "0");
            params.put("reverse", String.valueOf(reverse));
            
            // 使用doGet方法
            Map<String, Object> response = httpUtil.doGet(path, params, endpoint, project);
            
            Map<String, Object> result = new HashMap<>();
            
            if (response != null && response.containsKey("body")) {
                String responseBody = SafeMapUtil.getResponseBody(response);
                if (SafeMapUtil.isSuccessResponse(response)) {
                    try {
                        // 解析响应体
                        Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                        
                        // 检查并处理logs数组
                        if (responseMap.containsKey("logs")) {
                            // 将原始信息直接放入结果
                            result.putAll(responseMap);
                        } else {
                            // 构建兼容格式
                            result.put("count", 0);
                            result.put("logs", new ArrayList<>());
                            result.put("progress", "Complete");
                        }
                    } catch (Exception e) {
                        // 尝试作为List<Map>格式解析
                        try {
                            List<Map<String, Object>> logs = objectMapper.readValue(responseBody, List.class);
                            result.put("count", logs.size());
                            result.put("logs", logs);
                            result.put("progress", "Complete");
                        } catch (Exception ex) {
                            result.put("error", "解析日志响应失败: " + e.getMessage() + "，原始响应: " + responseBody);
                        }
                    }
                } else {
                    result.put("error", responseBody);
                }
            } else {
                result.put("count", 0);
                result.put("logs", new ArrayList<>());
                result.put("progress", "Complete");
            }
            
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("查询日志异常: " + e.getMessage(), 500);
        }
    }

    /**
     * 获取索引配置
     * @param logstore 日志库名称
     * @return 返回索引配置
     * @throws BusinessException 获取失败时抛出
     */
    @Override
    public Map<String, Object> getIndex(String logstore, String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        try {
            validateBasicParams(logstore, endpoint, project);
            String path = "/logstores/" + logstore.trim() + "/index";
            Map<String, Object> response = httpUtil.doGet(path, null, endpoint, project);
            return ResponseUtil.processResponse(response, "获取索引配置失败");
        } catch (Exception e) {
            throw new BusinessException("获取索引配置失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 获取项目信息
     * 
     * @return 返回项目信息
     */
    @Override
    public Map<String, Object> getProject(String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        
        try {
            String path = "";  // 获取项目信息的路径是根路径
            
            Map<String, Object> response = httpUtil.doGet(path, null, endpoint, project);
            
            Map<String, Object> result = new HashMap<>();
            
            // 检查response是否为null或不包含body
            if (response == null) {
                result.put("error", "获取项目信息失败: 响应为空");
                return result;
            }
            
            // 检查是否包含error字段
            if (response.containsKey("error")) {
                String errorMsg = (String) response.get("error");
                result.put("error", errorMsg);
                return result;
            }
            
            if (response.containsKey("body")) {
                String responseBody = (String) response.get("body");
                if (response.containsKey("statusCode") && (int)response.get("statusCode") >= 200 && (int)response.get("statusCode") < 300) {
                    try {
                        Map<String, Object> bodyMap = objectMapper.readValue(responseBody, Map.class);
                        result.putAll(bodyMap);
                    } catch (Exception e) {
                        result.put("error", "解析项目信息响应失败: " + e.getMessage());
                    }
                } else {
                    result.put("error", responseBody);
                }
            } else {
                result.put("error", "获取项目信息失败: 响应中不包含body");
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException("获取项目信息失败: " + e.getMessage(), 500);
        }
    }
    
    /**
     * 获取直方图数据
     */
    @Override
    public Map<String, Object> getHistograms(String logstore, long from, long to, String topic, String query, String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        try {
            String path = "/logstores/" + logstore + "/index";
            Map<String, String> params = new HashMap<>();
            params.put("type", "histogram");
            params.put("from", String.valueOf(from));
            params.put("to", String.valueOf(to));
            if (topic != null && !topic.isEmpty()) params.put("topic", topic);
            if (query != null && !query.isEmpty()) params.put("query", query);

            Map<String, Object> response = httpUtil.doGet(path, params, endpoint, project);
            return ResponseUtil.processDataResponse(response, "获取直方图数据失败");
        } catch (Exception e) {
            throw new BusinessException("获取直方图数据失败: " + e.getMessage(), 500);
        }
    }
    
    /**
     * 诊断SLS连接问题
     */
    @Override
    public Map<String, Object> diagnoseSLSConnection(String logstore, String systemName) {
        Map<String, Object> result = new HashMap<>();
        boolean connectionValid = false;
        // 直接测试连接
        try {
            Map<String, Object> connectionTest;
            if (logstore != null && !logstore.trim().isEmpty()) {
                connectionTest = getIndex(logstore.trim(), systemName);
                result.put("logstoreTestResult", connectionTest);
                connectionValid = connectionTest != null && !connectionTest.containsKey("error");
            } else {
                connectionTest = getProject(systemName);
                result.put("projectTestResult", connectionTest);
                connectionValid = connectionTest != null && !connectionTest.containsKey("error");
            }
            if (connectionValid) {
                List<Map<String, Object>> logstores = getLogstoreList(systemName);
                if (logstores != null) {
                    result.put("logstores", logstores);
                    result.put("logstoreCount", logstores.size());
                } else {
                    result.put("logstores", new ArrayList<>());
                    result.put("logstoreCount", 0);
                }
            }
        } catch (Exception e) {
            connectionValid = false;
            result.put("error", e.getMessage());
        }
        result.put("connectionValid", connectionValid);
        result.put("message", connectionValid ? "SLS连接正常" : "SLS连接失败，请检查配置和网络");
        return result;
    }

    @Override
    public Map<String, Object> listProject(String projectName, Integer offset, Integer size, String resourceGroupId, String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        
        try {
            String path = "/"; // 根路径
            Map<String, String> params = new HashMap<>();
            if (projectName != null && !projectName.isEmpty()) params.put("projectName", projectName);
            if (offset != null) params.put("offset", String.valueOf(offset));
            if (size != null) params.put("size", String.valueOf(size));
            if (resourceGroupId != null && !resourceGroupId.isEmpty()) params.put("resourceGroupId", resourceGroupId);

            Map<String, Object> response = httpUtil.doGet(path, params, endpoint, project);

            Map<String, Object> result = new HashMap<>();
            if (response != null && response.containsKey("body")) {
                String responseBody = (String) response.get("body");
                if (response.containsKey("statusCode") && (int)response.get("statusCode") >= 200 && (int)response.get("statusCode") < 300) {
                    Map<String, Object> bodyMap = objectMapper.readValue(responseBody, Map.class);
                    result.putAll(bodyMap);
                } else {
                    result.put("error", responseBody);
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException("获取项目信息失败: " + e.getMessage(), 500);
        }
    }
    
    @Override
    public Map<String, Object> queryLogsBySql(String query, Boolean powerSql, String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        
        try {
            String path = "/logs";
            Map<String, String> params = new HashMap<>();
            params.put("query", query);
            if (powerSql != null) params.put("powerSql", powerSql.toString());
            Map<String, Object> response = httpUtil.doGet(path, params, endpoint, project);

            Map<String, Object> result = new HashMap<>();
            if (response != null && response.containsKey("body")) {
                String responseBody = (String) response.get("body");
                if (response.containsKey("statusCode") && (int)response.get("statusCode") >= 200 && (int)response.get("statusCode") < 300) {
                    try {
                        Object bodyObj = objectMapper.readValue(responseBody, Object.class);
                        result.put("data", bodyObj);
                    } catch (Exception e) {
                        result.put("raw", responseBody);
                    }
                } else {
                    result.put("error", responseBody);
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException("获取日志信息失败: " + e.getMessage(), 500);
        }
    }
    
    @Override
    public Map<String, Object> getLogging(String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        
        try {
            String path = "/logging";
            Map<String, Object> response = httpUtil.doGet(path, null, endpoint, project);

            Map<String, Object> result = new HashMap<>();
            if (response != null && response.containsKey("body")) {
                String responseBody = (String) response.get("body");
                if (response.containsKey("statusCode") && (int)response.get("statusCode") >= 200 && (int)response.get("statusCode") < 300) {
                    try {
                        Map<String, Object> bodyMap = objectMapper.readValue(responseBody, Map.class);
                        result.putAll(bodyMap);
                    } catch (Exception e) {
                        result.put("raw", responseBody);
                    }
                } else {
                    result.put("error", responseBody);
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException("获取日志信息失败: " + e.getMessage(), 500);
        }
    }
    
    @Override
    public Map<String, Object> getLogstore(String logstore, String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        return getLogstore(logstore, ep[0], ep[1]);
    }
    private Map<String, Object> getLogstore(String logstore, String endpoint, String project) {
        try {
            String path = "/logstores/" + logstore;
            Map<String, Object> response = httpUtil.doGet(path, null, endpoint, project);
            return ResponseUtil.processResponse(response, "获取Logstore信息失败");
        } catch (Exception e) {
            throw new BusinessException("获取Logstore信息失败: " + e.getMessage(), 500);
        }
    }
    
    @Override
    public List<Map<String, Object>> listShards(String logstore, String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        try {
            String path = "/logstores/" + logstore + "/shards";
            Map<String, Object> response = httpUtil.doGet(path, null, endpoint, project);
            return ResponseUtil.processListResponse(response, "获取Shard列表失败");
        } catch (Exception e) {
            throw new BusinessException("获取Shard列表失败: " + e.getMessage(), 500);
        }
    }
    
    @Override
    public Map<String, Object> getLogsPro(
            String logstore,
            Long from,
            Long to,
            String query,
            Integer line,
            Integer offset,
            Boolean reverse,
            Boolean powerSql,
            String topic,
            String systemName
    ) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        try {
            String path = "/logstores/" + logstore;
            Map<String, String> params = new HashMap<>();
            params.put("type", "log");
            params.put("from", String.valueOf(from));
            params.put("to", String.valueOf(to));

            // 新增：对 query 进行条件自动加引号处理
            query = quoteConditions(query);

            // 自动拼接 * | with_pack_meta
            String finalQuery;
            if (query == null || query.trim().isEmpty()) {
                finalQuery = "* | with_pack_meta";
            } else if (query.contains(" * | with_pack_meta")) {
                finalQuery = query;
            } else {
                finalQuery = query + "   * | with_pack_meta";
            }

            if (finalQuery != null && !finalQuery.isEmpty()) params.put("query", finalQuery);
            if (line != null) params.put("line", String.valueOf(line));
            if (offset != null) params.put("offset", String.valueOf(offset));
            if (reverse != null) params.put("reverse", String.valueOf(reverse));
            if (powerSql != null) params.put("powerSql", String.valueOf(powerSql));
            if (topic != null && !topic.isEmpty()) params.put("topic", topic);

            Map<String, Object> response = httpUtil.doGet(path, params, endpoint, project);

            Map<String, Object> result = new HashMap<>();
            if (response != null && response.containsKey("body")) {
                String responseBody = (String) response.get("body");
                if (response.containsKey("statusCode") && (int)response.get("statusCode") >= 200 && (int)response.get("statusCode") < 300) {
                    try {
                        Object bodyObj = objectMapper.readValue(responseBody, Object.class);
                        result.put("data", bodyObj);
                    } catch (Exception e) {
                        result.put("raw", responseBody);
                    }
                } else {
                    result.put("error", responseBody);
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException("获取日志失败: " + e.getMessage(), 500);
        }
    }
    
    // 新增：条件自动加引号方法
    private String quoteConditions(String query) {
        if (query == null || query.trim().isEmpty()) return query;
        // 匹配括号、AND、OR、NOT、已加引号内容、普通单词
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "\"[^\"]*\"|\\(|\\)|\\bAND\\b|\\bOR\\b|\\bNOT\\b|[^\\s()]+"
        );
        java.util.regex.Matcher matcher = pattern.matcher(query);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group();
            if (token.equals("(") || token.equals(")") ||
                token.equalsIgnoreCase("AND") ||
                token.equalsIgnoreCase("OR") ||
                token.equalsIgnoreCase("NOT")) {
                sb.append(token);
            } else if (token.startsWith("\"") && token.endsWith("\"")) {
                sb.append(token);
            } else {
                sb.append("\"").append(token).append("\"");
            }
            sb.append(" ");
        }
        return sb.toString().trim();
    }
    
    @Override
    public Map<String, Object> getCursor(String logstore, int shardId, String from, String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        try {
            String path = "/logstores/" + logstore + "/shards/" + shardId;
            Map<String, String> params = new HashMap<>();
            params.put("type", "cursor");
            params.put("from", from);

            Map<String, Object> response = httpUtil.doGet(path, params, endpoint, project);

            Map<String, Object> result = new HashMap<>();
            if (response != null && response.containsKey("body")) {
                String responseBody = (String) response.get("body");
                if (response.containsKey("statusCode") && (int)response.get("statusCode") >= 200 && (int)response.get("statusCode") < 300) {
                    try {
                        Map<String, Object> bodyMap = objectMapper.readValue(responseBody, Map.class);
                        result.putAll(bodyMap);
                    } catch (Exception e) {
                        result.put("raw", responseBody);
                    }
                } else {
                    result.put("error", responseBody);
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException("获取游标失败: " + e.getMessage(), 500);
        }
    }
    
    @Override
    public Map<String, Object> getCursorTime(String logstore, int shardId, String cursor, String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        try {
            String path = "/logstores/" + logstore + "/shards/" + shardId;
            Map<String, String> params = new HashMap<>();
            params.put("type", "cursor_time");
            params.put("cursor", cursor);

            Map<String, Object> response = httpUtil.doGet(path, params, endpoint, project);

            Map<String, Object> result = new HashMap<>();
            if (response != null && response.containsKey("body")) {
                String responseBody = (String) response.get("body");
                if (response.containsKey("statusCode") && (int)response.get("statusCode") >= 200 && (int)response.get("statusCode") < 300) {
                    try {
                        Map<String, Object> bodyMap = objectMapper.readValue(responseBody, Map.class);
                        result.putAll(bodyMap);
                    } catch (Exception e) {
                        result.put("raw", responseBody);
                    }
                } else {
                    result.put("error", responseBody);
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException("获取游标时间失败: " + e.getMessage(), 500);
        }
    }
    
    @Override
    public Map<String, Object> getContextLogs(String logstore, String packId, String packMeta, int backLines, int forwardLines, String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        try {
            // 直接使用传入的packId和packMeta查询上下文日志
            String path = "/logstores/" + logstore;
            Map<String, String> contextParams = new HashMap<>();
            contextParams.put("type", "context_log");  // 必需的参数，指定查询类型为上下文日志
            contextParams.put("pack_id", packId);
            contextParams.put("pack_meta", packMeta);
            contextParams.put("back_lines", String.valueOf(backLines));
            contextParams.put("forward_lines", String.valueOf(forwardLines));
            
            Map<String, Object> contextResponse = httpUtil.doGet(path, contextParams, endpoint, project);
            Map<String, Object> result = new HashMap<>();
            
            if (contextResponse != null && contextResponse.containsKey("body")) {
                String responseBody = (String) contextResponse.get("body");
                if (contextResponse.containsKey("statusCode") && (int)contextResponse.get("statusCode") >= 200 && (int)contextResponse.get("statusCode") < 300) {
                    try {
                        Map<String, Object> bodyMap = objectMapper.readValue(responseBody, Map.class);
                        result.putAll(bodyMap);
                        // 添加查询信息
                        result.put("pack_id", packId);
                        result.put("pack_meta", packMeta);
                    } catch (Exception e) {
                        result.put("raw", responseBody);
                        result.put("pack_id", packId);
                        result.put("pack_meta", packMeta);
                    }
                } else {
                    result.put("error", responseBody);
                    result.put("pack_id", packId);
                    result.put("pack_meta", packMeta);
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException("查询日志上下文失败: " + e.getMessage(), 500);
        }
    }
    
    @Override
    public Map<String, Object> getRawLogs(String logstore, String acceptEncoding, Map<String, Object> body, String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        try {
            String path = "/logstores/" + logstore + "/logs";
            Map<String, String> params = new HashMap<>();
            if (body != null) {
                for (Map.Entry<String, Object> entry : body.entrySet()) {
                    params.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            String jsonBody = objectMapper.writeValueAsString(body);
            Map<String, Object> response = httpUtil.doPost(path, params, jsonBody, endpoint, project);
            Map<String, Object> result = new HashMap<>();
            if (response != null && response.containsKey("body")) {
                String responseBody = (String) response.get("body");
                if (response.containsKey("statusCode") && (int)response.get("statusCode") >= 200 && (int)response.get("statusCode") < 300) {
                    Object bodyObj = objectMapper.readValue(responseBody, Object.class);
                    result.put("data", bodyObj);
                } else {
                    result.put("error", responseBody);
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException("获取原始日志失败: " + e.getMessage(), 500);
        }
    }
    
    @Override
    public Map<String, Object> getLogstoreMeteringMode(String logstore, String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        try {
            String path = "/logstores/" + logstore + "/meteringmode";
            Map<String, Object> response = httpUtil.doGet(path, null, endpoint, project);

            Map<String, Object> result = new HashMap<>();
            if (response != null && response.containsKey("body")) {
                String responseBody = (String) response.get("body");
                if (response.containsKey("statusCode") && (int)response.get("statusCode") >= 200 && (int)response.get("statusCode") < 300) {
                    try {
                        Map<String, Object> bodyMap = objectMapper.readValue(responseBody, Map.class);
                        result.putAll(bodyMap);
                    } catch (Exception e) {
                        result.put("raw", responseBody);
                    }
                } else {
                    result.put("error", responseBody);
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException("获取计量模式失败: " + e.getMessage(), 500);
        }
    }
    
    @Override
    public Map<String, Object> listMachineGroups(Integer offset, Integer size, String groupName, String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        try {
            String path = "/machinegroups";
            Map<String, String> params = new HashMap<>();
            if (offset != null) params.put("offset", String.valueOf(offset));
            if (size != null) params.put("size", String.valueOf(size));
            if (groupName != null && !groupName.isEmpty()) params.put("groupName", groupName);

            Map<String, Object> response = httpUtil.doGet(path, params, endpoint, project);

            Map<String, Object> result = new HashMap<>();
            if (response != null && response.containsKey("body")) {
                String responseBody = (String) response.get("body");
                if (response.containsKey("statusCode") && (int)response.get("statusCode") >= 200 && (int)response.get("statusCode") < 300) {
                    try {
                        Map<String, Object> bodyMap = objectMapper.readValue(responseBody, Map.class);
                        result.putAll(bodyMap);
                    } catch (Exception e) {
                        result.put("raw", responseBody);
                    }
                } else {
                    result.put("error", responseBody);
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException("获取机器组失败: " + e.getMessage(), 500);
        }
    }
    
    @Override
    public Map<String, Object> listMachines(String machineGroup, Integer offset, Integer size, String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        try {
            String path = "/machinegroups/" + machineGroup + "/machines";
            Map<String, String> params = new HashMap<>();
            if (offset != null) params.put("offset", String.valueOf(offset));
            if (size != null) params.put("size", String.valueOf(size));

            Map<String, Object> response = httpUtil.doGet(path, params, endpoint, project);

            Map<String, Object> result = new HashMap<>();
            if (response != null && response.containsKey("body")) {
                String responseBody = (String) response.get("body");
                if (response.containsKey("statusCode") && (int)response.get("statusCode") >= 200 && (int)response.get("statusCode") < 300) {
                    try {
                        Map<String, Object> bodyMap = objectMapper.readValue(responseBody, Map.class);
                        result.putAll(bodyMap);
                    } catch (Exception e) {
                        result.put("raw", responseBody);
                    }
                } else {
                    result.put("error", responseBody);
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException("获取机器失败: " + e.getMessage(), 500);
        }
    }
    
    @Override
    public Map<String, Object> getMachineGroup(String machineGroup, String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        try {
            String path = "/machinegroups/" + machineGroup;
            Map<String, Object> response = httpUtil.doGet(path, null, endpoint, project);

            Map<String, Object> result = new HashMap<>();
            if (response != null && response.containsKey("body")) {
                String responseBody = (String) response.get("body");
                if (response.containsKey("statusCode") && (int)response.get("statusCode") >= 200 && (int)response.get("statusCode") < 300) {
                    try {
                        Map<String, Object> bodyMap = objectMapper.readValue(responseBody, Map.class);
                        result.putAll(bodyMap);
                    } catch (Exception e) {
                        result.put("raw", responseBody);
                    }
                } else {
                    result.put("error", responseBody);
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException("获取机器组配置失败: " + e.getMessage(), 500);
        }
    }
    
    @Override
    public Map<String, Object> getAppliedConfigs(String machineGroup,  String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        String endpoint = ep[0];
        String project = ep[1];
        try {
            String path = "/machinegroups/" + machineGroup + "/configs";
            Map<String, Object> response = httpUtil.doGet(path, null, endpoint, project);

            Map<String, Object> result = new HashMap<>();
            if (response != null && response.containsKey("body")) {
                String responseBody = (String) response.get("body");
                if (response.containsKey("statusCode") && (int)response.get("statusCode") >= 200 && (int)response.get("statusCode") < 300) {
                    try {
                        Map<String, Object> bodyMap = objectMapper.readValue(responseBody, Map.class);
                        result.putAll(bodyMap);
                    } catch (Exception e) {
                        result.put("raw", responseBody);
                    }
                } else {
                    result.put("error", responseBody);
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException("获取机器组配置失败: " + e.getMessage(), 500);
        }
    }
    
    @Override
    public Map<String, Object> getAppliedMachineGroups(String configName, String systemName) {
        String[] ep = SlsConfigUtil.resolveEndpointAndProject(systemName, aliyunSLSConfig);
        return getAppliedMachineGroups(configName, ep[0], ep[1]);
    }
    private Map<String, Object> getAppliedMachineGroups(String configName, String endpoint, String project) {
        
        try {
            String path = "/configs/" + configName + "/machinegroups";
            Map<String, Object> response = httpUtil.doGet(path, null, endpoint, project);

            Map<String, Object> result = new HashMap<>();
            if (response != null && response.containsKey("body")) {
                String responseBody = (String) response.get("body");
                if (response.containsKey("statusCode") && (int)response.get("statusCode") >= 200 && (int)response.get("statusCode") < 300) {
                    try {
                        Map<String, Object> bodyMap = objectMapper.readValue(responseBody, Map.class);
                        result.putAll(bodyMap);
                    } catch (Exception e) {
                        result.put("raw", responseBody);
                    }
                } else {
                    result.put("error", responseBody);
                }
            }
            return result;
        } catch (Exception e) {
            throw new BusinessException("获取机器组配置失败: " + e.getMessage(), 500);
        }
    }
    
    @Override
    public IPage<McpServiceLog> getMcpServiceLog(Integer page, Integer size) {
        Page<McpServiceLog> pageObj = new Page<>(page, size);
        QueryWrapper<McpServiceLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        
        IPage<McpServiceLog> result = mcpServiceLogMapper.selectPage(pageObj, queryWrapper);
        
        log.info("分页查询结果: 总记录数={}, 当前页记录数={}, 总页数={}", 
                result.getTotal(), result.getRecords().size(), result.getPages());
        
        return result;
    }
} 