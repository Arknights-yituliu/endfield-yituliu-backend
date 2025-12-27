package org.yituliu.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * HTTP客户端工具类
 * 基于Spring RestTemplate实现，提供统一的API请求接口
 * @author 山桜
 */
@Slf4j
@Component
public class HttpClientUtil {
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    /**
     * 构造函数
     * @param restTemplate Spring RestTemplate实例
     * @param objectMapper Jackson ObjectMapper实例
     */
    public HttpClientUtil(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 发送GET请求
     * @param url 请求URL
     * @param responseType 响应类型
     * @param <T> 响应类型泛型
     * @return 响应对象
     */
    public <T> T get(String url, Class<T> responseType) {
        return get(url, null, null, responseType);
    }
    
    /**
     * 发送带参数的GET请求
     * @param url 请求URL
     * @param params 查询参数
     * @param headers 请求头
     * @param responseType 响应类型
     * @param <T> 响应类型泛型
     * @return 响应对象
     */
    public <T> T get(String url, Map<String, Object> params, HttpHeaders headers, Class<T> responseType) {
        try {
            // 构建URL
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            if (params != null) {
                params.forEach(builder::queryParam);
            }
            
            // 构建请求实体
            HttpEntity<?> entity = new HttpEntity<>(buildHeaders(headers));
            
            // 发送请求
            ResponseEntity<T> response = restTemplate.exchange(
                    builder.build().toUri(),
                    HttpMethod.GET,
                    entity,
                    responseType
            );
            
            log.debug("GET请求成功: {}, 响应状态: {}", url, response.getStatusCode());
            return response.getBody();
            
        } catch (RestClientException e) {
            log.error("GET请求失败: {}, 错误信息: {}", url, e.getMessage(), e);
            throw new HttpClientException("GET请求失败: " + url, e);
        }
    }
    
    /**
     * 发送带详细请求头的GET请求
     * @param url 请求URL
     * @param headers 详细请求头映射
     * @param responseType 响应类型
     * @param <T> 响应类型泛型
     * @return 响应对象
     */
    public <T> T getWithDetailedHeaders(String url, Map<String, String> headers, Class<T> responseType) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            
            // 设置默认请求头
            httpHeaders.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36 Edg/143.0.0.0");
            httpHeaders.set(HttpHeaders.ACCEPT, "application/json, text/plain, */*");
            
            // 添加自定义请求头
            if (headers != null) {
                headers.forEach(httpHeaders::set);
            }
            
            // 构建请求实体
            HttpEntity<?> entity = new HttpEntity<>(httpHeaders);
            
            // 发送请求
            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    responseType
            );
            
            log.debug("GET请求成功: {}, 响应状态: {}", url, response.getStatusCode());
            return response.getBody();
            
        } catch (RestClientException e) {
            log.error("GET请求失败: {}, 错误信息: {}", url, e.getMessage(), e);
            throw new HttpClientException("GET请求失败: " + url, e);
        }
    }
    
    /**
     * 发送POST请求
     * @param url 请求URL
     * @param requestBody 请求体
     * @param responseType 响应类型
     * @param <T> 响应类型泛型
     * @return 响应对象
     */
    public <T> T post(String url, Object requestBody, Class<T> responseType) {
        return post(url, requestBody, null, responseType);
    }
    
    /**
     * 发送带请求头的POST请求
     * @param url 请求URL
     * @param requestBody 请求体
     * @param headers 请求头
     * @param responseType 响应类型
     * @param <T> 响应类型泛型
     * @return 响应对象
     */
    public <T> T post(String url, Object requestBody, HttpHeaders headers, Class<T> responseType) {
        try {
            // 构建请求实体
            HttpEntity<Object> entity = new HttpEntity<>(requestBody, buildHeaders(headers));
            
            // 发送请求
            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    responseType
            );
            
            log.debug("POST请求成功: {}, 响应状态: {}", url, response.getStatusCode());
            return response.getBody();
            
        } catch (RestClientException e) {
            log.error("POST请求失败: {}, 错误信息: {}", url, e.getMessage(), e);
            throw new HttpClientException("POST请求失败: " + url, e);
        }
    }
    
    /**
     * 发送JSON格式的POST请求
     * @param url 请求URL
     * @param requestBody 请求体对象
     * @param responseType 响应类型
     * @param <T> 响应类型泛型
     * @return 响应对象
     */
    public <T> T postJson(String url, Object requestBody, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            return post(url, jsonBody, headers, responseType);
        } catch (Exception e) {
            log.error("JSON序列化失败: {}, 错误信息: {}", url, e.getMessage(), e);
            throw new HttpClientException("JSON序列化失败", e);
        }
    }
    
    /**
     * 发送GET请求并返回字符串
     * @param url 请求URL
     * @return 响应字符串
     */
    public String getAsString(String url) {
        return get(url, null, null, String.class);
    }
    
    /**
     * 发送GET请求并解析为指定类型
     * @param url 请求URL
     * @param typeReference Jackson类型引用
     * @param <T> 响应类型泛型
     * @return 响应对象
     */
    public <T> T getAsObject(String url, TypeReference<T> typeReference) {
        String response = getAsString(url);
        try {
            return objectMapper.readValue(response, typeReference);
        } catch (Exception e) {
            log.error("JSON反序列化失败: {}, 错误信息: {}", url, e.getMessage(), e);
            throw new HttpClientException("JSON反序列化失败", e);
        }
    }
    
    /**
     * 构建请求头
     * @param customHeaders 自定义请求头
     * @return 完整的请求头
     */
    private HttpHeaders buildHeaders(HttpHeaders customHeaders) {
        HttpHeaders headers = new HttpHeaders();
        
        // 设置默认请求头
        headers.set(HttpHeaders.USER_AGENT, "Endfield-Yituliu-Backend/1.0");
        headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        
        // 添加自定义请求头
        if (customHeaders != null) {
            headers.putAll(customHeaders);
        }
        
        return headers;
    }
    
    /**
     * 检查URL是否可达
     * @param url 要检查的URL
     * @return 是否可达
     */
    public boolean isUrlReachable(String url) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (RestClientException e) {
            log.debug("URL不可达: {}, 错误信息: {}", url, e.getMessage());
            return false;
        }
    }
    
    /**
     * HTTP客户端异常类
     */
    public static class HttpClientException extends RuntimeException {
        public HttpClientException(String message) {
            super(message);
        }
        
        public HttpClientException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}