package org.yituliu.entity.vo;

/**
 * URL每小时访问量统计VO
 * 用于返回每个URL在每个小时内的访问次数
 */
public class UrlHourlyVisitVO {
    private String url;
    private String hour;
    private Long count;

    public UrlHourlyVisitVO() {
    }

    public UrlHourlyVisitVO(String url, String hour, Long count) {
        this.url = url;
        this.hour = hour;
        this.count = count;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHour() {
        return hour;
    }

    public void setHour(String hour) {
        this.hour = hour;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "UrlHourlyVisitVO{" +
                "url='" + url + '\'' +
                ", hour='" + hour + '\'' +
                ", count=" + count +
                '}';
    }
}
