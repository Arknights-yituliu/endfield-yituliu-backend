-- CharacterPoolRecordServiceV3 性能测试和验证脚本
-- 用于验证基于数据库索引的优化效果

-- 1. 测试数据准备
-- 创建测试用户数据
INSERT INTO character_pool_record (id, uid, pool_id, pool_name, char_id, char_name, rarity, is_free, is_new, gacha_ts, seq_id, lang, pool_type, server_id) 
VALUES 
(1, 'test_user_1', 'pool_001', '新手池', 'char_001', '测试角色1', 5, false, true, '1640995200', 'seq_001', 'zh-cn', 'E_CharacterGachaPoolType_Beginner', 'server_001'),
(2, 'test_user_1', 'pool_002', '标准池', 'char_002', '测试角色2', 4, false, false, '1640995300', 'seq_002', 'zh-cn', 'E_CharacterGachaPoolType_Standard', 'server_001'),
(3, 'test_user_2', 'pool_001', '新手池', 'char_003', '测试角色3', 3, true, false, '1640995400', 'seq_003', 'zh-cn', 'E_CharacterGachaPoolType_Beginner', 'server_001');

-- 2. 验证唯一索引是否生效
-- 这条插入应该成功
INSERT INTO character_pool_record (id, uid, pool_id, pool_name, char_id, char_name, rarity, is_free, is_new, gacha_ts, seq_id, lang, pool_type, server_id) 
VALUES (4, 'test_user_3', 'pool_003', '特殊池', 'char_004', '测试角色4', 5, false, true, '1640995500', 'seq_004', 'zh-cn', 'E_CharacterGachaPoolType_Special', 'server_001');

-- 这条插入应该失败（重复的uid+pool_name+seq_id）
INSERT INTO character_pool_record (id, uid, pool_id, pool_name, char_id, char_name, rarity, is_free, is_new, gacha_ts, seq_id, lang, pool_type, server_id) 
VALUES (5, 'test_user_1', 'pool_001', '新手池', 'char_005', '测试角色5', 5, false, true, '1640995600', 'seq_001', 'zh-cn', 'E_CharacterGachaPoolType_Beginner', 'server_001');

-- 3. 性能测试查询
-- 查看查询执行计划
EXPLAIN SELECT * FROM character_pool_record WHERE uid = 'test_user_1';

-- 查看复合索引使用情况
EXPLAIN SELECT * FROM character_pool_record WHERE uid = 'test_user_1' AND pool_name = '新手池';

-- 查看唯一索引使用情况
EXPLAIN SELECT * FROM character_pool_record WHERE uid = 'test_user_1' AND pool_name = '新手池' AND seq_id = 'seq_001';

-- 4. 批量插入性能测试
-- 准备大量测试数据（1000条）
INSERT INTO character_pool_record (id, uid, pool_id, pool_name, char_id, char_name, rarity, is_free, is_new, gacha_ts, seq_id, lang, pool_type, server_id)
SELECT 
    1000 + n,
    CONCAT('perf_test_user_', FLOOR(n / 100)),
    CONCAT('pool_', FLOOR(n / 50) % 10),
    CASE WHEN n % 3 = 0 THEN '新手池' WHEN n % 3 = 1 THEN '标准池' ELSE '特殊池' END,
    CONCAT('char_', LPAD(n, 3, '0')),
    CONCAT('性能测试角色', n),
    (n % 5) + 1,
    (n % 2) = 0,
    (n % 10) = 0,
    1640995200 + n,
    CONCAT('seq_', LPAD(n, 4, '0')),
    'zh-cn',
    CASE WHEN n % 3 = 0 THEN 'E_CharacterGachaPoolType_Beginner' WHEN n % 3 = 1 THEN 'E_CharacterGachaPoolType_Standard' ELSE 'E_CharacterGachaPoolType_Special' END,
    'server_001'
FROM (
    SELECT a.N + b.N * 10 + c.N * 100 AS n
    FROM 
        (SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 
         UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a,
        (SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 
         UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b,
        (SELECT 0 AS N UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 
         UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c
) numbers
WHERE n < 1000;

-- 5. 查询性能测试
-- 统计查询时间
SELECT '查询性能测试开始' as test_step;

-- 测试1：按用户查询
SELECT COUNT(*) as user_count FROM character_pool_record WHERE uid = 'perf_test_user_1';

-- 测试2：按用户和池子查询
SELECT COUNT(*) as pool_count FROM character_pool_record WHERE uid = 'perf_test_user_1' AND pool_name = '新手池';

-- 测试3：精确查找
SELECT * FROM character_pool_record WHERE uid = 'perf_test_user_1' AND pool_name = '新手池' AND seq_id = 'seq_0001' LIMIT 1;

-- 6. 索引使用情况检查
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    NON_UNIQUE,
    SEQ_IN_INDEX,
    COLUMN_NAME,
    CARDINALITY
FROM INFORMATION_SCHEMA.STATISTICS 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'character_pool_record'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- 7. 表统计信息
SELECT 
    TABLE_NAME,
    TABLE_ROWS,
    DATA_LENGTH,
    INDEX_LENGTH,
    (DATA_LENGTH + INDEX_LENGTH) as TOTAL_LENGTH
FROM INFORMATION_SCHEMA.TABLES 
WHERE TABLE_SCHEMA = DATABASE() 
AND TABLE_NAME = 'character_pool_record';

-- 8. 重复数据检查
SELECT 
    uid,
    pool_name,
    seq_id,
    COUNT(*) as duplicate_count
FROM character_pool_record 
GROUP BY uid, pool_name, seq_id
HAVING COUNT(*) > 1;

-- 9. 清理测试数据
DELETE FROM character_pool_record WHERE uid LIKE 'perf_test_user_%';
DELETE FROM character_pool_record WHERE uid IN ('test_user_1', 'test_user_2', 'test_user_3');

-- 10. 最终验证
SELECT '测试完成 - CharacterPoolRecordServiceV3 优化验证' as final_status;

-- 显示优化后的表结构
SHOW CREATE TABLE character_pool_record;