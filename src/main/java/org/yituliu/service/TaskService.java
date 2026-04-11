package org.yituliu.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.yituliu.entity.po.TrafficStats;

import java.util.Calendar;
import java.util.Date;

@Service
public class TaskService {

    @Autowired
    private AdminService adminService;

    /**
     * 每小时统计任务
     * 每小时的第10分钟执行，统计上一小时的流量数据
     * 例如：15:10执行，统计14:00-15:00的数据
     * 
     * Cron表达式：0 10 * * * ?
     * - 0秒
     * - 10分
     * - 每小时
     * - 每天
     * - 每月
     * - 每周任意天
     */
    @Scheduled(cron = "0 10 * * * ?")
    public void hourlyTrafficStatsTask() {
        try {
            // 获取当前时间
            Calendar calendar = Calendar.getInstance();
            
            // 计算上一小时的整点时间（例如当前15:10，计算14:00:00.000）
            // 注意：先设置当前小时的整点，再减1小时，确保0点跨天计算正确
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            calendar.add(Calendar.HOUR_OF_DAY, -1);
            
            Date startTime = calendar.getTime();
            
            // 调用AdminService统计1小时流量数据
            // 这里统计的是14:00-15:00的完整1小时数据
            TrafficStats stats = adminService.calculateHourTrafficStats(startTime);
            
            // 记录执行日志
            System.out.println("每小时统计任务执行成功：" + startTime + " - 统计结果：PV=" + 
                stats.getPageViews() + ", UV=" + stats.getUniqueVisitors());
                
        } catch (Exception e) {
            // 记录错误日志
            System.err.println("每小时统计任务执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

   
    
}
