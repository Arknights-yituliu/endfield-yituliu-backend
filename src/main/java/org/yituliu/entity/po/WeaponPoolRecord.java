package org.yituliu.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("weapon_pool_record")
public class WeaponPoolRecord {
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
     * 武器ID
     */
    private String weaponId;
    /**
     * 武器名称
     */
    private String weaponName;
    private String weaponType;
    /**
     * 稀有度
     */
    private Integer rarity;
    /**
     * 是否新武器
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
     * 服务器ID
     */
    private String serverId;

    public WeaponPoolRecord() {
    }

    public WeaponPoolRecord(Long id, String roleId, String poolId, String poolName, String weaponId, String weaponName, String weaponType, Integer rarity, Boolean isNew, String gachaTs, String seqId, String lang, String serverId) {
        this.id = id;
        this.roleId = roleId;
        this.poolId = poolId;
        this.poolName = poolName;
        this.weaponId = weaponId;
        this.weaponName = weaponName;
        this.weaponType = weaponType;
        this.rarity = rarity;
        this.isNew = isNew;
        this.gachaTs = gachaTs;
        this.seqId = seqId;
        this.lang = lang;
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

    public Boolean getNew() {
        return isNew;
    }

    public void setNew(Boolean aNew) {
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

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    @Override
    public String toString() {
        return "WeaponPoolRecord{" +
                "id=" + id +
                ", roleId='" + roleId + '\'' +
                ", poolId='" + poolId + '\'' +
                ", poolName='" + poolName + '\'' +
                ", weaponId='" + weaponId + '\'' +
                ", weaponName='" + weaponName + '\'' +
                ", weaponType='" + weaponType + '\'' +
                ", rarity=" + rarity +
                ", isNew=" + isNew +
                ", gachaTs='" + gachaTs + '\'' +
                ", seqId='" + seqId + '\'' +
                ", lang='" + lang + '\'' +
                ", serverId='" + serverId + '\'' +
                '}';
    }
}
