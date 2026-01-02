package org.yituliu.entity.dto.pool.record.response;

/**
 * 抽卡API响应DTO
 * 对应Endfield抽卡记录API的响应结构
 * @author 山桜
 */
public class CharacterPoolRecordResponseDTO {
    
    /**
     * 响应码
     */
    private Integer code;
    
    /**
     * 响应数据
     */
    private CharacterPoolRecordDataDTO data;
    
    /**
     * 响应消息
     */
    private String msg;

    private String poolType;
    
    /**
     * 默认构造函数
     */
    public CharacterPoolRecordResponseDTO() {
    }

    public CharacterPoolRecordResponseDTO(Integer code, CharacterPoolRecordDataDTO data, String msg, String poolType) {
        this.code = code;
        this.data = data;
        this.msg = msg;
        this.poolType = poolType;
    }

    /**
     * 全参构造函数
     */

    
    // Getter和Setter方法
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public CharacterPoolRecordDataDTO getData() {
        return data;
    }

    public void setData(CharacterPoolRecordDataDTO data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getPoolType() {
        return poolType;
    }

    public void setPoolType(String poolType) {
        this.poolType = poolType;
    }

    @Override
    public String toString() {
        return "CharacterPoolRecordResponseDTO{" +
                "code=" + code +
                ", data=" + data +
                ", msg='" + msg + '\'' +
                ", poolType='" + poolType + '\'' +
                '}';
    }
}