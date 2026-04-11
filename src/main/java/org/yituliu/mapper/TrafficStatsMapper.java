package org.yituliu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;
import org.yituliu.entity.po.TrafficStats;

/**
 * 流量统计Mapper接口
 * 对应数据库表：traffic_stats
 */
@Repository
public interface TrafficStatsMapper extends BaseMapper<TrafficStats> {
}