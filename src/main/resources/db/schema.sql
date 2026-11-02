-- =============================================================
-- 访问统计建表脚本
-- 数据库：MySQL 8.x，字符集 utf8mb4，排序规则 utf8mb4_general_ci
-- 说明：脚本使用 CREATE TABLE IF NOT EXISTS，可在空库直接执行
-- =============================================================

CREATE DATABASE IF NOT EXISTS `endfield`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- CREATE DATABASE IF NOT EXISTS 对已存在的库不生效，故此处再强制对齐一次，
-- 避免已有库沿用历史排序规则导致字符串比较报 1267 非法混用
ALTER DATABASE `endfield`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `endfield`;

-- -------------------------------------------------------------
-- 访问日志明细表
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `access_log` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `url` VARCHAR(500) NOT NULL COMMENT '访问URL',
    `ip` VARCHAR(45) NOT NULL COMMENT '客户端IP地址',
    `region` VARCHAR(100) DEFAULT 'Unknown' COMMENT '访问地区',
    `referer` VARCHAR(500) DEFAULT 'Unknown' COMMENT '来源页面',
    `device` VARCHAR(50) DEFAULT 'Unknown' COMMENT '设备类型',
    `browser` VARCHAR(50) DEFAULT 'Unknown' COMMENT '浏览器信息',
    `os` VARCHAR(50) DEFAULT 'Unknown' COMMENT '操作系统',
    `user_agent` TEXT COMMENT '完整User-Agent',
    `access_time` DATETIME NOT NULL COMMENT '访问时间',
    PRIMARY KEY (`id`),
    INDEX `idx_access_time_ip` (`access_time`, `ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='访问日志表，记录用户访问信息用于统计分析';

-- 每小时访问量统计表
CREATE TABLE IF NOT EXISTS `access_log_hourly_stats` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `stat_hour` DATETIME NOT NULL COMMENT '统计的小时（整点）',
    `visit_count` BIGINT NOT NULL DEFAULT 0 COMMENT '该小时总访问量',
    `task_id` BIGINT NOT NULL COMMENT '统计任务ID',
    `record_code` INT NOT NULL DEFAULT 1 COMMENT '记录状态：1=展示，-1=过期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_stat_hour_code` (`stat_hour`, `record_code`),
    KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每小时访问量统计表';

-- 小时访问量统计任务记录表
CREATE TABLE IF NOT EXISTS `access_log_hourly_stats_task` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `stat_hour` DATETIME NOT NULL COMMENT '统计的小时（整点）',
    `task_id` BIGINT NOT NULL COMMENT '该小时当前有效的任务ID',
    `data_count` BIGINT NOT NULL DEFAULT 0 COMMENT '该小时访问量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近一次重新统计时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stat_hour` (`stat_hour`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='小时访问量统计任务记录表';

-- 每个URL每日访问量统计表
CREATE TABLE IF NOT EXISTS `access_log_url_daily_stats` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `url` VARCHAR(500) DEFAULT NULL COMMENT 'URL（已去除结尾斜杠）',
    `stat_day` DATETIME NOT NULL COMMENT '统计日期（当天 00:00:00）',
    `visit_count` BIGINT NOT NULL DEFAULT 0 COMMENT '该URL当天访问量',
    `task_id` BIGINT NOT NULL COMMENT '统计任务ID',
    `record_code` INT NOT NULL DEFAULT 1 COMMENT '记录状态：1=展示，-1=过期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_stat_day_code` (`stat_day`, `record_code`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_url` (`url`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='每个URL每日访问量统计表';

-- URL每日访问量统计任务记录表
CREATE TABLE IF NOT EXISTS `access_log_url_daily_stats_task` (
    `id` BIGINT NOT NULL COMMENT '主键，雪花ID',
    `stat_day` DATETIME NOT NULL COMMENT '统计日期（当天 00:00:00）',
    `task_id` BIGINT NOT NULL COMMENT '该日当前有效的任务ID',
    `data_count` BIGINT NOT NULL DEFAULT 0 COMMENT '该日统计的URL数量',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近一次重新统计时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stat_day` (`stat_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='URL每日访问量统计任务记录表';
