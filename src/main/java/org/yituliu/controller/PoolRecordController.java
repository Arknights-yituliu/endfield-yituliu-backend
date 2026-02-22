package org.yituliu.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yituliu.common.utils.Result;
import org.yituliu.entity.po.CharacterPoolRecord;
import org.yituliu.entity.po.EndministratorInfo;
import org.yituliu.entity.vo.PoolRecordVO;
import org.yituliu.service.CharacterPoolRecordService;
import org.yituliu.service.PoolRecordTaskService;

import java.util.List;


@RestController
public class PoolRecordController {



    private final CharacterPoolRecordService characterPoolRecordService;
    private final PoolRecordTaskService poolRecordTaskService;

    public PoolRecordController(CharacterPoolRecordService characterPoolRecordService, PoolRecordTaskService poolRecordTaskService) {

        this.characterPoolRecordService = characterPoolRecordService;
        this.poolRecordTaskService = poolRecordTaskService;
    }



    @PostMapping("/pool-record/create-task")
    public Result<String> createTask(HttpServletRequest httpServletRequest, @RequestParam String hgToken) {
        String taskId = poolRecordTaskService.createTask(httpServletRequest, hgToken);
        return Result.success(taskId);
    }

    @GetMapping("/pool-record/check-task")
    public Result<EndministratorInfo> checkTask(@RequestParam String taskId){
        return Result.success(poolRecordTaskService.checkTask(taskId));
    }





    @GetMapping("/pool-record/character/list")
    public Result<PoolRecordVO> getCharacterPoolRecordData(HttpServletRequest httpServletRequest, @RequestParam String taskId) {

        return Result.success( poolRecordTaskService.getCharacterPoolRecordData(httpServletRequest, taskId));
    }
}
