package org.yituliu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import org.yituliu.entity.po.CharacterPoolRecord;

import java.util.List;

/**
 * 角色卡池记录Mapper接口
 * 对应数据库表：character_pool_record
 * @author 山桜
 */
@Repository
public interface CharacterPoolRecordMapper extends BaseMapper<CharacterPoolRecord> {
    
    /**
     * 批量插入角色卡池记录
     * @param list 角色卡池记录列表
     * @return 插入记录数
     */
    int batchInsert(@Param("list") List<CharacterPoolRecord> list);

   
    /**
     * 查询指定roleId下的最大seq_id值（数字版本）
     * @param roleId 用户ID
     * @return 最大seq_id值，如果不存在则返回null
     */
    Integer getMaxSeqIdNumber(@Param("roleId") String roleId);

    /**
     * 查询指定roleId、指定卡池类型下的最大seq_id值（数字版本）
     * 用于按卡池类型增量导入，新增卡池类型无历史记录时返回null，触发全量补齐
     * @param roleId 用户ID
     * @param poolType 卡池类型
     * @return 最大seq_id值，如果该卡池类型不存在记录则返回null
     */
    Integer getMaxSeqIdNumberByPoolType(@Param("roleId") String roleId, @Param("poolType") String poolType);

    /**
     * 查询指定roleId下的最大seq_id值（字符串版本）
     * @param roleId 用户ID
     * @return 最大seq_id字符串，如果不存在则返回null
     */
    String getMaxSeqIdString(@Param("roleId") String roleId);

}
