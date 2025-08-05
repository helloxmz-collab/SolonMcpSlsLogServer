package com.anker.sls.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 阿里云SLS API HTTP请求工具类
 */
@Component
public class HttpUtil {

    private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);

    @Value("${aliyun.sls.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.sls.access-key-secret}")
    private String accessKeySecret;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HttpUtil(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行GET请求
     *
     * @param path API路径
     * @param params 查询参数
     * @return 响应结果
     */
    public Map<String, Object> doGet(String path, Map<String, String> params, String endpoint, String project) {
        
        try {
            log.info("================ doGet =================");
            String url = buildUrl(path, params, endpoint, project);
            HttpHeaders headers = buildHeaders("GET", path, params, null, endpoint, project);
            log.debug("[状态=请求开始] URL={} 请求头={}", url, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            
            log.debug("[状态=收到响应] 状态码={} 响应体={}", response.getStatusCode(), response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("[状态=成功]");
                log.info("================ doGet 结束 =================");
                return objectMapper.readValue(response.getBody(), Map.class);
            } else {
                log.error("[状态=失败] 响应体={}", response.getBody());
                log.info("================ doGet 结束 =================");
                return Collections.singletonMap("error", response.getBody());
            }
        } catch (Exception e) {
            log.error("[状态=异常] 异常信息={}", e.getMessage(), e);
            log.info("================ doGet 结束 =================");
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * 执行POST请求
     *
     * @param path API路径
     * @param params 查询参数
     * @param body 请求体
     * @return 响应结果
     */
    public Map<String, Object> doPost(String path, Map<String, String> params, Object body, String endpoint, String project) {
        
        try {
            log.info("================ doPost 开始 =================");
            String url = buildUrl(path, params, endpoint, project);
            String bodyString = body != null ? objectMapper.writeValueAsString(body) : "";
            HttpHeaders headers = buildHeaders("POST", path, params, bodyString, endpoint, project);
            log.debug("[状态=请求开始] URL={} 请求头={} 请求体={}", url, headers, bodyString);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(bodyString, headers),
                    String.class
            );
            
            log.debug("[状态=收到响应] 状态码={} 响应体={}", response.getStatusCode(), response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("[状态=成功]");
                log.info("================ doPost 结束 =================");
                return objectMapper.readValue(response.getBody(), Map.class);
            } else {
                log.error("[状态=失败] 响应体={}", response.getBody());
                log.info("================ doPost 结束 =================");
                return Collections.singletonMap("error", response.getBody());
            }
        } catch (Exception e) {
            log.error("[状态=异常] 异常信息={}", e.getMessage(), e);
            log.info("================ doPost 结束 =================");
            return Collections.singletonMap("error", e.getMessage());
        }
    }

    /**
     * 构建请求URL
     */
    private String buildUrl(String path, Map<String, String> params, String endpoint, String project) {
        String baseUrl = String.format("https://%s.%s%s", project, endpoint, path);
        
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl);
        params.forEach(builder::queryParam);
        return builder.build().toUriString();
    }

    /**
     * 构建请求头，包含阿里云SLS API所需的签名信息
     */
    private HttpHeaders buildHeaders(String method, String resource, Map<String, String> params, String body, String endpoint, String project) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        
        // 添加阿里云SLS所需的请求头
        String date = getGMTDate();
        headers.set("Date", date);
        headers.set("x-log-apiversion", "0.6.0");
        headers.set("x-log-signaturemethod", "hmac-sha1");
        
        // 计算内容MD5（如果有请求体）
        String contentMD5 = "";
        if (body != null && !body.isEmpty()) {
            contentMD5 = getContentMD5(body);
            headers.set("Content-MD5", contentMD5);
        }
        
        // 计算签名
        String signature = calculateSignature(method, contentMD5, "application/json", date, resource, params);
        headers.set("Authorization", "LOG " + accessKeyId + ":" + signature);
        headers.set("Host", String.format("%s.%s", project, endpoint));
        
        return headers;
    }

    /**
     * 获取GMT格式的日期字符串
     */
    private String getGMTDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date());
    }

    /**
     * 计算内容MD5
     */
    private String getContentMD5(String content) {
        byte[] bytes = DigestUtils.md5Digest(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 计算阿里云SLS API所需的签名
     */
    private String calculateSignature(String httpMethod, String contentMD5, String contentType, 
                                      String date, String resource, Map<String, String> params) {
        try {
            // 构建签名字符串
            StringBuilder sb = new StringBuilder();
            sb.append(httpMethod).append("\n");
            sb.append(contentMD5).append("\n");
            sb.append(contentType).append("\n");
            sb.append(date).append("\n");
            
            // 添加阿里云SLS标准头
            List<String> slsHeaders = new ArrayList<>();
            // 如果有SLS特殊头信息，添加到这里
            
            if (!slsHeaders.isEmpty()) {
                Collections.sort(slsHeaders);
                for (String header : slsHeaders) {
                    sb.append(header).append("\n");
                }
            }
            
            // 添加资源路径和参数
            sb.append(resource);
            
            if (params != null && !params.isEmpty()) {
                List<String> paramList = new ArrayList<>();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    // 只有特定的参数才需要参与签名
                    if (shouldIncludeInSignature(entry.getKey())) {
                        paramList.add(entry.getKey() + "=" + entry.getValue());
                    }
                }
                
                if (!paramList.isEmpty()) {
                    Collections.sort(paramList);
                    sb.append("?");
                    sb.append(String.join("&", paramList));
                }
            }
            
            String stringToSign = sb.toString();
            log.debug("[HttpUtil] 方法=calculateSignature 状态=待签名字符串 内容={}", stringToSign);
            
            // 使用HMAC-SHA1计算签名
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(accessKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signData);
            
        } catch (Exception e) {
            log.error("[HttpUtil] 方法=calculateSignature 状态=异常 异常信息={}", e.getMessage(), e);
            return "";
        }
    }

    /**
     * 判断参数是否需要包含在签名中
     */
    private boolean shouldIncludeInSignature(String paramName) {
        // 根据阿里云SLS API文档，确定哪些参数需要包含在签名中
        List<String> signParams = Arrays.asList(
            "projectName", "logstoreName", "shardId", "from", "to", 
            "topic", "query", "line", "offset", "reverse"
        );
        return signParams.contains(paramName);
    }
} 