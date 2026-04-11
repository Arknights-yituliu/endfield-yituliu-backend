package org.yituliu.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.util.Date;

@TableName("universal_data")
public class UniversalData {
    @TableId
    private Long id;
    private String data;
    private String version;
    private String source;
    private Date createDate;
    private String note;

    public UniversalData() {
    }

    public UniversalData(Long id, String data, String version, String source, Date createDate, String note) {
        this.id = id;
        this.data = data;
        this.version = version;
        this.source = source;
        this.createDate = createDate;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return "UniversalData{" +
                "id=" + id +
                ", data='" + data + '\'' +
                ", version='" + version + '\'' +
                ", source='" + source + '\'' +
                ", createDate=" + createDate +
                ", note='" + note + '\'' +
                '}';
    }
}