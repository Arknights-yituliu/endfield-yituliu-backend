package org.yituliu.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName("player_pool_record_task")
public class PlayerPoolRecordTask {
    @TableId
    private String taskId;
    private String token;
    private Boolean completeFlag;
    private String roleId;
    private Boolean startFlag;
    private Date createTime;
    private Date updateTime;

    public PlayerPoolRecordTask() {
    }

    public PlayerPoolRecordTask(String taskId, String token, Boolean completeFlag, String roleId, Boolean startFlag, Date createTime, Date updateTime) {
        this.taskId = taskId;
        this.token = token;
        this.completeFlag = completeFlag;
        this.roleId = roleId;
        this.startFlag = startFlag;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Boolean getCompleteFlag() {
        return completeFlag;
    }

    public void setCompleteFlag(Boolean completeFlag) {
        this.completeFlag = completeFlag;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public Boolean getStartFlag() {
        return startFlag;
    }

    public void setStartFlag(Boolean startFlag) {
        this.startFlag = startFlag;
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
        return "PlayerPoolRecordTask{" +
                "taskId='" + taskId + '\'' +
                ", token='" + token + '\'' +
                ", completeFlag=" + completeFlag +
                ", roleId='" + roleId + '\'' +
                ", startFlag=" + startFlag +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
