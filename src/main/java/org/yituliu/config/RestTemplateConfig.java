package org.yituliu.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate配置类
 * 配置HTTP客户端连接参数和超时设置
 * @author 山桜
 */
@Configuration
public class RestTemplateConfig {
    
    /**
     * 配置RestTemplate Bean
     * @param builder RestTemplateBuilder
     * @return 配置好的RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .requestFactory(this::clientHttpRequestFactory)
                .setConnectTimeout(Duration.ofSeconds(10))  // 连接超时10秒
                .setReadTimeout(Duration.ofSeconds(30))     // 读取超时30秒
                .build();
    }
    
    /**
     * 配置HTTP请求工厂
     * @return 配置好的ClientHttpRequestFactory
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 10秒连接超时
        factory.setReadTimeout(30000);     // 30秒读取超时
        return factory;
    }
}