package org.yituliu.entity.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;


/**
 * 角色卡池记录实体类
 * 对应数据库表：character_pool_record
 * @author 山桜
 */

public class CharacterPoolRecordDTO {
    
    /**
     * 记录ID，复合主键之一
     */
    @TableId
    private String id;
    
    /**
     * 玩家ID（游戏内）
     */
    private String endfieldUid;
    
    /**
     * 玩家UID
     */
    private String uid;
    
    /**
     * 卡池ID
     */
    private String poolId;
    
    /**
     * 卡池名称
     */
    private String poolName;
    
    /**
     * 角色ID
     */
    private String charId;
    
    /**
     * 角色名称
     */
    private String charName;
    
    /**
     * 稀有度
     */
    private Integer rarity;
    
    /**
     * 是否免费抽取
     */
    private Boolean isFree;
    
    /**
     * 是否新角色
     */
    private Boolean isNew;
    
    /**
     * 抽卡时间戳
     */
    private String gachaTs;
    
    /**
     * 序列ID，复合主键之一
     */
    @TableId
    private String seqId;
    
    /**
     * 语言
     */
    private String language;
    
    /**
     * 卡池类型
     */
    private String poolType;
    
    /**
     * 服务器ID
     */
    private String serverId;

    public CharacterPoolRecordDTO() {
    }

    public CharacterPoolRecordDTO(String id, String endfieldUid, String uid, String poolId, String poolName, String charId, String charName, Integer rarity, Boolean isFree, Boolean isNew, String gachaTs, String seqId, String language, String poolType, String serverId) {
        this.id = id;
        this.endfieldUid = endfieldUid;
        this.uid = uid;
        this.poolId = poolId;
        this.poolName = poolName;
        this.charId = charId;
        this.charName = charName;
        this.rarity = rarity;
        this.isFree = isFree;
        this.isNew = isNew;
        this.gachaTs = gachaTs;
        this.seqId = seqId;
        this.language = language;
        this.poolType = poolType;
        this.serverId = serverId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEndfieldUid() {
        return endfieldUid;
    }

    public void setEndfieldUid(String endfieldUid) {
        this.endfieldUid = endfieldUid;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getPoolId() {
        return poolId;
    }

    public void setPoolId(String poolId) {
        this.poolId = poolId;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public String getCharId() {
        return charId;
    }

    public void setCharId(String charId) {
        this.charId = charId;
    }

    public String getCharName() {
        return charName;
    }

    public void setCharName(String charName) {
        this.charName = charName;
    }

    public Integer getRarity() {
        return rarity;
    }

    public void setRarity(Integer rarity) {
        this.rarity = rarity;
    }

    public Boolean getIsFree() {
        return isFree;
    }

    public void setIsFree(Boolean isFree) {
        this.isFree = isFree;
    }

    public Boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(Boolean isNew) {
        this.isNew = isNew;
    }

    public String getGachaTs() {
        return gachaTs;
    }

    public void setGachaTs(String gachaTs) {
        this.gachaTs = gachaTs;
    }

    public String getSeqId() {
        return seqId;
    }

    public void setSeqId(String seqId) {
        this.seqId = seqId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getPoolType() {
        return poolType;
    }

    public void setPoolType(String poolType) {
        this.poolType = poolType;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }
}
