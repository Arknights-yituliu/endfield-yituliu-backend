package org.yituliu.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;


import java.util.Date;

@TableName("access_log")
public class AccessLog {
    @TableId("id")
    private Long id;
    private String url;
    private String ip;
    private String region;
    private String referer;
    private String device;
    private String browser;
    private String os;
    private String userAgent;
    private Date accessTime;

    public AccessLog() {
    }

    public AccessLog(Long id, String url, String ip, String region, String referer, String device, String browser, String os, String userAgent, Date accessTime) {
        this.id = id;
        this.url = url;
        this.ip = ip;
        this.region = region;
        this.referer = referer;
        this.device = device;
        this.browser = browser;
        this.os = os;
        this.userAgent = userAgent;
        this.accessTime = accessTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
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

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Date getAccessTime() {
        return accessTime;
    }

    public void setAccessTime(Date accessTime) {
        this.accessTime = accessTime;
    }

    @Override
    public String toString() {
        return "AccessLog{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", ip='" + ip + '\'' +
                ", region='" + region + '\'' +
                ", referer='" + referer + '\'' +
                ", device='" + device + '\'' +
                ", browser='" + browser + '\'' +
                ", os='" + os + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", accessTime=" + accessTime +
                '}';
    }
}
