package org.yituliu.entity.dto;

/**
 * 访问日志数据传输对象
 * 用于前端请求访问记录
 */
public class AccessLogDTO {
    private String url;
    private String region;
    private String device;
    private String browser;
    private String os;

    public AccessLogDTO() {
    }

    public AccessLogDTO(String url, String region, String device, String browser, String os) {
        this.url = url;
        this.region = region;
        this.device = device;
        this.browser = browser;
        this.os = os;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    @Override
    public String toString() {
        return "AccessLogDTO{" +
                "url='" + url + '\'' +
                ", region='" + region + '\'' +
                ", device='" + device + '\'' +
                ", browser='" + browser + '\'' +
                ", os='" + os + '\'' +
                '}';
    }
}