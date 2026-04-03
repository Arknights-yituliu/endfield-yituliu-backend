package org.yituliu.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.yituliu.common.utils.IdGenerator;
import org.yituliu.common.utils.IpUtil;
import org.yituliu.common.utils.UserAgentUtil;
import org.yituliu.entity.dto.AccessLogDTO;
import org.yituliu.entity.po.AccessLog;
import org.yituliu.mapper.AccessLogMapper;

import java.util.Date;

@Service
public class AdminService {

    private final AccessLogMapper accessLogMapper;
    private final IdGenerator idGenerator;

    public AdminService(AccessLogMapper accessLogMapper) {
        this.accessLogMapper = accessLogMapper;
        this.idGenerator = new IdGenerator(1L);
    }

    /**
     * 保存访问日志
     * @param request HTTP请求对象
     * @param accessLog 访问日志对象
     */
    public void saveAccessLog(HttpServletRequest request, AccessLogDTO accessLogDTO) {
        AccessLog accessLog = new AccessLog();
        
        if (accessLog.getId() == null) {
            accessLog.setId(idGenerator.nextId());
        }

        accessLog.setUrl(accessLogDTO.getUrl());

        if (accessLog.getAccessTime() == null) {
            accessLog.setAccessTime(new Date());
        }
        
        accessLog.setIp(IpUtil.getIpAddress(request));
        
        if (accessLog.getBrowser() == null) {
            accessLog.setBrowser(UserAgentUtil.getBrowser(request));
        }
        
        if (accessLog.getOs() == null) {
            accessLog.setOs(UserAgentUtil.getOs(request));
        }
        
        if (accessLog.getDevice() == null) {
            accessLog.setDevice(UserAgentUtil.getDevice(request));
        }
        
        if (accessLog.getRegion() == null) {
            accessLog.setRegion("Unknown");
        }
        
        accessLogMapper.insert(accessLog);
    }
}
