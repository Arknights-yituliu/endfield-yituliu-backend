package org.yituliu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yituliu.common.utils.Result;
import org.yituliu.service.CharacterPoolRecordService;

@RestController
public class PoolRecordController {

    private final CharacterPoolRecordService characterPoolRecordService;

    public PoolRecordController(CharacterPoolRecordService characterPoolRecordService) {
        this.characterPoolRecordService = characterPoolRecordService;
    }

    @PostMapping("/pool-record/character/upload")
    public Result<String> uploadCharacterPoolRecord(HttpServletRequest httpServletRequest, @RequestParam String uid, @RequestParam String url) {
        characterPoolRecordService.savePoolRecord(httpServletRequest,uid, url);
        return Result.success("后端启动成功");
    }
}
