package org.yituliu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import org.yituliu.entity.po.AccessLog;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 访问日志Mapper接口
 * 对应数据库表：access_log
 */
@Repository
public interface AccessLogMapper extends BaseMapper<AccessLog> {
    
    /**
     * 统计指定时间范围内的总访问量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 总访问量
     */
    Long countTotalVisits(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 统计指定时间范围内的独立访客数（按IP去重）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 独立访客数
     */
    Long countUniqueVisitors(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 按地域统计访问量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 地域访问量统计列表
     */
    List<Map<String, Object>> countByRegion(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 按设备统计访问量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 设备访问量统计列表
     */
    List<Map<String, Object>> countByDevice(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 按浏览器统计访问量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 浏览器访问量统计列表
     */
    List<Map<String, Object>> countByBrowser(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 按操作系统统计访问量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作系统访问量统计列表
     */
    List<Map<String, Object>> countByOs(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 按URL统计访问量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return URL访问量统计列表
     */
    List<Map<String, Object>> countByUrl(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 按小时统计访问量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 小时访问量统计列表
     */
    List<Map<String, Object>> countByHour(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 按天统计访问量
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 天访问量统计列表
     */
    List<Map<String, Object>> countByDay(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
}
