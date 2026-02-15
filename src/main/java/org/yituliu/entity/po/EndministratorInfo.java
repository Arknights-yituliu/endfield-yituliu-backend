package org.yituliu.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;


@TableName
public class EndministratorInfo {
    @TableId
    private String roleId;
    private String uid;

    private String nickName;

    public EndministratorInfo() {
    }

    public EndministratorInfo(String uid, String roleId, String nickName) {
        this.uid = uid;
        this.roleId = roleId;
        this.nickName = nickName;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
}
