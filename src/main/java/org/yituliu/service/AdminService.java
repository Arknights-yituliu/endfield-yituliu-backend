package org.yituliu.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.yituliu.common.utils.IdGenerator;
import org.yituliu.common.utils.IpUtil;
import org.yituliu.common.utils.UserAgentUtil;
import org.yituliu.entity.dto.AccessLogDTO;
import org.yituliu.entity.po.AccessLog;
import org.yituliu.entity.po.TrafficStats;
import org.yituliu.mapper.AccessLogMapper;
import org.yituliu.mapper.TrafficStatsMapper;

import java.util.Calendar;
import java.util.Date;

@Service
public class AdminService {

    private final AccessLogMapper accessLogMapper;
    private final TrafficStatsMapper trafficStatsMapper;
    private final IdGenerator idGenerator;

    public AdminService(AccessLogMapper accessLogMapper, TrafficStatsMapper trafficStatsMapper) {
        this.accessLogMapper = accessLogMapper;
        this.trafficStatsMapper = trafficStatsMapper;
        this.idGenerator = new IdGenerator(1L);
    }

    /**
     * 保存访问日志
     * 
     * @param request   HTTP请求对象
     * @param accessLog 访问日志对象
     */
    public void saveAccessLog(HttpServletRequest request, AccessLogDTO accessLogDTO) {
        AccessLog accessLog = new AccessLog();

        if (accessLog.getId() == null) {
            accessLog.setId(idGenerator.nextId());
        }

        accessLog.setReferer(request.getHeader("Referer"));
        if (accessLog.getReferer() == null) {
            accessLog.setReferer("Unknown");
        }

        accessLog.setUrl(accessLogDTO.getUrl());
        if(accessLogDTO.getUrl() == null){
            accessLogDTO.setUrl("Empty");
        }

        if (accessLog.getAccessTime() == null) {
            accessLog.setAccessTime(new Date());
        }

        accessLog.setIp(IpUtil.getIpAddress(request));

        accessLog.setBrowser(UserAgentUtil.getBrowser(request));

        accessLog.setOs(UserAgentUtil.getOs(request));

        accessLog.setDevice(UserAgentUtil.getDevice(request));

        if("Unknown".equals(accessLog.getBrowser())||"Unknown".equals(accessLog.getOs())||"Unknown".equals(accessLog.getDevice())){
            accessLog.setUserAgent(UserAgentUtil.getUserAgent(request));
        }

        if (accessLog.getRegion() == null) {
            accessLog.setRegion("Unknown");
        }
        accessLogMapper.insert(accessLog);
    }

    /**
     * 统计1小时内的页面浏览量和独立访客数
     * 
     * @param startTime 统计开始时间
     * @return TrafficStats 流量统计结果
     */
    public TrafficStats calculateHourTrafficStats(Date startTime) {
        // 计算结束时间（开始时间 + 1小时）
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startTime);
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        Date endTime = calendar.getTime();
        
        // 统计页面浏览量(PV)
        Long pageViews = accessLogMapper.countPageViews(startTime, endTime);
        
        // 统计独立访客数(UV)
        Long uniqueVisitors = accessLogMapper.countUniqueVisitors(startTime, endTime);
        
        // 创建流量统计对象
        TrafficStats trafficStats = new TrafficStats();
        trafficStats.setId(idGenerator.nextId());
        trafficStats.setPageViews(pageViews != null ? pageViews : 0L);
        trafficStats.setUniqueVisitors(uniqueVisitors != null ? uniqueVisitors : 0L);
        trafficStats.setStatStartTime(startTime);
        trafficStats.setStatEndTime(endTime);
        trafficStats.setCreateTime(new Date());
        
        // 保存统计结果到数据库
        trafficStatsMapper.insert(trafficStats);
        
        return trafficStats;
    }

   

}
