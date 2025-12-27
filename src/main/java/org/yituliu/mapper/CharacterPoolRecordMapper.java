package org.yituliu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.springframework.stereotype.Repository;
import org.yituliu.entity.po.CharacterPoolRecord;

/**
 * 角色卡池记录Mapper接口
 * 对应数据库表：character_pool_record
 * @author 山桜
 */
@Repository
public interface CharacterPoolRecordMapper extends BaseMapper<CharacterPoolRecord> {
    
    /**
     * 根据玩家UID查询抽卡记录
     * @param uid 玩家UID
     * @return 抽卡记录列表
     */
    // List<CharacterPoolRecord> selectByUid(@Param("uid") String uid);
    
    /**
     * 根据卡池ID和玩家UID查询抽卡记录
     * @param poolId 卡池ID
     * @param uid 玩家UID
     * @return 抽卡记录列表
     */
    // List<CharacterPoolRecord> selectByPoolIdAndUid(@Param("poolId") String poolId, @Param("uid") String uid);
    
    /**
     * 根据时间范围查询抽卡记录
     * @param startTs 开始时间戳
     * @param endTs 结束时间戳
     * @param uid 玩家UID
     * @return 抽卡记录列表
     */
    // List<CharacterPoolRecord> selectByTimeRange(@Param("startTs") String startTs, @Param("endTs") String endTs, @Param("uid") String uid);
}
