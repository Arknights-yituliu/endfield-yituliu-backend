package org.yituliu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import org.yituliu.entity.po.AccessLog;

import java.util.Date;

/**
 * 访问日志Mapper接口
 * 对应数据库表：access_log
 */
@Repository
public interface AccessLogMapper extends BaseMapper<AccessLog> {
    
    /**
     * 统计指定时间范围内的页面浏览量(PV)
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 页面浏览量
     */
    Long countPageViews(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
    
    /**
     * 统计指定时间范围内的独立访客数(UV)
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 独立访客数
     */
    Long countUniqueVisitors(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
}
