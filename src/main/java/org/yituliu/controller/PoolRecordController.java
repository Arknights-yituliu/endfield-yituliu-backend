package org.yituliu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yituliu.common.utils.Result;
import org.yituliu.entity.log.BatchProcessResult;
import org.yituliu.entity.po.CharacterPoolRecord;
import org.yituliu.service.CharacterPoolRecordService;
import org.yituliu.service.CharacterPoolRecordServiceV2;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
public class PoolRecordController {

    private final CharacterPoolRecordService characterPoolRecordService;

    private final CharacterPoolRecordServiceV2 characterPoolRecordServiceV2;

    public PoolRecordController(CharacterPoolRecordService characterPoolRecordService, CharacterPoolRecordServiceV2 characterPoolRecordServiceV2) {
        this.characterPoolRecordService = characterPoolRecordService;
        this.characterPoolRecordServiceV2 = characterPoolRecordServiceV2;
    }

    @PostMapping("/pool-record/character/upload/ew")
    public Result<String> uploadCharacterPoolRecord(HttpServletRequest httpServletRequest, @RequestParam String uid, @RequestParam String url) {
        return Result.success(characterPoolRecordService.saveCharacterPoolRecord(httpServletRequest,uid, url));
    }

    @PostMapping("/pool-record/character/upload")
    public Result<CompletableFuture<BatchProcessResult>> uploadCharacterPoolRecordAsync(HttpServletRequest httpServletRequest, @RequestParam String uid, @RequestParam String url) {
        return Result.success(characterPoolRecordServiceV2.saveCharacterPoolRecordAsync(httpServletRequest,uid, url));
    }


    @GetMapping("/pool-record/character/list")
    public Result<List<CharacterPoolRecord>> getCharacterPoolRecordData(HttpServletRequest httpServletRequest, @RequestParam String uid) {
        return Result.success(characterPoolRecordService.getCharacterPoolRecordData(httpServletRequest, uid));
    }
}
