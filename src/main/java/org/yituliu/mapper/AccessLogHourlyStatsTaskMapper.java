package org.yituliu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;
import org.yituliu.entity.po.AccessLogHourlyStatsTask;

/**
 * 小时访问量统计任务记录Mapper接口
 * 对应数据库表：access_log_hourly_stats_task
 */
@Repository
public interface AccessLogHourlyStatsTaskMapper extends BaseMapper<AccessLogHourlyStatsTask> {
}
