package org.yituliu.entity.dto.pool.record;


public class EndfieldUserInfoDTO {
    private String u8Token;
    private String nickName;
    private String uid;
    private String roleId;

    public EndfieldUserInfoDTO() {
    }

    public EndfieldUserInfoDTO(String u8Token, String nickName, String uid, String roleId) {
        this.u8Token = u8Token;
        this.nickName = nickName;
        this.uid = uid;
        this.roleId = roleId;
    }

    public String getU8Token() {
        return u8Token;
    }

    public void setU8Token(String u8Token) {
        this.u8Token = u8Token;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
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

    @Override
    public String toString() {
        return "EndfieldUserInfoDTO{" +
                "u8Token='" + u8Token + '\'' +
                ", nickName='" + nickName + '\'' +
                ", uid='" + uid + '\'' +
                ", roleId='" + roleId + '\'' +
                '}';
    }
}