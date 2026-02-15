package org.yituliu.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;


/**
 * 角色卡池记录实体类
 * 对应数据库表：character_pool_record
 *
 * @author 山桜
 */
@TableName("character_pool_record")
public class CharacterPoolRecord {

    /**
     * 记录ID，主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 玩家UID
     */
    private String roleId;

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
     * 序列ID
     */
    private String seqId;

    /**
     * 语言
     */
    private String lang;

    /**
     * 卡池类型
     */
    private String poolType;

    /**
     * 服务器ID
     */
    private String serverId;

    public CharacterPoolRecord() {
    }

    public CharacterPoolRecord(Long id, String roleId, String poolId, String poolName, String charId, String charName, Integer rarity, Boolean isFree, Boolean isNew, String gachaTs, String seqId, String lang, String poolType, String serverId) {
        this.id = id;
        this.roleId = roleId;
        this.poolId = poolId;
        this.poolName = poolName;
        this.charId = charId;
        this.charName = charName;
        this.rarity = rarity;
        this.isFree = isFree;
        this.isNew = isNew;
        this.gachaTs = gachaTs;
        this.seqId = seqId;
        this.lang = lang;
        this.poolType = poolType;
        this.serverId = serverId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
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

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
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
