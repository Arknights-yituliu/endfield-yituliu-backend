package org.yituliu.entity.dto.pool.record.response;

/**
 * 单个抽卡记录DTO
 * 对应Endfield抽卡记录API中的单个记录项
 * @author 山桜
 */
public class CharacterPoolRecordDTO {

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
     * 稀有度（3-5星）
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
     * 抽卡时间戳（毫秒）
     */
    private String gachaTs;

    /**
     * 序列ID
     */
    private String seqId;

    /**
     * 默认构造函数
     */
    public CharacterPoolRecordDTO() {
    }

    /**
     * 全参构造函数
     */
    public CharacterPoolRecordDTO(String poolId, String poolName, String charId, String charName,
                                  Integer rarity, Boolean isFree, Boolean isNew, String gachaTs, String seqId) {
        this.poolId = poolId;
        this.poolName = poolName;
        this.charId = charId;
        this.charName = charName;
        this.rarity = rarity;
        this.isFree = isFree;
        this.isNew = isNew;
        this.gachaTs = gachaTs;
        this.seqId = seqId;
    }

    // Getter和Setter方法
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


    @Override
    public String toString() {
        return "GachaRecordDTO{" +
                "poolId='" + poolId + '\'' +
                ", poolName='" + poolName + '\'' +
                ", charId='" + charId + '\'' +
                ", charName='" + charName + '\'' +
                ", rarity=" + rarity +
                ", isFree=" + isFree +
                ", isNew=" + isNew +
                ", gachaTs='" + gachaTs + '\'' +
                ", seqId='" + seqId + '\'' +
                '}';
    }
}