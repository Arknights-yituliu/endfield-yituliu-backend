package org.yituliu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.yituliu.entity.po.WeaponPoolRecord;

public interface WeaponPoolRecordMapper extends BaseMapper<WeaponPoolRecord> {
    
    /**
     * 批量插入武器卡池记录
     * @param list 武器卡池记录列表
     * @return 影响行数
     */
    int batchInsert(java.util.List<WeaponPoolRecord> list);
    
    /**
     * 查询指定roleId下的最大seq_id值（数字版本）
     * @param roleId 角色ID
     * @return 最大seq_id值
     */
    Integer getMaxSeqIdNumber(String roleId);
    
    /**
     * 查询指定roleId下的最大seq_id值（字符串版本）
     * @param roleId 角色ID
     * @return 最大seq_id值
     */
    String getMaxSeqIdString(String roleId);
}