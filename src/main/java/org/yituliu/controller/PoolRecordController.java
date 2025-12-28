package org.yituliu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yituliu.common.utils.Result;
import org.yituliu.entity.po.CharacterPoolRecord;
import org.yituliu.service.CharacterPoolRecordService;

import java.util.List;

@RestController
public class PoolRecordController {

    private final CharacterPoolRecordService characterPoolRecordService;

    public PoolRecordController(CharacterPoolRecordService characterPoolRecordService) {
        this.characterPoolRecordService = characterPoolRecordService;
    }

    @PostMapping("/pool-record/character/upload")
    public Result<String> uploadCharacterPoolRecord(HttpServletRequest httpServletRequest, @RequestParam String uid, @RequestParam String url) {
        ;
        return Result.success(characterPoolRecordService.saveCharacterPoolRecord(httpServletRequest,uid, url));
    }

    @GetMapping("/pool-record/character/list")
    public Result<List<CharacterPoolRecord>> getCharacterPoolRecordData(HttpServletRequest httpServletRequest, @RequestParam String uid) {
        return Result.success(characterPoolRecordService.getCharacterPoolRecordData(httpServletRequest, uid));
    }
}
