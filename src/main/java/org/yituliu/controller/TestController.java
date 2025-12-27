package org.yituliu.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yituliu.common.utils.Result;

@RestController
public class TestController {
    @GetMapping("/")
    public Result<String> startStatus() {

        return Result.success("后端启动成功");
    }
}
