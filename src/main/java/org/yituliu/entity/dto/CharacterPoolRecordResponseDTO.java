package org.yituliu.entity.dto;

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
    
    /**
     * 默认构造函数
     */
    public CharacterPoolRecordResponseDTO() {
    }
    
    /**
     * 全参构造函数
     */
    public CharacterPoolRecordResponseDTO(Integer code, CharacterPoolRecordDataDTO data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }
    
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
    
    /**
     * 判断响应是否成功
     * @return 成功返回true，失败返回false
     */
    public boolean isSuccess() {
        return code != null && code == 0;
    }
    
    @Override
    public String toString() {
        return "GachaResponseDTO{" +
                "code=" + code +
                ", data=" + data +
                ", msg='" + msg + '\'' +
                '}';
    }
}