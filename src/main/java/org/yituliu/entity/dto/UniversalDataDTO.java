package org.yituliu.entity.dto;

/**
 * 通用数据传输对象
 * 用于前端请求和响应
 */
public class UniversalDataDTO {
    private String data;
    private String version;
    private String source;
    private String note;

    public UniversalDataDTO() {
    }

    public UniversalDataDTO(String data, String version, String source, String note) {
        this.data = data;
        this.version = version;
        this.source = source;
        this.note = note;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return "UniversalDataDTO{" +
                "data='" + data + '\'' +
                ", version='" + version + '\'' +
                ", source='" + source + '\'' +
                ", note='" + note + '\'' +
                '}';
    }
}