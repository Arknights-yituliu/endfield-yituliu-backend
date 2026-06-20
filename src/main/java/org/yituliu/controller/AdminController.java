package org.yituliu.controller;


import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yituliu.common.utils.Result;
import org.yituliu.entity.dto.AccessLogDTO;
import org.yituliu.entity.vo.UrlPeriodDataVO;
import org.yituliu.entity.vo.UrlTotalVisitVO;
import org.yituliu.entity.vo.UrlVisitGroupVO;

import org.yituliu.service.AdminService;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Date;
import java.util.List;

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

    /**
     * 获取指定时间范围内每个URL每天的访问次数统计（按URL分组）
     * 返回Top 30 URL，时间范围最大不超过30天
     * @param start 开始时间（格式 yyyy-MM-dd HH:mm:ss）
     * @param end   结束时间（格式 yyyy-MM-dd HH:mm:ss）
     * @return URL分组访问量列表
     */
    @GetMapping("/access-log/daily")
    public Result<List<UrlVisitGroupVO>> getUrlDailyVisits(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date end) {
        List<UrlVisitGroupVO> visits = adminService.getUrlDailyVisits(start, end);
        return Result.success(visits);
    }

    /**
     * 获取指定时间范围内每个URL的总访问次数
     * Top 15 单独列出，其余合并为"其他"，第一项固定为"访问总和"，时间范围最大不超过30天
     * @param start 开始时间（格式 yyyy-MM-dd HH:mm:ss）
     * @param end   结束时间（格式 yyyy-MM-dd HH:mm:ss）
     * @return URL总访问量列表
     */
    @GetMapping("/access-log/total")
    public Result<List<UrlTotalVisitVO>> getUrlTotalVisits(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date end) {
        List<UrlTotalVisitVO> visits = adminService.getUrlTotalVisits(start, end);
        return Result.success(visits);
    }

    /**
     * 获取指定时间范围内每小时的总访问量（所有URL聚合）
     * 时间范围最大不超过30天
     * @param start 开始时间（格式 yyyy-MM-dd HH:mm:ss）
     * @param end   结束时间（格式 yyyy-MM-dd HH:mm:ss）
     * @return 每小时总访问量列表
     */
    @GetMapping("/access-log/hourly-total")
    public Result<List<UrlPeriodDataVO>> getHourlyTotalVisits(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date end) {
        List<UrlPeriodDataVO> visits = adminService.getHourlyTotalVisits(start, end);
        return Result.success(visits);
    }

    /**
     * 获取指定时间范围内每日的总访问量（所有URL聚合）
     * 时间范围最大不超过30天
     * @param start 开始时间（格式 yyyy-MM-dd HH:mm:ss）
     * @param end   结束时间（格式 yyyy-MM-dd HH:mm:ss）
     * @return 每日总访问量列表
     */
    @GetMapping("/access-log/daily-total")
    public Result<List<UrlPeriodDataVO>> getDailyTotalVisits(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date end) {
        List<UrlPeriodDataVO> visits = adminService.getDailyTotalVisits(start, end);
        return Result.success(visits);
    }
}
