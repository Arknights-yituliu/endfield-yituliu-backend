-- AccessLog表建表语句
-- 访问日志表，用于记录用户访问信息

CREATE TABLE access_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    url VARCHAR(500) NOT NULL COMMENT '访问URL',
    ip VARCHAR(45) NOT NULL COMMENT '客户端IP地址',
    region VARCHAR(100) DEFAULT 'Unknown' COMMENT '访问地区',
    referer VARCHAR(500) DEFAULT 'Unknown' COMMENT '来源页面',
    device VARCHAR(50) DEFAULT 'Unknown' COMMENT '设备类型',
    browser VARCHAR(50) DEFAULT 'Unknown' COMMENT '浏览器信息',
    os VARCHAR(50) DEFAULT 'Unknown' COMMENT '操作系统',
    user_agent TEXT COMMENT '完整User-Agent',
    access_time DATETIME NOT NULL COMMENT '访问时间',
    
    -- 自动维护字段
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引定义
    INDEX idx_access_time_ip (access_time, ip) COMMENT '时间+IP复合索引，优化PV/UV统计',
    INDEX idx_ip (ip) COMMENT 'IP索引，优化按IP查询',
    INDEX idx_access_time (access_time) COMMENT '时间索引，优化时间范围查询',
    INDEX idx_url (url(100)) COMMENT 'URL索引（前缀索引）',
    INDEX idx_device (device) COMMENT '设备索引',
    INDEX idx_browser (browser) COMMENT '浏览器索引',
    INDEX idx_os (os) COMMENT '操作系统索引',
    INDEX idx_region (region) COMMENT '地区索引',
    INDEX idx_created_time (created_time) COMMENT '创建时间索引'
) 
ENGINE=InnoDB 
DEFAULT CHARSET=utf8mb4 
COLLATE=utf8mb4_unicode_ci 
COMMENT='访问日志表，记录用户访问信息用于统计分析';

-- 分区表建议（如果数据量巨大，千万级以上）
-- 按年分区，便于数据管理和查询优化
-- CREATE TABLE access_log (
--     ... 字段定义同上 ...
-- ) 
-- PARTITION BY RANGE (YEAR(access_time)) (
--     PARTITION p2024 VALUES LESS THAN (2025),
--     PARTITION p2025 VALUES LESS THAN (2026),
--     PARTITION p2026 VALUES LESS THAN (2027),
--     PARTITION p_future VALUES LESS THAN MAXVALUE
-- );

-- 表注释和字段注释说明
ALTER TABLE access_log COMMENT = '访问日志表 - 用户行为分析数据源';

-- 验证表结构
DESC access_log;

-- 查看表索引
SHOW INDEX FROM access_log;

-- 示例数据插入
INSERT INTO access_log (url, ip, region, referer, device, browser, os, user_agent, access_time) 
VALUES 
('/home', '192.168.1.100', '北京', 'https://www.google.com', 'PC', 'Chrome', 'Windows', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36', '2024-01-15 10:00:00'),
('/product/123', '192.168.1.101', '上海', 'https://www.baidu.com', 'Mobile', 'Safari', 'iOS', 'Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)', '2024-01-15 10:01:00'),
('/about', '192.168.1.102', '广州', 'Direct', 'Tablet', 'Firefox', 'Android', 'Mozilla/5.0 (Android; Tablet; rv:85.0) Gecko/85.0', '2024-01-15 10:02:00');

-- 统计查询测试
-- PV统计（页面浏览量）
SELECT COUNT(*) as page_views 
FROM access_log 
WHERE access_time >= '2024-01-15 00:00:00' 
  AND access_time <= '2024-01-15 23:59:59';

-- UV统计（独立访客数）
SELECT COUNT(DISTINCT ip) as unique_visitors 
FROM access_log 
WHERE access_time >= '2024-01-15 00:00:00' 
  AND access_time <= '2024-01-15 23:59:59';

-- 按设备统计
SELECT device, COUNT(*) as count 
FROM access_log 
WHERE access_time >= '2024-01-15 00:00:00' 
GROUP BY device 
ORDER BY count DESC;

-- 按浏览器统计
SELECT browser, COUNT(*) as count 
FROM access_log 
WHERE access_time >= '2024-01-15 00:00:00' 
GROUP BY browser 
ORDER BY count DESC;

-- 性能优化建议
-- 1. 定期清理过期数据（保留最近N天）
-- DELETE FROM access_log WHERE access_time < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- 2. 定期优化表
-- OPTIMIZE TABLE access_log;

-- 3. 监控表大小和索引使用
-- SELECT 
--     table_name,
--     table_rows,
--     data_length,
--     index_length,
--     round(((data_length + index_length) / 1024 / 1024), 2) as total_size_mb
-- FROM information_schema.tables 
-- WHERE table_name = 'access_log';