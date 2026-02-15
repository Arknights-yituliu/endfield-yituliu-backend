package org.yituliu.common.utils;

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
            .addInterceptor(new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Request request = chain.request()
                            .newBuilder()
                            .removeHeader("Accept-Encoding") // 移除Accept-Encoding头，避免服务器返回压缩数据
                            .build();
                    return chain.proceed(request);
                }
            })
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
                // 获取错误响应体，以便更好地诊断问题
                String errorBody = null;
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    try {
                        errorBody = responseBody.string();
                    } catch (Exception e) {
                        // 忽略获取错误体时的异常
                    }
                }
                throw new IOException("Unexpected code " + response + ". Error body: " + errorBody);
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

    /**
     * 发送POST请求（JSON格式）
     *
     * @param url     请求URL
     * @param json    请求体JSON字符串
     * @param headers 请求头参数
     * @return 返回响应字符串
     * @throws IOException 如果请求失败则抛出异常
     */
    public static String postJsonWithHeaders(String url, String json, Map<String, String> headers) throws IOException {
        // 创建请求体
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        RequestBody requestBody = RequestBody.create(json, mediaType);

        // 创建Request.Builder
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(requestBody);

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
     * 发送简单的POST请求（JSON格式，无请求头）
     *
     * @param url  请求URL
     * @param json 请求体JSON字符串
     * @return 返回响应字符串
     * @throws IOException 如果请求失败则抛出异常
     */
    public static String postJson(String url, String json) throws IOException {
        return postJsonWithHeaders(url, json, null);
    }

    /**
     * 发送POST请求（表单格式）
     *
     * @param url     请求URL
     * @param params  请求参数
     * @param headers 请求头参数
     * @return 返回响应字符串
     * @throws IOException 如果请求失败则抛出异常
     */
    public static String postFormWithHeaders(String url, Map<String, String> params, Map<String, String> headers) throws IOException {
        // 创建表单请求体
        FormBody.Builder formBuilder = new FormBody.Builder();
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                formBuilder.add(entry.getKey(), entry.getValue());
            }
        }
        RequestBody requestBody = formBuilder.build();

        // 创建Request.Builder
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(requestBody);

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
     * 发送简单的POST请求（表单格式，无请求头）
     *
     * @param url    请求URL
     * @param params 请求参数
     * @return 返回响应字符串
     * @throws IOException 如果请求失败则抛出异常
     */
    public static String postForm(String url, Map<String, String> params) throws IOException {
        return postFormWithHeaders(url, params, null);
    }
}
