package org.yituliu.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yituliu.common.utils.Result;



@RestController

public class GeneralDataCollectionController {
    
    /**
     * 上传通用数据  Universal Data Upload System - Udus
     * @return
     */
    @PostMapping("/udus")
    public Result<String> uploadGeneralDataCollection() {
        return Result.success("上传成功");
    }

    
}
