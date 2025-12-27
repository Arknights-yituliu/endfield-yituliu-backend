package org.yituliu.service;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.yituliu.entity.dto.GachaResponseDTO;
import org.yituliu.util.HttpClientUtil;
import org.yituliu.util.UrlParser;

import java.util.Map;

/**
 * Endfield API服务类
 * 提供Endfield游戏API的调用接口
 * @author 山桜
 */
@Slf4j
@Service
public class EndfieldApiService {
    
    private final HttpClientUtil httpClientUtil;
    
    /**
     * Endfield API基础URL
     */
    @Value("${endfield.api.base-url:https://endfield.hypergryph.com}")
    private String baseUrl;
    
    /**
     * 构造函数
     * @param httpClientUtil HTTP客户端工具类
     */
    public EndfieldApiService(HttpClientUtil httpClientUtil) {
        this.httpClientUtil = httpClientUtil;
    }
    

    
    /**
     * 构建抽卡API请求URL
     * @param params URL参数
     * @return 完整的API URL
     */
    private String buildGachaApiUrl(Map<String, Object> params) {
        StringBuilder urlBuilder = new StringBuilder(baseUrl);
        urlBuilder.append("/webview/api/record/char");
        
        // 添加查询参数
        urlBuilder.append("?lang=").append(params.get("lang"));
        urlBuilder.append("&seq_id=").append(params.get("seq_id"));
        urlBuilder.append("&pool_type=").append(params.get("pool_type"));
        urlBuilder.append("&token=").append(params.get("token"));
        urlBuilder.append("&server_id=").append(params.get("server_id"));
        
        return urlBuilder.toString();
    }
    
    /**
     * 检查API服务是否可用
     * @return 是否可用
     */
    public boolean isApiAvailable() {
        try {
            String healthUrl = baseUrl + "/webview/api/health";
            return httpClientUtil.isUrlReachable(healthUrl);
        } catch (Exception e) {
            log.warn("检查API可用性失败", e);
            return false;
        }
    }
    
    /**
     * 获取API基础URL
     * @return API基础URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }
    
    /**
     * Endfield API异常类
     */
    public static class EndfieldApiException extends RuntimeException {
        public EndfieldApiException(String message) {
            super(message);
        }
        
        public EndfieldApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}