package org.yituliu.entity.dto;

/**
 * 单个抽卡记录DTO
 * 对应Endfield抽卡记录API中的单个记录项
 * @author 山桜
 */
public class GachaRecordDTO {
    
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
    public GachaRecordDTO() {
    }
    
    /**
     * 全参构造函数
     */
    public GachaRecordDTO(String poolId, String poolName, String charId, String charName, 
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
    
    /**
     * 获取抽卡时间的格式化字符串
     * @return 格式化后的时间字符串
     */
    public String getFormattedGachaTime() {
        if (gachaTs == null || gachaTs.isEmpty()) {
            return "";
        }
        try {
            long timestamp = Long.parseLong(gachaTs);
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
        } catch (NumberFormatException e) {
            return gachaTs;
        }
    }
    
    /**
     * 获取稀有度对应的星级字符串
     * @return 星级字符串（如：★★★★★）
     */
    public String getRarityStars() {
        if (rarity == null) {
            return "";
        }
        StringBuilder stars = new StringBuilder();
        for (int i = 0; i < rarity; i++) {
            stars.append("★");
        }
        return stars.toString();
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