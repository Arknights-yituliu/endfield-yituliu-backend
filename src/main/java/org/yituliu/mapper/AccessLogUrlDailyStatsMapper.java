package org.yituliu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import org.yituliu.entity.po.AccessLogUrlDailyStats;

import java.util.List;

/**
 * URL每日访问量统计Mapper接口
 * 对应数据库表：access_log_url_daily_stats
 */
@Repository
public interface AccessLogUrlDailyStatsMapper extends BaseMapper<AccessLogUrlDailyStats> {

    /**
     * 批量插入某天的URL访问量统计
     * 单日统计一次性写入几十到几百条，逐条 insert 会产生大量数据库往返，
     * 配合 rewriteBatchedStatements 合并为一条多值 INSERT
     *
     * @param list 待写入的统计记录，不可为空
     * @return 写入条数
     */
    int insertBatch(@Param("list") List<AccessLogUrlDailyStats> list);
}
