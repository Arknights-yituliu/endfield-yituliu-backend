package org.yituliu.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.yituliu.entity.dto.GachaResponseDTO;
import org.yituliu.service.EndfieldApiService;
import org.yituliu.util.HttpClientUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Endfield API请求示例类
 * 演示如何使用HttpClientUtil发送HTTP GET请求
 * @author 山桜
 */
@Slf4j
@Component
public class EndfieldApiExample {
    
    private final HttpClientUtil httpClientUtil;
    private final EndfieldApiService endfieldApiService;
    
    /**
     * 构造函数
     * @param httpClientUtil HTTP客户端工具类
     * @param endfieldApiService Endfield API服务类
     */
    public EndfieldApiExample(HttpClientUtil httpClientUtil, EndfieldApiService endfieldApiService) {
        this.httpClientUtil = httpClientUtil;
        this.endfieldApiService = endfieldApiService;
    }
    
    /**
     * 示例1：使用HttpClientUtil发送基本GET请求
     */
    public void exampleBasicGetRequest() {
        String url = "";
        
        try {
            log.info("开始发送基本GET请求: {}", url);
            
            // 使用HttpClientUtil发送GET请求
            GachaResponseDTO response = httpClientUtil.get(url, GachaResponseDTO.class);
            
            if (response != null && response.isSuccess()) {
                log.info("请求成功，响应码: {}, 记录数量: {}", 
                        response.getCode(),
                        response.getData() != null && response.getData().getList() != null ? 
                        response.getData().getList().size() : 0);
            } else {
                log.warn("请求失败，响应码: {}, 消息: {}", 
                        response != null ? response.getCode() : "null", 
                        response != null ? response.getMsg() : "null");
            }
            
        } catch (Exception e) {
            log.error("请求异常", e);
        }
    }
    
    /**
     * 示例2：使用HttpClientUtil发送带详细请求头的GET请求
     */
    public void exampleDetailedHeadersGetRequest() {
        String url = "";
        
        try {
            log.info("开始发送带详细请求头的GET请求: {}", url);
            
            // 构建详细请求头映射
            Map<String, String> headers = new HashMap<>();
            headers.put("accept", "application/json, text/plain, */*");
            headers.put("accept-encoding", "gzip, deflate, br, zstd");
            headers.put("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
            headers.put("if-none-match", "W/\"3db-dAnLSmaymYp4yw2FTM2q/Oca/kc\"");
            headers.put("priority", "u=1, i");
            headers.put("referer", "");
            headers.put("sec-ch-ua", "\"Microsoft Edge\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"");
            headers.put("sec-ch-ua-mobile", "?0");
            headers.put("sec-ch-ua-platform", "\"Windows\"");
            headers.put("sec-fetch-dest", "empty");
            headers.put("sec-fetch-mode", "cors");
            headers.put("sec-fetch-site", "same-origin");
            
            // 使用HttpClientUtil发送带详细请求头的GET请求
            GachaResponseDTO response = httpClientUtil.getWithDetailedHeaders(url, headers, GachaResponseDTO.class);
            
            if (response != null && response.isSuccess()) {
                log.info("请求成功，响应码: {}, 记录数量: {}", 
                        response.getCode(),
                        response.getData() != null && response.getData().getList() != null ? 
                        response.getData().getList().size() : 0);
            } else {
                log.warn("请求失败，响应码: {}, 消息: {}", 
                        response != null ? response.getCode() : "null", 
                        response != null ? response.getMsg() : "null");
            }
            
        } catch (Exception e) {
            log.error("请求异常", e);
        }
    }
    
    /**
     * 示例3：使用HttpClientUtil发送带Spring HttpHeaders的GET请求
     */
    public void exampleSpringHeadersGetRequest() {
        String url = "";
        
        try {
            log.info("开始发送带Spring HttpHeaders的GET请求: {}", url);
            
            // 构建Spring HttpHeaders
            HttpHeaders headers = new HttpHeaders();
            headers.set("accept", "application/json, text/plain, */*");
            headers.set("accept-encoding", "gzip, deflate, br, zstd");
            headers.set("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
            headers.set("if-none-match", "W/\"3db-dAnLSmaymYp4yw2FTM2q/Oca/kc\"");
            headers.set("priority", "u=1, i");
            headers.set("referer", "");
            headers.set("sec-ch-ua", "\"Microsoft Edge\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"");
            headers.set("sec-ch-ua-mobile", "?0");
            headers.set("sec-ch-ua-platform", "\"Windows\"");
            headers.set("sec-fetch-dest", "empty");
            headers.set("sec-fetch-mode", "cors");
            headers.set("sec-fetch-site", "same-origin");
            
            // 使用HttpClientUtil发送带Spring HttpHeaders的GET请求
            GachaResponseDTO response = httpClientUtil.get(url, null, headers, GachaResponseDTO.class);
            
            if (response != null && response.isSuccess()) {
                log.info("请求成功，响应码: {}, 记录数量: {}", 
                        response.getCode(),
                        response.getData() != null && response.getData().getList() != null ? 
                        response.getData().getList().size() : 0);
            } else {
                log.warn("请求失败，响应码: {}, 消息: {}", 
                        response != null ? response.getCode() : "null", 
                        response != null ? response.getMsg() : "null");
            }
            
        } catch (Exception e) {
            log.error("请求异常", e);
        }
    }
    

    /**
     * 运行所有示例
     */
    public void runAllExamples() {
        log.info("开始运行所有HTTP请求示例");
        
        exampleBasicGetRequest();
        exampleDetailedHeadersGetRequest();
        exampleSpringHeadersGetRequest();
        
        log.info("所有HTTP请求示例运行完成");
    }
}