package org.yituliu.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * 流量统计实体类
 * 用于记录页面浏览量和独立访客数等流量指标
 * 
 * @author yituliu
 * @version 1.0
 * @since 2024
 */
@TableName("traffic_stats")
public class TrafficStats {
    
    /**
     * 主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 页面浏览量 (Page Views)
     */
    private Long pageViews;
    
    /**
     * 独立访客数 (Unique Visitors)
     */
    private Long uniqueVisitors;
    
    /**
     * 统计开始时间
     */
    private Date statStartTime;
    
    /**
     * 统计结束时间
     */
    private Date statEndTime;
    
    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 无参构造函数
     */
    public TrafficStats() {
    }

    /**
     * 全参构造函数
     * 
     * @param id 主键ID
     * @param pageViews 页面浏览量
     * @param uniqueVisitors 独立访客数
     * @param statStartTime 统计开始时间
     * @param statEndTime 统计结束时间
     * @param createTime 创建时间
     */
    public TrafficStats(Long id, Long pageViews, Long uniqueVisitors, Date statStartTime, Date statEndTime, Date createTime) {
        this.id = id;
        this.pageViews = pageViews;
        this.uniqueVisitors = uniqueVisitors;
        this.statStartTime = statStartTime;
        this.statEndTime = statEndTime;
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPageViews() {
        return pageViews;
    }

    public void setPageViews(Long pageViews) {
        this.pageViews = pageViews;
    }

    public Long getUniqueVisitors() {
        return uniqueVisitors;
    }

    public void setUniqueVisitors(Long uniqueVisitors) {
        this.uniqueVisitors = uniqueVisitors;
    }

    public Date getStatStartTime() {
        return statStartTime;
    }

    public void setStatStartTime(Date statStartTime) {
        this.statStartTime = statStartTime;
    }

    public Date getStatEndTime() {
        return statEndTime;
    }

    public void setStatEndTime(Date statEndTime) {
        this.statEndTime = statEndTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "TrafficStats{" +
                "id=" + id +
                ", pageViews=" + pageViews +
                ", uniqueVisitors=" + uniqueVisitors +
                ", statStartTime=" + statStartTime +
                ", statEndTime=" + statEndTime +
                ", createTime=" + createTime +
                '}';
    }
}