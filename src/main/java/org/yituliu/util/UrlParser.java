package org.yituliu.util;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * URL参数解析工具类
 * 用于解析URL中的查询参数并提供默认值处理
 * @author 山桜
 */
public class UrlParser {
    
    /**
     * 解析URL中的参数并返回参数映射
     * 支持默认值处理，当参数不存在时使用默认值
     * @param urlString 要解析的URL字符串
     * @return 包含所有参数及其值的Map对象
     */
    public static Map<String, String> parseUrlParameters(String urlString) {
        Map<String, String> params = new HashMap<>();
        
        try {
            URL url = new URL(urlString);
            String query = url.getQuery();
            
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    int idx = pair.indexOf("=");
                    if (idx > 0) {
                        String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                        String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                        params.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("解析URL参数失败: " + e.getMessage(), e);
        }
        
        return params;
    }
    
    /**
     * 解析URL参数并返回特定参数值，如果参数不存在则返回默认值
     * @param urlString 要解析的URL字符串
     * @param paramName 参数名称
     * @param defaultValue 默认值
     * @return 参数值或默认值
     */
    public static String getParameter(String urlString, String paramName, String defaultValue) {
        Map<String, String> params = parseUrlParameters(urlString);
        return params.getOrDefault(paramName, defaultValue);
    }
    
    /**
     * 解析URL参数并返回特定参数值，如果参数不存在则返回默认值（整数类型）
     * @param urlString 要解析的URL字符串
     * @param paramName 参数名称
     * @param defaultValue 默认值
     * @return 参数值或默认值
     */
    public static int getIntParameter(String urlString, String paramName, int defaultValue) {
        try {
            String value = getParameter(urlString, paramName, String.valueOf(defaultValue));
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * 专门解析Endfield抽卡记录URL参数的方法
     * @param urlString Endfield抽卡记录URL
     * @return 包含所有抽卡相关参数的Map对象
     */
    public static Map<String, Object> parseEndfieldGachaUrl(String urlString) {
        Map<String, Object> result = new HashMap<>();
        
        // 解析基本参数
        result.put("lang", getParameter(urlString, "lang", "zh-cn"));
        result.put("seq_id", getIntParameter(urlString, "seq_id", 0));
        result.put("pool_type", getParameter(urlString, "pool_type", "E_CharacterGachaPoolType_Special"));
        result.put("token", getParameter(urlString, "token", ""));
        result.put("server_id", getIntParameter(urlString, "server_id", 0));
        
        return result;
    }
    
    /**
     * 测试方法：演示如何使用URL解析功能
     */
    public static void main(String[] args) {
        String testUrl = "";
        
        // 方法1：使用通用解析方法
        Map<String, String> params = parseUrlParameters(testUrl);
        System.out.println("通用解析结果:");
        params.forEach((key, value) -> System.out.println(key + ": " + value));
        
        System.out.println("\n---\n");
        
        // 方法2：使用专用方法解析Endfield抽卡URL
        Map<String, Object> endfieldParams = parseEndfieldGachaUrl(testUrl);
        System.out.println("Endfield抽卡URL解析结果:");
        endfieldParams.forEach((key, value) -> System.out.println(key + ": " + value));
        
        System.out.println("\n---\n");
        
        // 方法3：获取单个参数值
        String lang = getParameter(testUrl, "lang", "zh-cn");
        int seqId = getIntParameter(testUrl, "seq_id", 0);
        System.out.println("单个参数获取:");
        System.out.println("lang: " + lang);
        System.out.println("seq_id: " + seqId);
    }
}