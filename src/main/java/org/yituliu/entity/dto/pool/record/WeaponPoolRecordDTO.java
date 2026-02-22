package org.yituliu.entity.dto.pool.record;

/**
 * 单个抽卡记录DTO
 * 对应Endfield抽卡记录API中的单个记录项
 * @author 山桜
 */
public class WeaponPoolRecordDTO {

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
    private String weaponId;

    /**
     * 角色名称
     */
    private String weaponName;

    private String weaponType;

    /**
     * 稀有度（3-5星）
     */
    private Integer rarity;

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
    public WeaponPoolRecordDTO() {
    }

    public WeaponPoolRecordDTO(String poolId, String poolName, String weaponId, String weaponName, String weaponType, Integer rarity, Boolean isNew, String gachaTs, String seqId) {
        this.poolId = poolId;
        this.poolName = poolName;
        this.weaponId = weaponId;
        this.weaponName = weaponName;
        this.weaponType = weaponType;
        this.rarity = rarity;
        this.isNew = isNew;
        this.gachaTs = gachaTs;
        this.seqId = seqId;
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

    public String getWeaponId() {
        return weaponId;
    }

    public void setWeaponId(String weaponId) {
        this.weaponId = weaponId;
    }

    public String getWeaponName() {
        return weaponName;
    }

    public void setWeaponName(String weaponName) {
        this.weaponName = weaponName;
    }

    public String getWeaponType() {
        return weaponType;
    }

    public void setWeaponType(String weaponType) {
        this.weaponType = weaponType;
    }

    public Integer getRarity() {
        return rarity;
    }

    public void setRarity(Integer rarity) {
        this.rarity = rarity;
    }

    public Boolean getIsNew() {
        return isNew;
    }

    public void setIsNew(Boolean aNew) {
        isNew = aNew;
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
        return "WeaponPoolRecordDTO{" +
                "poolId='" + poolId + '\'' +
                ", poolName='" + poolName + '\'' +
                ", weaponId='" + weaponId + '\'' +
                ", weaponName='" + weaponName + '\'' +
                ", weaponType='" + weaponType + '\'' +
                ", rarity=" + rarity +
                ", isNew=" + isNew +
                ", gachaTs='" + gachaTs + '\'' +
                ", seqId='" + seqId + '\'' +
                '}';
    }
}