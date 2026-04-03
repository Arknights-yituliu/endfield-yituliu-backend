package org.yituliu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;
import org.yituliu.entity.po.AccessLog;

/**
 * 访问日志Mapper接口
 * 对应数据库表：access_log
 */
@Repository
public interface AccessLogMapper extends BaseMapper<AccessLog> {
}
