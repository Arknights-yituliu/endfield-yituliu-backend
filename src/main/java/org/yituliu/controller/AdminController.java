package org.yituliu.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yituliu.common.annotation.RedisCacheable;
import org.yituliu.common.utils.JsonMapper;
import org.yituliu.common.utils.Result;
import org.yituliu.entity.dto.pool.record.CharacterPoolRecordDTO;
import org.yituliu.entity.dto.pool.record.CharacterPoolRecordDataDTO;
import org.yituliu.entity.dto.pool.record.CharacterPoolRecordResponseDTO;
import org.yituliu.common.utils.FileUtil;

import java.util.List;


@RestController
public class TestController {
    @GetMapping("/")
    public Result<String> startStatus() {
        return Result.success("后端启动成功");
    }








}
