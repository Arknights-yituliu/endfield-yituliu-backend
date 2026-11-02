package org.yituliu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;
import org.yituliu.entity.po.AccessLogHourlyStats;

/**
 * 每小时访问量统计Mapper接口
 * 对应数据库表：access_log_hourly_stats
 */
@Repository
public interface AccessLogHourlyStatsMapper extends BaseMapper<AccessLogHourlyStats> {
}
