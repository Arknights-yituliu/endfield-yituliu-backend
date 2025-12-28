package org.yituliu.util;

import okhttp3.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OkHttp工具类，用于发送HTTP请求
 * 
 * @author yituliu
 */
public class OkHttpUtil {

    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)  // 连接超时时间
            .readTimeout(10, TimeUnit.SECONDS)     // 读取超时时间
            .writeTimeout(10, TimeUnit.SECONDS)    // 写入超时时间
            .build();

    /**
     * 发送带请求头的GET请求
     *
     * @param url     请求URL
     * @param headers 请求头参数
     * @return 返回响应字符串
     * @throws IOException 如果请求失败则抛出异常
     */
    public static String getWithHeaders(String url, Map<String, String> headers) throws IOException {
        // 创建Request.Builder
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .get();

        // 添加请求头
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        // 构建请求
        Request request = requestBuilder.build();

        // 发送请求并获取响应
        try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
            // 检查响应是否成功
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            // 获取响应体并返回
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                return responseBody.string();
            }
            return null;
        }
    }

    /**
     * 发送简单的GET请求（无请求头）
     *
     * @param url 请求URL
     * @return 返回响应字符串
     * @throws IOException 如果请求失败则抛出异常
     */
    public static String get(String url) throws IOException {
        return getWithHeaders(url, null);
    }
}
