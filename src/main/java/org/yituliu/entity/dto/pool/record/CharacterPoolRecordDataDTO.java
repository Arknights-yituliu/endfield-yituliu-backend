package org.yituliu.entity.dto.pool.record;

import java.util.List;

/**
 * 抽卡数据DTO
 * 包含抽卡记录列表和分页信息
 * @author 山桜
 */
public class CharacterPoolRecordDataDTO {
    
    /**
     * 抽卡记录列表
     */
    private List<CharacterPoolRecordDTO> list;
    
    /**
     * 是否还有更多数据
     */
    private Boolean hasMore;
    
    /**
     * 默认构造函数
     */
    public CharacterPoolRecordDataDTO() {
    }
    
    /**
     * 全参构造函数
     */
    public CharacterPoolRecordDataDTO(List<CharacterPoolRecordDTO> list, Boolean hasMore) {
        this.list = list;
        this.hasMore = hasMore;
    }
    
    // Getter和Setter方法
    public List<CharacterPoolRecordDTO> getList() {
        return list;
    }
    
    public void setList(List<CharacterPoolRecordDTO> list) {
        this.list = list;
    }
    
    public Boolean getHasMore() {
        return hasMore;
    }
    
    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }
    
    @Override
    public String toString() {
        return "GachaDataDTO{" +
                "list=" + list +
                ", hasMore=" + hasMore +
                '}';
    }
}