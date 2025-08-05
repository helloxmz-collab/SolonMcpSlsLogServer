package com.anker.sls.util;

import com.anker.sls.config.AliyunSLSConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Java原生HTTP请求工具类，用于SLS API调用
 */
@Component
public class JavaHttpUtil {

    private static final Logger log = LoggerFactory.getLogger(JavaHttpUtil.class);

    @Value("${aliyun.sls.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.sls.access-key-secret}")
    private String accessKeySecret;

    @Autowired
    private AliyunSLSConfig config;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Gson gson = new Gson();
    private static final String CONTENT_TYPE = "application/json";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_MD5_HEADER = "Content-MD5";
    private static final String DATE_HEADER = "Date";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String SLS_HEADER_PREFIX = "x-log-";
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final String DEFAULT_ENCODING = "UTF-8";
    private static final Locale LOCALE_US = Locale.US;
    private static final TimeZone UTC_TIMEZONE = TimeZone.getTimeZone("GMT");
    private static final SimpleDateFormat RFC_822_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    static {
        RFC_822_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * 发送HTTP请求到SLS服务
     *
     * @param method      HTTP方法 (GET, POST, PUT, DELETE等)
     * @param url         请求URL
     * @param params      URL参数
     * @param headers     请求头
     * @param requestBody 请求体 (用于POST/PUT请求)
     * @param accessKeyId 阿里云访问密钥ID
     * @param accessKeySecret 阿里云访问密钥Secret
     * @return 响应体字符串
     */
    public static String sendRequest(String method, String url, Map<String, String> params, 
                                    Map<String, String> headers, String requestBody,
                                    String accessKeyId, String accessKeySecret) {
        try {
            // 处理URL参数
            if (params != null && !params.isEmpty()) {
                StringBuilder urlBuilder = new StringBuilder(url);
                if (!url.contains("?")) {
                    urlBuilder.append("?");
                } else if (!url.endsWith("&")) {
                    urlBuilder.append("&");
                }
                
                List<String> sortedParams = new ArrayList<>();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    sortedParams.add(URLEncoder.encode(entry.getKey(), DEFAULT_ENCODING) +
                            "=" +
                            URLEncoder.encode(entry.getValue(), DEFAULT_ENCODING));
                }
                Collections.sort(sortedParams);
                urlBuilder.append(String.join("&", sortedParams));
                url = urlBuilder.toString();
            }
            
            // 安全地提取资源路径
            String resourcePath = "";
            try {
                int slashIndex = url.indexOf("/", 8); // 跳过https://
                if (slashIndex != -1) {
                    String fullPath = url.substring(slashIndex);
                    int questionMarkIndex = fullPath.indexOf("?");
                    if (questionMarkIndex != -1) {
                        resourcePath = fullPath.substring(0, questionMarkIndex);
                    } else {
                        resourcePath = fullPath;
                    }
                } else {
                    // 如果URL中没有路径部分，则使用默认根路径
                    resourcePath = "/";
                }
            } catch (Exception e) {
                // 如果提取出现任何问题，默认使用根路径
                resourcePath = "/";
                log.warn("提取资源路径时出错: {}, URL: {}", e.getMessage(), url);
            }
            
            // 构建认证头
            Map<String, String> authHeaders = new HashMap<>();
            if (headers != null) {
                authHeaders.putAll(headers);
            }
            
            // 添加日期头
            String dateHeader = RFC_822_DATE_FORMAT.format(new Date());
            authHeaders.put(DATE_HEADER, dateHeader);
            
            // 添加内容MD5和内容类型
            String contentMD5 = "";
            // 使用纯application/json类型
            String contentType = "application/json";
            authHeaders.put(CONTENT_TYPE_HEADER, contentType);
            
            if (requestBody != null && !requestBody.isEmpty()) {
                // 计算MD5，确保使用标准方法计算
                contentMD5 = calculateMD5(requestBody); 
                authHeaders.put(CONTENT_MD5_HEADER, contentMD5);
            }
            
            // 构建规范化的头部
            String canonicalizedHeaders = buildCanonicalizedHeaders(authHeaders);
            
            // 构建规范化资源 - 包括查询参数
            String canonicalizedResource = resourcePath;
            if (url.contains("?")) {
                String queryString = url.substring(url.indexOf("?") + 1);
                // 处理查询参数
                String[] queryParams = queryString.split("&");
                List<String> sortedQueryParams = new ArrayList<>();
                for (String param : queryParams) {
                    // 只处理需要签名的参数
                    String paramName = param.split("=")[0];
                    if (shouldSignParameter(paramName)) {
                        sortedQueryParams.add(param);
                    }
                }
                
                if (!sortedQueryParams.isEmpty()) {
                    Collections.sort(sortedQueryParams);
                    canonicalizedResource += "?" + String.join("&", sortedQueryParams);
                }
            }
            
            // 打印调试信息
            log.debug("请求方法: {}", method);
            log.debug("请求URL: {}", url);
            log.debug("资源路径: {}", resourcePath);
            log.debug("规范化头部: {}", canonicalizedHeaders);
            log.debug("规范化资源: {}", canonicalizedResource);
            
            // 计算签名
            String signature = signRequest(accessKeySecret, method, contentMD5, contentType, dateHeader, 
                                      canonicalizedHeaders, canonicalizedResource);
            
            // 添加授权头
            authHeaders.put(AUTHORIZATION_HEADER, "LOG " + accessKeyId + ":" + signature);
            
            // 发送请求
            Map<String, Object> response = sendRequest(method, url, authHeaders, requestBody);
            
            // 检查是否有错误
            if (response.containsKey("error")) {
                String errorMsg = (String) response.get("error");
                log.error("SLS API请求失败: {}", errorMsg);
                return "{\"success\":false,\"error\":\"" + errorMsg + "\"}";
            }
            
            // 检查状态码
            int statusCode = (int) response.getOrDefault("statusCode", 0);
            if (statusCode >= 400) {
                String errorBody = (String) response.get("body");
                log.error("SLS API返回错误状态码: {}, 响应: {}", statusCode, errorBody);
                return "{\"success\":false,\"statusCode\":" + statusCode + ",\"error\":\"" + 
                        (errorBody != null ? errorBody.replace("\"", "\\\"") : "未知错误") + "\"}";
            }
            
            // 返回响应体
            return (String) response.get("body");
        } catch (Exception e) {
            log.error("发送SLS请求失败: {}", e.getMessage(), e);
            StringBuilder errorJson = new StringBuilder("{\"success\":false,\"error\":\"");
            errorJson.append(e.getMessage().replace("\"", "\\\""));
            errorJson.append("\",\"errorType\":\"").append(e.getClass().getName()).append("\"");
            
            // 添加堆栈跟踪信息
            StringBuilder stackTrace = new StringBuilder();
            for (StackTraceElement element : e.getStackTrace()) {
                stackTrace.append(element.toString()).append("\\n");
                if (stackTrace.length() > 500) {
                    stackTrace.append("...(truncated)");
                    break;
                }
            }
            errorJson.append(",\"stackTrace\":\"").append(stackTrace).append("\"");
            
            // 添加额外信息
            errorJson.append(",\"requestMethod\":\"").append(method).append("\"");
            errorJson.append(",\"requestUrl\":\"").append(url.replace("\"", "\\\"")).append("\"");
            errorJson.append("}");
            
            return errorJson.toString();
        }
    }

    /**
     * 内部发送HTTP请求方法
     */
    public static Map<String, Object> sendRequest(String method, String url, Map<String, String> headers, String requestBody) {
        HttpURLConnection conn = null;
        Map<String, Object> result = new HashMap<>();
        
        try {
            URL requestUrl = new URL(url);
            conn = (HttpURLConnection) requestUrl.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(15000);
            
            // 设置通用请求头
            conn.setRequestProperty("Accept", "application/json");
            
            // 添加自定义请求头
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    conn.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            
            // 添加请求体（如果有）
            if (requestBody != null && !requestBody.isEmpty() && 
                (method.equals("POST") || method.equals("PUT"))) {
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }
            
            // 获取响应码
            int responseCode = conn.getResponseCode();
            result.put("statusCode", responseCode);
            
            // 读取响应
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            } catch (IOException e) {
                log.error("读取响应失败: {}", e.getMessage());
                result.put("error", "读取响应失败: " + e.getMessage());
                return result;
            }
            
            // 存储响应体
            result.put("body", response.toString());
            
            // 添加响应头
            Map<String, List<String>> responseHeaders = conn.getHeaderFields();
            Map<String, String> headers_map = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
                if (entry.getKey() != null) {
                    headers_map.put(entry.getKey(), String.join(", ", entry.getValue()));
                }
            }
            result.put("headers", headers_map);
            
            return result;
        } catch (Exception e) {
            log.error("HTTP请求失败: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
            return result;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 计算SLS API的签名
     */
    public static String signRequest(String accessKeySecret, String method, String contentMD5,
                                    String contentType, String date, String canonicalizedHeaders,
                                    String canonicalizedResource) {
        try {
            // 构建待签名字符串
            StringBuilder stringToSign = new StringBuilder();
            stringToSign.append(method).append("\n");
            stringToSign.append(contentMD5).append("\n");
            // 修改Content-Type，只使用application/json，不带charset
            stringToSign.append("application/json").append("\n");
            stringToSign.append(date).append("\n");
            stringToSign.append(canonicalizedHeaders);
            stringToSign.append(canonicalizedResource);
            
            log.debug("待签名字符串构建: \n方法: {}\nContent-MD5: {}\nContent-Type: {}\n日期: {}\n规范化头部: {}\n规范化资源: {}", 
                    method, contentMD5, "application/json", date, canonicalizedHeaders, canonicalizedResource);
            log.debug("完整待签名字符串: \n{}", stringToSign.toString());
            
            // 计算签名
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(new SecretKeySpec(accessKeySecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA1_ALGORITHM));
            byte[] signData = mac.doFinal(stringToSign.toString().getBytes(StandardCharsets.UTF_8));
            
            // Base64编码
            String signature = Base64.getEncoder().encodeToString(signData);
            log.debug("计算得到的签名: {}", signature);
            return signature;
        } catch (Exception e) {
            log.error("计算签名失败: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 生成SLS API请求的标准头部
     *
     * @param accessKeyId     访问密钥ID
     * @param accessKeySecret 访问密钥
     * @param method          HTTP方法
     * @param contentType     内容类型 (可为空，默认application/json)
     * @param resource        要访问的SLS资源，例如 /logstores/your-logstore
     * @param body            请求体 (可为空)
     * @return 包含所有必要头部的Map
     */
    public static Map<String, String> buildHeaders(String accessKeyId, String accessKeySecret, 
                                                 String method, String contentType,
                                                 String resource, String body) {
        Map<String, String> headers = new HashMap<>();
        
        // 设置标准头部
        String date = RFC_822_DATE_FORMAT.format(new Date());
        headers.put("Date", date);
        headers.put("Host", "log.aliyuncs.com");
        headers.put("x-log-apiversion", "0.6.0");
        headers.put("x-log-signaturemethod", "hmac-sha1");
        
        // 设置内容类型
        headers.put("Content-Type", "application/json");
        
        // 如果有请求体，计算MD5
        String contentMD5 = "";
        if (body != null && !body.isEmpty()) {
            contentMD5 = calculateMD5(body);
            headers.put("Content-MD5", contentMD5);
        }
        
        // 计算签名
        String canonicalizedHeaders = buildCanonicalizedHeaders(headers);
        String signature = signRequest(accessKeySecret, method, contentMD5, "application/json", 
                                       date, canonicalizedHeaders, resource);
        
        // 添加授权头
        headers.put("Authorization", "LOG " + accessKeyId + ":" + signature);
        
        return headers;
    }
    
    /**
     * 构建标准化的SLS请求头字符串
     *
     * @param headers 请求头Map
     * @return 标准化的头部字符串
     */
    private static String buildCanonicalizedHeaders(Map<String, String> headers) {
        // 提取所有以x-log-或x-acs-开头的头部
        List<String> canonicalizedHeaders = new ArrayList<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String key = entry.getKey();
            if (key != null && 
                (key.toLowerCase().startsWith("x-log-") || key.toLowerCase().startsWith("x-acs-"))) {
                canonicalizedHeaders.add(key.toLowerCase() + ":" + entry.getValue());
                log.debug("规范化头部 - 添加: {} = {}", key.toLowerCase(), entry.getValue());
            }
        }
        
        // 如果没有SLS特定的头部，返回空字符串
        if (canonicalizedHeaders.isEmpty()) {
            log.debug("规范化头部 - 无SLS特定头部");
            return "";
        }
        
        // 按照字典序排序
        Collections.sort(canonicalizedHeaders);
        log.debug("规范化头部 - 排序后: {}", canonicalizedHeaders);
        
        // 构建规范化头部字符串
        StringBuilder builder = new StringBuilder();
        for (String header : canonicalizedHeaders) {
            builder.append(header).append("\n");
        }
        
        String result = builder.toString();
        log.debug("规范化头部 - 最终结果: \n{}", result);
        return result;
    }

    /**
     * 执行GET请求
     * @param path API路径
     * @param params 请求参数
     * @return 响应结果
     */
    public Map<String, Object> doGet(String path, Map<String, String> params, String endpoint, String project) {
        try {
            log.info("================ doGet =================");
            log.debug("[状态=请求开始] 描述=执行GET请求 path={}", path);
            if (params != null) {
                log.debug("[状态=参数准备] 描述=GET请求参数: {} path={}", params, path);
            }
            
            // 确保path格式正确
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            
            // 构建完整的API路径，包括项目
            String apiPath = path;
            String fullResource = apiPath;
            
            // 构建URL查询参数
            StringBuilder queryString = new StringBuilder();
            if (params != null && !params.isEmpty()) {
                queryString.append("?");
                List<String> sortedParams = new ArrayList<>();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    sortedParams.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + 
                                   URLEncoder.encode(entry.getValue(), "UTF-8"));
                }
                queryString.append(String.join("&", sortedParams));
                
                fullResource = apiPath + queryString.toString();
            }
            
            // 构建完整URL
            String url = "https://" + project + "." + endpoint + fullResource;
            log.debug("[状态=URL构建] 描述=GET请求最终URL: {} path={}", url, path);
            
            // 构建请求头 - 使用新添加的方法
            Map<String, String> headers = buildHeaders("GET", path, params, null, endpoint, project);
            log.debug("[状态=请求头构建] 描述=GET请求最终头部: {} path={}", headers, path);
            
            // 发送请求
            Map<String, Object> response = sendRequest("GET", url, headers, null);
            log.info("================ doGet 结束 =================");
            return response;
        } catch (Exception e) {
            log.error("[状态=请求失败] 描述=执行GET请求失败 path={}", path, e);
            log.info("================ doGet 结束 =================");
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("errorCode", "InternalError");
            result.put("errorMessage", e.getMessage());
            return result;
        }
    }

    /**
     * 执行POST请求
     * @param path API路径
     * @param params 查询参数
     * @param body 请求体
     * @return 响应结果
     */
    public Map<String, Object> doPost(String path, Map<String, String> params, String body, String endpoint, String project) {
        try {
            log.info("================ doPost =================");
            log.debug("[状态=请求开始] 描述=执行POST请求 path={}", path);
            if (params != null) {
                log.debug("[状态=参数准备] 描述=POST请求参数: {} path={}", params, path);
            }
            log.debug("[状态=请求体准备] 描述=POST请求体: {} path={}", body, path);
            
            // 确保path格式正确
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            
            // 日志查询特殊处理
            Map<String, String> queryParams = new HashMap<>();
            if (params != null) {
                queryParams.putAll(params);
            }
            
            // 如果是日志查询接口且有请求体，特殊处理签名计算
            String specialBody = body;
            if (path.contains("/logstores/") && path.endsWith("/logs") && body != null) {
                try {
                    // 尝试解析JSON
                    Map<String, Object> bodyMap = gson.fromJson(body, Map.class);
                    
                    // 将部分参数提取到URL参数中 - 修复数值格式问题
                    // 确保from参数存在且正确
                    if (bodyMap.containsKey("from")) {
                        Object fromValue = bodyMap.get("from");
                        // 确保整数格式而不是科学计数法
                        if (fromValue instanceof Number) {
                            long longValue = ((Number)fromValue).longValue();
                            // 明确使用long值作为字符串，避免任何格式变化
                            queryParams.put("from", Long.toString(longValue));
                            log.debug("[状态=参数准备] 描述=提取from参数: {} path={}", Long.toString(longValue), path);
                        } else {
                            queryParams.put("from", String.valueOf(fromValue));
                            log.debug("[状态=参数准备] 描述=提取from参数(非数字): {} path={}", fromValue, path);
                        }
                    } else {
                        // 如果没有from参数，使用默认值（24小时前）
                        long defaultFrom = System.currentTimeMillis() / 1000 - 86400;
                        queryParams.put("from", Long.toString(defaultFrom));
                        log.debug("[状态=参数准备] 描述=添加默认from参数: {} path={}", defaultFrom, path);
                    }
                    
                    if (bodyMap.containsKey("to")) {
                        Object toValue = bodyMap.get("to");
                        if (toValue instanceof Number) {
                            long longValue = ((Number)toValue).longValue();
                            queryParams.put("to", Long.toString(longValue));
                            log.debug("[状态=参数准备] 描述=提取to参数: {} path={}", Long.toString(longValue), path);
                        } else {
                            queryParams.put("to", String.valueOf(toValue));
                        }
                    } else {
                        // 如果没有to参数，使用当前时间
                        long defaultTo = System.currentTimeMillis() / 1000;
                        queryParams.put("to", Long.toString(defaultTo));
                        log.debug("[状态=参数准备] 描述=添加默认to参数: {} path={}", defaultTo, path);
                    }
                    
                    if (bodyMap.containsKey("line")) {
                        Object lineValue = bodyMap.get("line");
                        if (lineValue instanceof Number) {
                            int intValue = ((Number)lineValue).intValue();
                            queryParams.put("line", Integer.toString(intValue));
                        } else {
                            queryParams.put("line", String.valueOf(lineValue));
                        }
                    }
                    
                    if (bodyMap.containsKey("offset")) {
                        Object offsetValue = bodyMap.get("offset");
                        if (offsetValue instanceof Number) {
                            int intValue = ((Number)offsetValue).intValue();
                            queryParams.put("offset", Integer.toString(intValue));
                        } else {
                            queryParams.put("offset", String.valueOf(offsetValue));
                        }
                    }
                    
                    // 布尔值和其他参数
                    if (bodyMap.containsKey("reverse")) {
                        queryParams.put("reverse", String.valueOf(bodyMap.get("reverse")));
                    }
                    
                    if (bodyMap.containsKey("powerSql")) {
                        queryParams.put("powerSql", String.valueOf(bodyMap.get("powerSql")));
                    }
                    
                    // 保留query参数在请求体中
                    Map<String, Object> newBodyMap = new HashMap<>();
                    if (bodyMap.containsKey("query")) {
                        newBodyMap.put("query", bodyMap.get("query"));
                    } else {
                        // 添加默认查询
                        newBodyMap.put("query", "*");
                    }
                    
                    // 重新序列化
                    specialBody = gson.toJson(newBodyMap);
                    log.debug("[状态=请求体准备] 描述=日志查询特殊处理-新请求体: {} path={}", specialBody, path);
                    log.debug("[状态=参数准备] 描述=日志查询特殊处理-URL参数: {} path={}", queryParams, path);
                    
                    // 额外检查必需参数
                    log.debug("[状态=参数准备] 描述=日志查询参数检查-from参数是否存在: {}, 值: {} path={}", 
                            queryParams.containsKey("from"), queryParams.get("from"), path);
                } catch (Exception e) {
                    log.warn("[状态=警告] 描述=解析日志查询请求体失败，使用原始请求体: {}", e.getMessage(), e);
                }
            }
            
            // 构建完整URL
            String url = buildUrl(path, queryParams, endpoint, project);
            log.debug("[状态=URL构建] 描述=POST请求最终URL: {} path={}", url, path);
            
            // 构建请求头 - 使用新添加的方法
            Map<String, String> headers = buildHeaders("POST", path, queryParams, specialBody, endpoint, project);
            log.debug("[状态=请求头构建] 描述=POST请求头: {} path={}", headers, path);
            
            // 发送请求
            HttpURLConnection conn = null;
            InputStream inputStream = null;
            Map<String, Object> result = new HashMap<>();
            
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                
                // 设置通用请求头
                conn.setRequestProperty("Accept", "application/json");
                
                // 添加自定义请求头
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
                
                // 写入请求体
                if (specialBody != null && !specialBody.isEmpty()) {
                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = specialBody.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }
                }
                
                // 获取响应
                int statusCode = conn.getResponseCode();
                result.put("statusCode", statusCode);
                
                Map<String, List<String>> responseHeaders = conn.getHeaderFields();
                result.put("headers", responseHeaders);
                
                // 读取响应内容
                StringBuilder responseBody = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        responseBody.append(line);
                    }
                }
                
                result.put("body", responseBody.toString());
                
                return result;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        } catch (Exception e) {
            log.error("[状态=请求失败] 描述=执行POST请求失败 path={}", path, e);
            log.info("================ doPost 结束 =================");
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("statusCode", 500);
            return errorResult;
        }
    }

    /**
     * 执行POST请求，支持自定义headers
     */
    public Map<String, Object> doPost(String path, Map<String, String> params, String body, Map<String, String> customHeaders) {
        try {
            log.info("================ doPost 开始 =================");
            log.debug("[状态=请求开始] 描述=执行POST请求(自定义headers) path={}", path);
            if (params != null) {
                log.debug("[状态=参数准备] 描述=POST请求参数: {} path={}", params, path);
            }
            log.debug("[状态=请求体准备] 描述=POST请求体: {} path={}", body, path);

            // 确保path格式正确
            if (!path.startsWith("/")) {
                path = "/" + path;
            }

            Map<String, String> queryParams = new HashMap<>();
            if (params != null) {
                queryParams.putAll(params);
            }

            String specialBody = body;
            if (path.contains("/logstores/") && path.endsWith("/logs") && body != null) {
                try {
                    Map<String, Object> bodyMap = gson.fromJson(body, Map.class);
                    if (bodyMap.containsKey("from")) {
                        Object fromValue = bodyMap.get("from");
                        if (fromValue instanceof Number) {
                            long longValue = ((Number)fromValue).longValue();
                            queryParams.put("from", Long.toString(longValue));
                        } else {
                            queryParams.put("from", String.valueOf(fromValue));
                        }
                    } else {
                        long defaultFrom = System.currentTimeMillis() / 1000 - 86400;
                        queryParams.put("from", Long.toString(defaultFrom));
                    }
                    if (bodyMap.containsKey("to")) {
                        Object toValue = bodyMap.get("to");
                        if (toValue instanceof Number) {
                            long longValue = ((Number)toValue).longValue();
                            queryParams.put("to", Long.toString(longValue));
                        } else {
                            queryParams.put("to", String.valueOf(toValue));
                        }
                    } else {
                        long defaultTo = System.currentTimeMillis() / 1000;
                        queryParams.put("to", Long.toString(defaultTo));
                    }
                    if (bodyMap.containsKey("line")) {
                        Object lineValue = bodyMap.get("line");
                        if (lineValue instanceof Number) {
                            int intValue = ((Number)lineValue).intValue();
                            queryParams.put("line", Integer.toString(intValue));
                        } else {
                            queryParams.put("line", String.valueOf(lineValue));
                        }
                    }
                    if (bodyMap.containsKey("offset")) {
                        Object offsetValue = bodyMap.get("offset");
                        if (offsetValue instanceof Number) {
                            int intValue = ((Number)offsetValue).intValue();
                            queryParams.put("offset", Integer.toString(intValue));
                        } else {
                            queryParams.put("offset", String.valueOf(offsetValue));
                        }
                    }
                    if (bodyMap.containsKey("reverse")) {
                        queryParams.put("reverse", String.valueOf(bodyMap.get("reverse")));
                    }
                    if (bodyMap.containsKey("powerSql")) {
                        queryParams.put("powerSql", String.valueOf(bodyMap.get("powerSql")));
                    }
                    Map<String, Object> newBodyMap = new HashMap<>();
                    if (bodyMap.containsKey("query")) {
                        newBodyMap.put("query", bodyMap.get("query"));
                    } else {
                        newBodyMap.put("query", "*");
                    }
                    specialBody = gson.toJson(newBodyMap);
                } catch (Exception e) {
                    log.warn("[状态=警告] 描述=解析日志查询请求体失败，使用原始请求体: {}", e.getMessage(), e);
                }
            }

            String url = buildUrl(path, queryParams, "cn-beijing.log.aliyuncs.com", "ads-sls");
            log.debug("[状态=URL构建] 描述=POST请求最终URL: {} path={}", url, path);

            Map<String, String> headers = buildHeaders("POST", path, queryParams, specialBody, "cn-beijing.log.aliyuncs.com", "ads-sls");
            if (customHeaders != null) {
                headers.putAll(customHeaders);
            }
            log.debug("[状态=请求头构建] 描述=POST请求头: {} path={}", headers, path);

            HttpURLConnection conn = null;
            Map<String, Object> result = new HashMap<>();
            try {
                conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setRequestProperty("Accept", "application/json");
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
                if (specialBody != null && !specialBody.isEmpty()) {
                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = specialBody.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }
                }
                int statusCode = conn.getResponseCode();
                result.put("statusCode", statusCode);
                Map<String, List<String>> responseHeaders = conn.getHeaderFields();
                result.put("headers", responseHeaders);
                StringBuilder responseBody = new StringBuilder();
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        responseBody.append(line);
                    }
                }
                result.put("body", responseBody.toString());
                return result;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        } catch (Exception e) {
            log.error("[状态=请求失败] 描述=执行POST请求(自定义headers)失败 path={}", path, e);
            log.info("================ doPost 结束 =================");
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("statusCode", 500);
            return errorResult;
        }
    }

    /**
     * 构建完整URL
     */
    private String buildUrl(String path, Map<String, String> params, String endpoint, String project) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://").append(project).append(".").append(endpoint);
        
        // 确保path以/开头
        if (!path.startsWith("/")) {
            urlBuilder.append("/");
        }
        
        // 添加路径
        urlBuilder.append(path);
        
        // 添加查询参数 - 日志查询API需要特殊处理
        if (params != null && !params.isEmpty()) {
            urlBuilder.append("?");
            
            // 如果是日志查询API，确保关键参数优先且按照顺序添加
            if (path.contains("/logstores/") && path.endsWith("/logs")) {
                List<String> paramList = new ArrayList<>();
                String[] keyParams = {"from", "to", "topic", "query", "line", "offset", "reverse"};
                
                // 首先添加关键参数，按照顺序
                for (String key : keyParams) {
                    if (params.containsKey(key)) {
                        try {
                            paramList.add(key + "=" + URLEncoder.encode(params.get(key), "UTF-8"));
                            log.debug("[状态=参数] 描述=添加日志查询关键参数: {} = {}", key, params.get(key));
                        } catch (Exception e) {
                            log.error("[状态=错误] 描述=编码参数错误: {}", e.getMessage(), e);
                            paramList.add(key + "=" + params.get(key));
                        }
                    }
                }
                
                // 然后添加其他参数
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (!Arrays.asList(keyParams).contains(entry.getKey())) {
                        try {
                            paramList.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + 
                                       URLEncoder.encode(entry.getValue(), "UTF-8"));
                            log.debug("[状态=参数] 描述=添加日志查询其他参数: {} = {}", entry.getKey(), entry.getValue());
                        } catch (Exception e) {
                            log.error("[状态=错误] 描述=编码参数错误: {}", e.getMessage(), e);
                            paramList.add(entry.getKey() + "=" + entry.getValue());
                        }
                    }
                }
                
                urlBuilder.append(String.join("&", paramList));
            } else {
                // 普通API使用原来的处理方式
                List<String> paramList = new ArrayList<>();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    try {
                        paramList.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + 
                                   URLEncoder.encode(entry.getValue(), "UTF-8"));
                    } catch (Exception e) {
                        log.error("[状态=错误] 描述=Error encoding URL parameter");
                        paramList.add(entry.getKey() + "=" + entry.getValue());
                    }
                }
                urlBuilder.append(String.join("&", paramList));
            }
        }
        
        String fullUrl = urlBuilder.toString();
        log.debug("[状态=URL] 描述=构建的SLS URL: {}", fullUrl);
        return fullUrl;
    }

    /**
     * 判断参数是否需要包含在签名中
     */
    private static boolean shouldSignParameter(String paramName) {
        // 以下参数需要包含在签名计算中
        List<String> signParams = Arrays.asList(
            "acl", "delete", "group", "groupchecking", "compose", "composecheck", 
            "rebuild", "buildsearch", "indexcfg", "tags", "topic", "saveas", 
            "status", "logging", "histogram", "x-log-deleteobject", "x-log-compresstype",
            // 添加标准查询参数
            "from", "to", "topic", "query", "line", "offset", "reverse", "powerSql",
            "shard", "type", "notimeout",
            // 添加上下文查询相关参数
            "pack_id", "pack_meta", "back_lines", "forward_lines",
            // 添加日志查询相关参数
            "logstore", "project", "configName", "start", "end", "progress",
            "count", "cursor", "source", "request", "cw", "time", "key", "size",
            "wd", "rewrite", "token", "sort", "order", "distinct", "where", "filter"
        );
        boolean shouldSign = signParams.contains(paramName);
        log.debug("[状态=参数] 描述=参数: {}, 是否签名: {}", paramName, shouldSign);
        return shouldSign;
    }

    /**
     * 计算字符串的MD5值
     */
    private static String calculateMD5(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(content.getBytes(StandardCharsets.UTF_8));
            
            // 将字节数组转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[状态=错误] 描述=计算MD5失败: {}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 构建HTTP请求头 - 重载方法，支持Map参数
     */
    private Map<String, String> buildHeaders(String method, String path, Map<String, String> params, String body, String endpoint, String project) {
        try {
            Map<String, String> headers = new HashMap<>();
            
            // 构建规范化资源
            String canonicalizedResource = path;
            
            if (params != null && !params.isEmpty()) {
                List<String> signedParams = new ArrayList<>();
                for (Map.Entry<String, String> param : params.entrySet()) {
                    if (shouldSignParameter(param.getKey())) {
                        signedParams.add(param.getKey() + "=" + param.getValue());
                    }
                }
                
                if (!signedParams.isEmpty()) {
                    Collections.sort(signedParams);
                    canonicalizedResource += "?" + String.join("&", signedParams);
                }
            }
            
            // 设置内容类型 - 修改为纯application/json
            headers.put("Content-Type", "application/json");
            
            // 设置SLS特定的头
            headers.put("x-log-apiversion", "0.6.0");
            headers.put("x-log-signaturemethod", "hmac-sha1");
            
            // 修复Host头，去除可能存在的空格
            String cleanProject = project != null ? project.trim() : "ads-sls";
            String cleanEndpoint = endpoint != null ? endpoint.trim() : "cn-beijing.log.aliyuncs.com";
            headers.put("Host", cleanProject + "." + cleanEndpoint);
            
            // 日期头 - 格式必须严格遵循RFC 822标准
            String dateHeader = RFC_822_DATE_FORMAT.format(new Date());
            headers.put("Date", dateHeader);
            
            // 如果有请求体，添加Content-MD5
            String contentMD5 = "";
            if (body != null && !body.isEmpty()) {
                contentMD5 = calculateMD5(body);
                headers.put("Content-MD5", contentMD5);
                
                // 添加请求体大小
                headers.put("x-log-bodyrawsize", String.valueOf(body.getBytes(StandardCharsets.UTF_8).length));
            }
            
            // 规范化自定义头部
            String canonicalizedHeaders = buildCanonicalizedHeaders(headers);
            
            log.debug("[状态=方法] 描述=doGet 路径: {}, 规范化资源: {}", method, path);
            log.debug("[状态=头部] 描述=规范化头部: {}", canonicalizedHeaders);
            
            // 计算签名
            String signature = signRequest(accessKeySecret, method, contentMD5, "application/json", 
                               dateHeader, canonicalizedHeaders, canonicalizedResource);
            
            headers.put("Authorization", "LOG " + accessKeyId + ":" + signature);
            
            return headers;
        } catch (Exception e) {
            log.error("[状态=错误] 描述=构建请求头失败: {}", e.getMessage(), e);
            // 返回基本请求头
            Map<String, String> basicHeaders = new HashMap<>();
            basicHeaders.put("Content-Type", "application/json");
            return basicHeaders;
        }
    }
} 