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

        System.out.println(accessLog);

        accessLogMapper.insert(accessLog);
    }
}
