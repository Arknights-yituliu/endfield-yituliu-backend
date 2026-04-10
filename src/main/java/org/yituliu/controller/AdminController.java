package org.yituliu.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yituliu.common.utils.Result;
import org.yituliu.entity.dto.AccessLogDTO;

import org.yituliu.service.AdminService;

import jakarta.servlet.http.HttpServletRequest;

@RestController

public class AdminController {

    private final AdminService adminService;

   
    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * 保存访问日志
     * @param accessLog 访问日志对象
     * @return 操作结果
     */
    @PostMapping("/access-log")
    public Result<Void> saveAccessLog(HttpServletRequest request, @RequestBody AccessLogDTO accessLogDTO) {
        adminService.saveAccessLog(request, accessLogDTO);
        return Result.success();
    }
}
