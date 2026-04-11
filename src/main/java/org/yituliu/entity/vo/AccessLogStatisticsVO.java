package org.yituliu.entity.vo;



import java.util.Map;


public class AccessLogStatisticsVO {
    private Long totalVisits;
    private Long uniqueVisitors;
    private Long pageViews;
    private Map<String, Long> visitsByRegion;
    private Map<String, Long> visitsByDevice;
    private Map<String, Long> visitsByBrowser;
    private Map<String, Long> visitsByOs;
    private Map<String, Long> visitsByUrl;
    private Map<String, Long> visitsByHour;
    private Map<String, Long> visitsByDay;

    public AccessLogStatisticsVO() {
    }

    public AccessLogStatisticsVO(Long totalVisits, Long uniqueVisitors, Long pageViews, 
                                   Map<String, Long> visitsByRegion, Map<String, Long> visitsByDevice,
                                   Map<String, Long> visitsByBrowser, Map<String, Long> visitsByOs,
                                   Map<String, Long> visitsByUrl, Map<String, Long> visitsByHour,
                                   Map<String, Long> visitsByDay) {
        this.totalVisits = totalVisits;
        this.uniqueVisitors = uniqueVisitors;
        this.pageViews = pageViews;
        this.visitsByRegion = visitsByRegion;
        this.visitsByDevice = visitsByDevice;
        this.visitsByBrowser = visitsByBrowser;
        this.visitsByOs = visitsByOs;
        this.visitsByUrl = visitsByUrl;
        this.visitsByHour = visitsByHour;
        this.visitsByDay = visitsByDay;
    }

    public Long getTotalVisits() {
        return totalVisits;
    }

    public void setTotalVisits(Long totalVisits) {
        this.totalVisits = totalVisits;
    }

    public Long getUniqueVisitors() {
        return uniqueVisitors;
    }

    public void setUniqueVisitors(Long uniqueVisitors) {
        this.uniqueVisitors = uniqueVisitors;
    }

    public Long getPageViews() {
        return pageViews;
    }

    public void setPageViews(Long pageViews) {
        this.pageViews = pageViews;
    }

    public Map<String, Long> getVisitsByRegion() {
        return visitsByRegion;
    }

    public void setVisitsByRegion(Map<String, Long> visitsByRegion) {
        this.visitsByRegion = visitsByRegion;
    }

    public Map<String, Long> getVisitsByDevice() {
        return visitsByDevice;
    }

    public void setVisitsByDevice(Map<String, Long> visitsByDevice) {
        this.visitsByDevice = visitsByDevice;
    }

    public Map<String, Long> getVisitsByBrowser() {
        return visitsByBrowser;
    }

    public void setVisitsByBrowser(Map<String, Long> visitsByBrowser) {
        this.visitsByBrowser = visitsByBrowser;
    }

    public Map<String, Long> getVisitsByOs() {
        return visitsByOs;
    }

    public void setVisitsByOs(Map<String, Long> visitsByOs) {
        this.visitsByOs = visitsByOs;
    }

    public Map<String, Long> getVisitsByUrl() {
        return visitsByUrl;
    }

    public void setVisitsByUrl(Map<String, Long> visitsByUrl) {
        this.visitsByUrl = visitsByUrl;
    }

    public Map<String, Long> getVisitsByHour() {
        return visitsByHour;
    }

    public void setVisitsByHour(Map<String, Long> visitsByHour) {
        this.visitsByHour = visitsByHour;
    }

    public Map<String, Long> getVisitsByDay() {
        return visitsByDay;
    }

    public void setVisitsByDay(Map<String, Long> visitsByDay) {
        this.visitsByDay = visitsByDay;
    }

    @Override
    public String toString() {
        return "AccessLogStatisticsVO{" +
                "totalVisits=" + totalVisits +
                ", uniqueVisitors=" + uniqueVisitors +
                ", pageViews=" + pageViews +
                ", visitsByRegion=" + visitsByRegion +
                ", visitsByDevice=" + visitsByDevice +
                ", visitsByBrowser=" + visitsByBrowser +
                ", visitsByOs=" + visitsByOs +
                ", visitsByUrl=" + visitsByUrl +
                ", visitsByHour=" + visitsByHour +
                ", visitsByDay=" + visitsByDay +
                '}';
    }
}
