package org.yituliu.entity.dto;

import lombok.Data;

@Data
public class AccessLogDTO {
    private String url;
    private String region;
    private String device;
    private String browser;
    private String os;
}
