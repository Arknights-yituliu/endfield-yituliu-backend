package org.yituliu.entity.dto.pool.record;




public class WeaponPoolRecordResponseDTO {
    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应数据
     */
    private WeaponPoolRecordDataDTO data;

    /**
     * 响应消息
     */
    private String msg;

    public WeaponPoolRecordResponseDTO() {
    }

    public WeaponPoolRecordResponseDTO(Integer code, WeaponPoolRecordDataDTO data, String msg) {
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

    public WeaponPoolRecordDataDTO getData() {
        return data;
    }

    public void setData(WeaponPoolRecordDataDTO data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
