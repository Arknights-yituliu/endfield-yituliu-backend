package org.yituliu.entity.dto.pool.record.response;

/**
 * 抽卡API响应DTO
 * 对应Endfield抽卡记录API的响应结构
 * @author 山桜
 */
public class CharacterPoolRecordResponseWapperDTO {

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应数据
     */
    private CharacterPoolRecordResponseDTO data;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 默认构造函数
     */
    public CharacterPoolRecordResponseWapperDTO() {
    }

    public CharacterPoolRecordResponseWapperDTO(Integer code, CharacterPoolRecordResponseDTO data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public CharacterPoolRecordResponseDTO getData() {
        return data;
    }

    public void setData(CharacterPoolRecordResponseDTO data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "CharacterPoolRecordResponseWapperDTO{" +
                "code=" + code +
                ", data=" + data +
                ", msg='" + msg + '\'' +
                '}';
    }
}