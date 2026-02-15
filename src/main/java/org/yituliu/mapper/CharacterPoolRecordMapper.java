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
     * 查询指定roleId下的最大seq_id值（字符串版本）
     * @param roleId 用户ID
     * @return 最大seq_id字符串，如果不存在则返回null
     */
    String getMaxSeqIdString(@Param("roleId") String roleId);

}
