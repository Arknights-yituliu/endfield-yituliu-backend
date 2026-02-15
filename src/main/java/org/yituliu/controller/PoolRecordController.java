package org.yituliu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yituliu.common.utils.Result;
import org.yituliu.entity.po.CharacterPoolRecord;
import org.yituliu.entity.po.EndministratorInfo;
import org.yituliu.service.CharacterPoolRecordService;

import java.util.List;


@RestController
public class PoolRecordController {



    private final CharacterPoolRecordService characterPoolRecordService;

    public PoolRecordController(CharacterPoolRecordService characterPoolRecordService) {

        this.characterPoolRecordService = characterPoolRecordService;
    }



    @PostMapping("/pool-record/create-task")
    public Result<String> createTask(HttpServletRequest httpServletRequest, @RequestParam String hgToken) {
        String taskId = characterPoolRecordService.createTask(httpServletRequest, hgToken);
        return Result.success(taskId);
    }

    @GetMapping("/pool-record/check-task")
    public Result<EndministratorInfo> checkTask(@RequestParam String taskId){
        return Result.success(characterPoolRecordService.checkTask(taskId));
    }





    @GetMapping("/pool-record/character/list")
    public Result<List<CharacterPoolRecord>> getCharacterPoolRecordData(HttpServletRequest httpServletRequest, @RequestParam String roleId) {

        return Result.success( characterPoolRecordService.getCharacterPoolRecordData(httpServletRequest, roleId));
    }
}
