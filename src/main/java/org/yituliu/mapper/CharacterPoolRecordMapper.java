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
   
}
