-- CharacterPoolRecord表索引优化脚本
-- 用于CharacterPoolRecordServiceV3的数据库层面优化
-- 创建时间: 2025-12-30

-- 1. 创建唯一索引，防止重复插入
-- 组合索引：(uid, pool_name, seq_id) 确保同一用户的同一池子的同一序列ID不会重复
ALTER TABLE character_pool_record 
ADD UNIQUE INDEX uk_uid_pool_seq (uid, pool_name, seq_id);

-- 2. 添加常用查询索引，提升查询性能
-- 按用户ID查询的索引
ALTER TABLE character_pool_record 
ADD INDEX idx_uid (uid);

-- 3. 按卡池名称和用户ID的复合索引
ALTER TABLE character_pool_record 
ADD INDEX idx_uid_pool_name (uid, pool_name);

-- 4. 按稀有度和用户ID的索引（如果需要按稀有度统计）
ALTER TABLE character_pool_record 
ADD INDEX idx_uid_rarity (uid, rarity);

-- 5. 按时间戳排序的索引（如果需要按时间查询）
ALTER TABLE character_pool_record 
ADD INDEX idx_uid_gacha_ts (uid, gacha_ts);

-- 6. 查看创建的索引
SHOW INDEX FROM character_pool_record;

-- 7. 分析表结构，确保索引正确
ANALYZE TABLE character_pool_record;

-- 8. 如果需要删除现有重复数据（谨慎操作）
-- 首先查看重复数据
-- SELECT uid, pool_name, seq_id, COUNT(*) as cnt 
-- FROM character_pool_record 
-- GROUP BY uid, pool_name, seq_id 
-- HAVING COUNT(*) > 1;

-- 9. 如果确认需要清理重复数据，可以执行以下SQL（谨慎操作）
-- DELETE t1 FROM character_pool_record t1
-- INNER JOIN character_pool_record t2 
-- WHERE t1.id > t2.id 
-- AND t1.uid = t2.uid 
-- AND t1.pool_name = t2.pool_name 
-- AND t1.seq_id = t2.seq_id;

-- 10. 验证索引是否正常工作
-- EXPLAIN SELECT * FROM character_pool_record 
-- WHERE uid = 'test_user' AND pool_name = 'test_pool' AND seq_id = 'test_seq';

-- 11. 检查索引使用情况
-- SHOW STATUS LIKE 'Handler_read%';

-- 12. 如果需要查看重复键插入时的具体错误信息
-- 可以执行以下测试插入（确保有重复数据时）
-- INSERT INTO character_pool_record (uid, pool_name, seq_id, char_name, rarity, is_free, is_new, gacha_ts, lang, pool_type, server_id) 
-- VALUES ('test_user', 'test_pool', 'test_seq', 'test_char', 5, false, false, '1234567890', 'zh-cn', 'E_CharacterGachaPoolType_Special', 'test_server');

-- 优化建议：
-- 1. 确保character_pool_record表使用InnoDB引擎以支持事务和外键
-- 2. 如果数据量很大，考虑定期归档历史数据
-- 3. 监控索引使用情况，删除不常用的索引
-- 4. 根据实际查询模式调整索引策略

-- 完成提示
SELECT 'CharacterPoolRecord索引优化完成!' as status;