package org.yituliu.entity.vo;

import java.util.List;

/**
 * URL分组访问量统计VO
 * 每个URL包含其所有时间段的访问次数明细（同时含小时和天级别数据）
 */
public class UrlVisitGroupVO {
    private String url;
    private List<UrlPeriodDataVO> data;

    public UrlVisitGroupVO() {
    }

    public UrlVisitGroupVO(String url, List<UrlPeriodDataVO> data) {
        this.url = url;
        this.data = data;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<UrlPeriodDataVO> getData() {
        return data;
    }

    public void setData(List<UrlPeriodDataVO> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "UrlVisitGroupVO{" +
                "url='" + url + '\'' +
                ", data=" + data +
                '}';
    }
}
