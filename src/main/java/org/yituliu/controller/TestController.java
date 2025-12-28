package org.yituliu.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yituliu.common.enums.ResultCode;
import org.yituliu.common.utils.Result;
import org.yituliu.util.OkHttpUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
public class TestController {
    @GetMapping("/")
    public Result<String> startStatus() {
        return Result.success("后端启动成功");
    }

    /**
     * 测试OkHttp发送带请求头的GET请求
     *
     * @return 返回请求结果
     */
    @GetMapping("/test-okhttp")
    public Result<String> testOkHttp() {
        try {
            // 请求URL（示例使用JSONPlaceholder的测试API）
            String url = "https://jsonplaceholder.typicode.com/posts/1";

            // 创建请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "OkHttp Example");
            headers.put("Accept", "application/json");
            headers.put("Authorization", "Bearer token123"); // 示例授权头

            // 发送带请求头的GET请求
            String response = OkHttpUtil.getWithHeaders(url, headers);

            return Result.success("请求成功");
        } catch (IOException e) {
            e.printStackTrace();
            return Result.failure(ResultCode.INTERFACE_OUTER_INVOKE_ERROR);
        }
    }
}
