package org.yituliu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.yituliu.entity.po.PlayerPoolRecordTask;

public interface PlayerPoolRecordTaskMapper extends BaseMapper<PlayerPoolRecordTask> {
    
    /**
     * 查询create_time倒序下，startFlag = false的第一个记录
     * @return PlayerPoolRecordTask对象
     */
    PlayerPoolRecordTask selectFirstByStartFlagFalseOrderByCreateTimeDesc();
}
