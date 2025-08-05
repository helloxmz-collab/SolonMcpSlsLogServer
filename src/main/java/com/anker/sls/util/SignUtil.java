package com.anker.sls.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.DigestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class SignUtil {
    
    private static final Logger log = LoggerFactory.getLogger(SignUtil.class);

    /**
     * 计算SLS API HMAC-SHA1签名
     *
     * @param stringToSign 要签名的字符串
     * @param accessKeySecret 访问密钥
     * @return Base64编码的签名
     */
    public static String signString(String stringToSign, String accessKeySecret) {
        log.info("================ 签名计算开始 =================");
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(accessKeySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            log.info("================ 签名计算结束 =================");
            return Base64.getEncoder().encodeToString(signData);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("[SignUtil] 方法=sign 状态=异常 描述=计算签名失败", e);
            log.info("================ 签名计算结束 =================");
            throw new RuntimeException("计算HMAC-SHA1签名失败", e);
        }
    }

    /**
     * 计算SLS API请求的MD5哈希值
     *
     * @param content 内容
     * @return Base64编码的MD5哈希值
     */
    public static String md5(String content) {
        byte[] md5Bytes = DigestUtils.md5Digest(content.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(md5Bytes);
    }
} 