package org.yituliu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yituliu.common.enums.ResultCode;
import org.yituliu.common.exception.ServiceException;
import org.yituliu.common.utils.IdGenerator;
import org.yituliu.entity.dto.CharacterPoolRecordDTO;
import org.yituliu.entity.dto.CharacterPoolRecordResponseDTO;
import org.yituliu.entity.po.CharacterPoolRecord;
import org.yituliu.mapper.CharacterPoolRecordMapper;

import org.yituliu.util.OkHttpUtil;
import org.yituliu.util.UrlParser;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.yituliu.util.UrlEncodeUtil.smartUrlEncode;

/**
 * CharacterPoolRecordService V3 - 基于数据库索引的优化版本
 * 
 * 主要优化：
 * 1. 移除内存中的重复检查逻辑，利用数据库唯一索引
 * 2. 简化代码逻辑，提高可维护性
 * 3. 保留异步并发处理能力
 * 4. 优雅处理唯一约束违反错误
 * 
 * @author 山桜
 */
@Service
public class CharacterPoolRecordServiceV3 {

    private static final Logger logger = LoggerFactory.getLogger(CharacterPoolRecordServiceV3.class);

    /**
     * 卡池类型常量集合
     * 包含三种卡池类型，静态初始化确保只存在一个实例
     */
    private static final Set<String> POOL_TYPES = Set.of(
            "E_CharacterGachaPoolType_Special",
            "E_CharacterGachaPoolType_Standard",
            "E_CharacterGachaPoolType_Beginner"
    );

    private static final int BATCH_SIZE = 200; // 批处理大小
    private static final int RETRY_COUNT = 3; // 重试次数
    private static final long RETRY_DELAY_MS = 500; // 重试延迟
    private static final int API_DELAY_MS = 50; // API调用间隔

    private final String CHARACTER_RECORD_API = "https://endfield.hypergryph.com/webview/api/record/char";
    private final String LANG = "zh-cn";

    private final CharacterPoolRecordMapper characterPoolRecordMapper;
    private final IdGenerator idGenerator;
    private final ThreadPoolExecutor asyncExecutor;
    
    // 性能监控
    private final AtomicLong totalProcessedRecords = new AtomicLong(0);
    private final AtomicLong totalDuplicatedRecords = new AtomicLong(0);
    private final AtomicLong totalFailedRecords = new AtomicLong(0);
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    /**
     * 构造函数
     *
     * @param characterPoolRecordMapper 角色卡池记录Mapper
     */
    public CharacterPoolRecordServiceV3(CharacterPoolRecordMapper characterPoolRecordMapper) {
        this.characterPoolRecordMapper = characterPoolRecordMapper;
        this.idGenerator = new IdGenerator(1L);
        
        // 配置线程池 - 针对高并发优化
        this.asyncExecutor = new ThreadPoolExecutor(
                15, // 核心线程数 - 适中避免过载
                40, // 最大线程数 - 足够的并发能力
                60L, // 空闲线程存活时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000), // 队列容量 - 大量缓冲
                r -> {
                    Thread thread = new Thread(r, "character-pool-v3-" + System.currentTimeMillis());
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：使用调用者运行
        );
    }

    /**
     * 保存角色卡池记录 - 异步版本
     * 核心优化：移除内存检查，直接利用数据库唯一索引处理重复
     *
     * @param httpServletRequest HTTP请求
     * @param uid 用户ID
     * @param url API URL
     * @return CompletableFuture<BatchProcessResult> 处理结果
     */
    @Async("asyncExecutor")
    public CompletableFuture<BatchProcessResult> saveCharacterPoolRecordAsync(HttpServletRequest httpServletRequest, String uid, String url) {
        long startTime = System.currentTimeMillis();
        activeTasks.incrementAndGet();
        
        try {
            logger.info("开始处理任务: {}, 用户: {}", uid, url);
            
            if (uid == null) {
                throw new ServiceException(ResultCode.USER_NOT_EXIST);
            }

            Map<String, String> urlParams = UrlParser.parseEndfieldGachaUrl(url);
            String serverId = urlParams.get("server_id");
            
            // 并发处理所有池类型
            List<CompletableFuture<List<CharacterPoolRecord>>> futures = new ArrayList<>();
            
            for (String poolType : POOL_TYPES) {
                CompletableFuture<List<CharacterPoolRecord>> future = CompletableFuture
                        .supplyAsync(() -> processPoolType(urlParams, poolType, uid, serverId), asyncExecutor);
                futures.add(future);
            }

            // 等待所有池类型处理完成
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            
            List<CharacterPoolRecord> allRecords = allFutures.thenApply(v -> {
                List<CharacterPoolRecord> result = new ArrayList<>();
                for (CompletableFuture<List<CharacterPoolRecord>> future : futures) {
                    try {
                        result.addAll(future.get());
                    } catch (Exception e) {
                        logger.error("获取处理结果失败", e);
                    }
                }
                return result;
            }).join();

            // 批量插入数据库 - 利用数据库唯一约束
            BatchProcessResult result = batchInsertWithUniqueIndex(allRecords, uid);
            
            long duration = System.currentTimeMillis() - startTime;
            logger.info("任务完成: {}, 耗时: {}ms, 新增: {}, 重复: {}, 失败: {}", 
                    uid, duration, result.getSuccessCount(), result.getDuplicatedCount(), result.getFailedCount());
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            logger.error("处理任务失败: {}", uid, e);
            totalFailedRecords.incrementAndGet();
            
            BatchProcessResult errorResult = new BatchProcessResult();
            errorResult.setSuccessCount(0);
            errorResult.setDuplicatedCount(0);
            errorResult.setFailedCount(0);
            errorResult.setErrorMessage(e.getMessage());
            
            return CompletableFuture.completedFuture(errorResult);
        } finally {
            activeTasks.decrementAndGet();
        }
    }

    /**
     * 同步版本 - 适用于测试和小批量数据
     */
    public BatchProcessResult saveCharacterPoolRecordSync(HttpServletRequest httpServletRequest, String uid, String url) {
        return saveCharacterPoolRecordAsync(httpServletRequest, uid, url).join();
    }

    /**
     * 获取实时性能统计
     */
    public PerformanceStats getPerformanceStats() {
        PerformanceStats stats = new PerformanceStats();
        stats.setTotalProcessedRecords(totalProcessedRecords.get());
        stats.setTotalDuplicatedRecords(totalDuplicatedRecords.get());
        stats.setTotalFailedRecords(totalFailedRecords.get());
        stats.setActiveTasks(activeTasks.get());
        stats.setCurrentTime(new Date());
        return stats;
    }

    /**
     * 处理单个池类型的数据
     * 简化逻辑：移除所有内存检查，直接返回所有记录
     */
    private List<CharacterPoolRecord> processPoolType(Map<String, String> urlParams, String poolType, String uid, String serverId) {
        List<CharacterPoolRecord> records = new ArrayList<>();
        
        try {
            Boolean hasMore = true;
            String poolSeqId = null;
            int pageCount = 0;
            
            while (hasMore && pageCount < 10) { // 限制页数防止无限循环
                //TODO  这里不对
                String finalPoolSeqId = poolSeqId;
                CharacterPoolRecordResponseDTO responseDTO = retryExecute(() ->
                    requestCharacterPoolRecordAPI(urlParams, poolType, finalPoolSeqId)
                );
                
                if (responseDTO == null || responseDTO.getData() == null) {
                    break;
                }
                
                hasMore = responseDTO.getData().getHasMore();
                List<CharacterPoolRecordDTO> recordDTOList = responseDTO.getData().getList();
                
                if (recordDTOList.isEmpty()) {
                    break;
                }
                
                for (CharacterPoolRecordDTO dto : recordDTOList) {
                    CharacterPoolRecord record = convertToEntity(dto, uid, serverId, poolType);
                    records.add(record);
                    poolSeqId = dto.getSeqId();
                }
                
                pageCount++;
                
                // 添加小延迟避免API限制
                if (hasMore) {
                    Thread.sleep(API_DELAY_MS);
                }
                
            }
            
        } catch (Exception e) {
            logger.error("处理池类型失败: {}", poolType, e);
        }
        
        return records;
    }

    /**
     * 批量插入记录，利用数据库唯一索引自动处理重复
     * 核心优化：移除复杂的内存检查，让数据库处理重复
     */
    @Transactional
    public BatchProcessResult batchInsertWithUniqueIndex(List<CharacterPoolRecord> records, String uid) {
        BatchProcessResult result = new BatchProcessResult();
        
        if (records.isEmpty()) {
            result.setSuccessCount(0);
            result.setDuplicatedCount(0);
            result.setFailedCount(0);
            result.setMessage("没有记录需要插入");
            return result;
        }
        
        int successCount = 0;
        int duplicatedCount = 0;
        int failedCount = 0;
        List<String> errorMessages = new ArrayList<>();
        
        // 分批插入
        for (int i = 0; i < records.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, records.size());
            List<CharacterPoolRecord> batch = records.subList(i, endIndex);
            
            try {
                // 生成ID
                for (CharacterPoolRecord record : batch) {
                    record.setId(idGenerator.nextId());
                }
                
                // 批量插入 - 利用数据库唯一约束
                characterPoolRecordMapper.batchInsert(batch);
                successCount += batch.size();
                totalProcessedRecords.addAndGet(batch.size());
                
                logger.debug("批处理插入完成: {}/{}", successCount, records.size());
                
            } catch (Exception e) {
                logger.warn("批处理插入遇到重复数据或其他错误: {}-{}, 原因: {}", i, endIndex, e.getMessage());
                
                // 尝试逐条插入，捕获重复错误
                for (CharacterPoolRecord record : batch) {
                    try {
                        characterPoolRecordMapper.insert(record);
                        successCount++;
                        totalProcessedRecords.incrementAndGet();
                    } catch (Exception singleError) {
                        if (isDuplicateKeyError(singleError)) {
                            duplicatedCount++;
                            totalDuplicatedRecords.incrementAndGet();
                            logger.debug("检测到重复记录: {}-{}", record.getPoolName(), record.getSeqId());
                        } else {
                            failedCount++;
                            totalFailedRecords.incrementAndGet();
                            errorMessages.add("记录插入失败: " + record.getSeqId() + " - " + singleError.getMessage());
                            logger.error("单条记录插入失败: {}", record.getSeqId(), singleError);
                        }
                    }
                }
            }
        }
        
        result.setSuccessCount(successCount);
        result.setDuplicatedCount(duplicatedCount);
        result.setFailedCount(failedCount);
        result.setErrorMessages(errorMessages);
        result.setMessage(String.format("插入完成 - 新增: %d, 重复: %d, 失败: %d", successCount, duplicatedCount, failedCount));
        
        return result;
    }

    /**
     * 判断是否为重复键错误
     */
    private boolean isDuplicateKeyError(Exception error) {
        if (error instanceof org.mybatis.spring.MyBatisSystemException) {
            Throwable cause = error.getCause();
            if (cause instanceof java.sql.SQLException) {
                String sqlState = ((SQLException) cause).getSQLState();
                // MySQL重复键错误代码：23000
                return "23000".equals(sqlState);
            }
        }
        return error.getMessage() != null && 
               (error.getMessage().contains("Duplicate entry") || 
                error.getMessage().contains("UNIQUE constraint failed"));
    }

    /**
     * 带重试的API请求
     */
    private CharacterPoolRecordResponseDTO retryExecute(Supplier<CharacterPoolRecordResponseDTO> supplier) {
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                return supplier.get();
            } catch (Exception e) {
                if (i == RETRY_COUNT - 1) {
                    logger.error("API请求最终失败", e);
                    throw new RuntimeException("API请求失败", e);
                }
                
                try {
                    Thread.sleep(RETRY_DELAY_MS * (i + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("线程被中断", ie);
                }
            }
        }
        return null;
    }

    /**
     * 转化DTO类为实体类
     * @param dto
     * @param uid
     * @param serverId
     * @param poolType
     * @return
     */
    private CharacterPoolRecord convertToEntity(CharacterPoolRecordDTO dto, String uid, String serverId, String poolType) {
        CharacterPoolRecord record = new CharacterPoolRecord();
        record.setEndfieldUid("");
        record.setUid(uid);
        record.setPoolId(dto.getPoolId());
        record.setPoolName(dto.getPoolName());
        record.setCharId(dto.getCharId());
        record.setCharName(dto.getCharName());
        record.setRarity(dto.getRarity());
        record.setIsFree(dto.getIsFree());
        record.setIsNew(dto.getIsNew());
        record.setGachaTs(dto.getGachaTs());
        record.setSeqId(dto.getSeqId());
        record.setLang(LANG);
        record.setPoolType(poolType);
        record.setServerId(serverId);
        return record;
    }

    /**
     * 请求API - 保持原有逻辑
     */
    private CharacterPoolRecordResponseDTO requestCharacterPoolRecordAPI(Map<String, String> urlParams, String poolType, String seqId) {
        String token = urlParams.get("token");
        String server_id = urlParams.get("server_id");
        
        String encodeToken = smartUrlEncode(token);
        String url = CHARACTER_RECORD_API + "?lang=" + LANG + "&pool_type=" + poolType + "&token=" + encodeToken + "&server_id=" + server_id;
        if (seqId != null) {
            url += "&seq_id=" + seqId;
        }
        
        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:146.0) Gecko/20100101 Firefox/146.0");
            headers.put("Accept", "application/json, text/plain, */*");
            headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
            headers.put("Accept-Encoding", "gzip, deflate, br, zstd");
            headers.put("Connection", "keep-alive");
            headers.put("Sec-Fetch-Dest", "empty");
            headers.put("Sec-Fetch-Mode", "cors");
            headers.put("Sec-Fetch-Site", "same-origin");
            headers.put("Priority", "u=0");
            headers.put("Host", "endfield.hypergryph.com");
            
            String response = OkHttpUtil.getWithHeaders(url, headers);
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(response, CharacterPoolRecordResponseDTO.class);
            
        } catch (IOException e) {
            logger.error("API请求失败: {}", url, e);
            throw new RuntimeException("API请求失败", e);
        }
    }

    /**
     * 获取用户角色卡池记录（原有接口保持不变）
     */
    public List<CharacterPoolRecord> getCharacterPoolRecordData(HttpServletRequest httpServletRequest, String uid) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CharacterPoolRecord> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        queryWrapper.eq(CharacterPoolRecord::getUid, uid);
        return characterPoolRecordMapper.selectList(queryWrapper);
    }

    /**
     * 关闭资源
     */
    public void shutdown() {
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                asyncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // 内部类定义
    public static class BatchProcessResult {
        private int successCount;
        private int duplicatedCount;
        private int failedCount;
        private String message;
        private String errorMessage;
        private List<String> errorMessages;

        // Getters and Setters
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getDuplicatedCount() { return duplicatedCount; }
        public void setDuplicatedCount(int duplicatedCount) { this.duplicatedCount = duplicatedCount; }
        
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public List<String> getErrorMessages() { return errorMessages; }
        public void setErrorMessages(List<String> errorMessages) { this.errorMessages = errorMessages; }
    }

    public static class PerformanceStats {
        private long totalProcessedRecords;
        private long totalDuplicatedRecords;
        private long totalFailedRecords;
        private int activeTasks;
        private Date currentTime;

        // Getters and Setters
        public long getTotalProcessedRecords() { return totalProcessedRecords; }
        public void setTotalProcessedRecords(long totalProcessedRecords) { this.totalProcessedRecords = totalProcessedRecords; }
        
        public long getTotalDuplicatedRecords() { return totalDuplicatedRecords; }
        public void setTotalDuplicatedRecords(long totalDuplicatedRecords) { this.totalDuplicatedRecords = totalDuplicatedRecords; }
        
        public long getTotalFailedRecords() { return totalFailedRecords; }
        public void setTotalFailedRecords(long totalFailedRecords) { this.totalFailedRecords = totalFailedRecords; }
        
        public int getActiveTasks() { return activeTasks; }
        public void setActiveTasks(int activeTasks) { this.activeTasks = activeTasks; }
        
        public Date getCurrentTime() { return currentTime; }
        public void setCurrentTime(Date currentTime) { this.currentTime = currentTime; }
    }

    // Supplier函数式接口
    @FunctionalInterface
    private interface Supplier<T> {
        T get() throws Exception;
    }
}