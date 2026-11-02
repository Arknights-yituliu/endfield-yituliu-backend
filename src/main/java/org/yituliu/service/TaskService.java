package org.yituliu.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.yituliu.common.utils.LogUtils;
import org.yituliu.entity.po.TrafficStats;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TaskService {

    /**
     * 回填覆盖的天数
     * 取 7 天覆盖常用查询范围，避免首次启动时一次性统计过多历史数据
     */
    private static final int BACKFILL_DAYS = 7;

    /**
     * 每小时的毫秒数
     */
    private static final long ONE_HOUR_MILLIS = 60L * 60 * 1000;

    /**
     * 回填是否正在执行，避免启动回填与每日补偿回填并发重复统计
     */
    private final AtomicBoolean backfillRunning = new AtomicBoolean(false);

    @Autowired
    private AdminService adminService;

    @Autowired
    @Qualifier("asyncExecutor")
    private Executor asyncExecutor;

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

    /**
     * 统计上一个完整小时的访问量并写入预聚合表
     * 每小时执行 3 次（每 20 分钟一次），重复执行幂等，供 /admin/access-log/hourly 接口直接读取
     */
    @Scheduled(cron = "0 0/20 * * * ?")
    public void statisticsLastHourAccessVisits() {
        try {
            adminService.statisticsLastHour();
        } catch (Exception e) {
            System.err.println("小时访问量统计任务执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 删除 access_log_hourly_stats 表中已过期的统计数据
     * 每天凌晨执行一次，清理重跑后遗留的 EXPIRE 记录
     */
    @Scheduled(cron = "0 20 4 * * ?")
    public void deleteExpireAccessHourlyStats() {
        try {
            adminService.deleteExpireHourlyStats();
        } catch (Exception e) {
            System.err.println("清理过期小时访问量统计任务执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 统计昨天的每个URL访问量并写入预聚合表
     * 一天执行 3 次（0:00、8:00、16:00），重复执行幂等，供 /admin/access-log/url/daily 接口直接读取
     */
    @Scheduled(cron = "0 0 0/8 * * ?")
    public void statisticsYesterdayUrlDailyVisits() {
        try {
            adminService.statisticsYesterdayUrlDailyVisits();
        } catch (Exception e) {
            System.err.println("昨日URL访问量统计任务执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 统计今天的每个URL访问量并写入预聚合表
     * 每 30 分钟执行一次，反复重跑刷新今天的最新数据，供 /admin/access-log/url/daily 接口直接读取
     */
    @Scheduled(cron = "0 0/30 * * * ?")
    public void statisticsTodayUrlDailyVisits() {
        try {
            adminService.statisticsTodayUrlDailyVisits();
        } catch (Exception e) {
            System.err.println("今日URL访问量统计任务执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 删除 access_log_url_daily_stats 表中已过期的统计数据
     * 每天凌晨执行一次，清理重跑后遗留的 EXPIRE 记录
     */
    @Scheduled(cron = "0 25 4 * * ?")
    public void deleteExpireAccessUrlDailyStats() {
        try {
            adminService.deleteExpireUrlDailyStats();
        } catch (Exception e) {
            System.err.println("清理过期URL每日访问量统计任务执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 应用启动完成后触发一次访问统计回填
     * 新表上线时没有历史数据，启动后立即补齐最近 7 天，接口不用等定时任务积累
     */
    @EventListener(ApplicationReadyEvent.class)
    public void backfillAccessStatsOnStartup() {
        submitAccessStatsBackfill();
    }

    /**
     * 每天 3:30 补偿回填最近 7 天缺失的访问统计
     * 只补齐任务记录表中缺失的周期，正常情况下几乎不产生统计开销
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void backfillAccessStatsTask() {
        submitAccessStatsBackfill();
    }

    /**
     * 将回填提交到异步线程池
     * 逐小时回填耗时较长，异步执行避免阻塞同一调度线程上的其他定时任务
     */
    private void submitAccessStatsBackfill() {
        if (!backfillRunning.compareAndSet(false, true)) {
            LogUtils.info("访问统计回填正在执行中，本次触发跳过");
            return;
        }
        asyncExecutor.execute(() -> {
            try {
                backfillAccessStats();
            } catch (Exception e) {
                LogUtils.error("访问统计回填失败", e);
            } finally {
                backfillRunning.set(false);
            }
        });
    }

    /**
     * 回填最近 7 天缺失的小时访问量与URL每日访问量统计
     * 小时统计回填到当前整点之前（最近 1 小时由每 20 分钟的任务负责）；
     * URL每日统计回填到昨天为止（今天的数据由每 30 分钟的任务持续刷新），
     * 与常规任务错开统计周期，避免并发重复写入同一周期
     */
    private void backfillAccessStats() {
        // 回填起始日 = 今天零点往前推 BACKFILL_DAYS 天
        Calendar dayCal = Calendar.getInstance();
        dayCal.set(Calendar.HOUR_OF_DAY, 0);
        dayCal.set(Calendar.MINUTE, 0);
        dayCal.set(Calendar.SECOND, 0);
        dayCal.set(Calendar.MILLISECOND, 0);
        Date today = dayCal.getTime();
        dayCal.add(Calendar.DAY_OF_YEAR, -BACKFILL_DAYS);
        Date startDay = dayCal.getTime();

        // 小时统计结束边界：当前整点再往前 1 小时（不含），最近 1 小时留给常规任务
        Calendar hourCal = Calendar.getInstance();
        hourCal.set(Calendar.MINUTE, 0);
        hourCal.set(Calendar.SECOND, 0);
        hourCal.set(Calendar.MILLISECOND, 0);
        hourCal.add(Calendar.HOUR_OF_DAY, -1);
        Date endHour = hourCal.getTime();

        // 1. 逐小时补齐：先查已统计的整点，只对缺失的小时执行统计
        Set<Long> hourlyCoverage = adminService.getStatisticHourlyCoverage(startDay, endHour);
        int filledHours = 0;
        Calendar cursor = Calendar.getInstance();
        cursor.setTime(startDay);
        while (cursor.getTime().before(endHour)) {
            Date hourStart = cursor.getTime();
            if (!hourlyCoverage.contains(hourStart.getTime())) {
                Date hourEnd = new Date(hourStart.getTime() + ONE_HOUR_MILLIS);
                try {
                    adminService.statisticsLastHour(hourStart, hourEnd);
                    filledHours++;
                } catch (Exception e) {
                    // 单个小时失败不影响后续小时，记录后继续
                    LogUtils.error("小时访问量回填失败：" + hourStart, e);
                }
            }
            cursor.add(Calendar.HOUR_OF_DAY, 1);
        }
        LogUtils.info("小时访问量回填完成，区间 {} 至 {}，共补齐 {} 个小时", startDay, endHour, filledHours);

        // 2. 逐天补齐URL访问量：今天的数据留给常规任务刷新，避免与其并发
        Set<Long> dailyCoverage = adminService.getUrlDailyStatCoverage(startDay, today);
        int filledDays = 0;
        cursor.setTime(startDay);
        while (cursor.getTime().before(today)) {
            Date dayStart = cursor.getTime();
            if (!dailyCoverage.contains(dayStart.getTime())) {
                Calendar nextDay = Calendar.getInstance();
                nextDay.setTime(dayStart);
                nextDay.add(Calendar.DAY_OF_YEAR, 1);
                try {
                    adminService.statisticsUrlDailyVisits(dayStart, nextDay.getTime());
                    filledDays++;
                } catch (Exception e) {
                    // 单天失败不影响后续日期，记录后继续
                    LogUtils.error("URL每日访问量回填失败：" + dayStart, e);
                }
            }
            cursor.add(Calendar.DAY_OF_YEAR, 1);
        }
        LogUtils.info("URL每日访问量回填完成，区间 {} 至 {}，共补齐 {} 天", startDay, today, filledDays);
    }

}
