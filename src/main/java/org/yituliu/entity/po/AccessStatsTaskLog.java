package org.yituliu.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

/**
 * 访问统计任务实体类
 * 用于记录统计任务的执行信息
 */
@TableName("access_stats_task_log")
public class AccessStatsTaskLog {
    
    /**
     * 主键ID
     */
    @TableId
    private Long id;
    
    /**
     * 统计开始时间
     */
    private Date startTime;
    
    /**
     * 统计结束时间
     */
    private Date endTime;
    
    /**
     * 任务类型
     */
    private String taskType;
    
    /**
     * 创建时间
     */
    private Date createTime;
    
    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 无参构造函数
     */
    public AccessStatsTaskLog() {
    }

    /**
     * 全参构造函数
     * 
     * @param id 主键ID
     * @param startTime 统计开始时间
     * @param endTime 统计结束时间
     * @param taskType 任务类型
     * @param createTime 创建时间
     * @param updateTime 更新时间
     */
    public AccessStatsTaskLog(Long id, Date startTime, Date endTime, String taskType, Date createTime, Date updateTime) {
        this.id = id;
        this.startTime = startTime;
        this.endTime = endTime;
        this.taskType = taskType;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "AccessStatsTask{" +
                "id=" + id +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", taskType='" + taskType + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}