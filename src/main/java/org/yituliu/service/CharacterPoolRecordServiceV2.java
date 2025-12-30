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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.yituliu.util.OkHttpUtil;
import org.yituliu.util.UrlParser;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.yituliu.util.UrlEncodeUtil.smartUrlEncode;

@Service
public class CharacterPoolRecordServiceV2 {

    private static final Logger logger = LoggerFactory.getLogger(CharacterPoolRecordServiceV2.class);

    private static final Set<String> POOL_TYPES = Set.of(
            "E_CharacterGachaPoolType_Special",
            "E_CharacterGachaPoolType_Standard",
            "E_CharacterGachaPoolType_Beginner"
    );

    private static final int BATCH_SIZE = 200; // 批处理大小
    private static final int MAX_CONCURRENT_REQUESTS = 50; // 最大并发请求数
    private static final int RETRY_COUNT = 3; // 重试次数
    private static final long RETRY_DELAY_MS = 1000; // 重试延迟

    private final String CHARACTER_RECORD_API = "https://endfield.hypergryph.com/webview/api/record/char";
    private final String Lang = "zh-cn";

    private final CharacterPoolRecordMapper characterPoolRecordMapper;
    private final IdGenerator idGenerator;
    private final ThreadPoolExecutor asyncExecutor;
    
    // 性能监控
    private final AtomicLong totalProcessedRecords = new AtomicLong(0);
    private final AtomicLong totalFailedRecords = new AtomicLong(0);
    private final AtomicInteger activeTasks = new AtomicInteger(0);
    private final ConcurrentHashMap<String, TaskStats> taskStatsMap = new ConcurrentHashMap<>();

    public CharacterPoolRecordServiceV2(CharacterPoolRecordMapper characterPoolRecordMapper) {
        this.characterPoolRecordMapper = characterPoolRecordMapper;
        this.idGenerator = new IdGenerator(1L);
        
        // 配置线程池
        this.asyncExecutor = new ThreadPoolExecutor(
                20, // 核心线程数
                50, // 最大线程数
                60L, // 空闲线程存活时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000), // 队列容量
                new ThreadFactoryBuilder().setNameFormat("character-pool-async-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：使用调用者运行
        );
    }

    /**
     * 高并发处理角色卡池记录
     * 支持批量异步处理，优化数据库操作
     */
    @Async("asyncExecutor")
    public CompletableFuture<BatchProcessResult> saveCharacterPoolRecordAsync(HttpServletRequest httpServletRequest, String uid, String url) {
        String taskId = generateTaskId();
        long startTime = System.currentTimeMillis();
        activeTasks.incrementAndGet();
        
        TaskStats taskStats = new TaskStats(taskId, uid, startTime);
        taskStatsMap.put(taskId, taskStats);
        
        try {
            logger.info("开始处理任务: {}, 用户: {}, URL: {}", taskId, uid, url);
            
            if (uid == null) {
                throw new ServiceException(ResultCode.USER_NOT_EXIST);
            }

            Map<String, String> urlParams = UrlParser.parseEndfieldGachaUrl(url);
            String serverId = urlParams.get("server_id");
            
            // 使用CompletableFuture进行并发处理
            List<CompletableFuture<List<CharacterPoolRecord>>> futures = new ArrayList<>();
            
            for (String poolType : POOL_TYPES) {
                CompletableFuture<List<CharacterPoolRecord>> future = CompletableFuture
                        .supplyAsync(() -> processPoolType(urlParams, poolType, uid, serverId), asyncExecutor)
                        .thenApply(this::filterExistingRecords);
                futures.add(future);
            }

            // 等待所有池类型处理完成
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            
            List<CharacterPoolRecord> allNewRecords = allFutures.thenApply(v -> {
                List<CharacterPoolRecord> result = new ArrayList<>();
                for (CompletableFuture<List<CharacterPoolRecord>> future : futures) {
                    try {
                        result.addAll(future.get());
                    } catch (Exception e) {
                        logger.error("获取处理结果失败", e);
                        totalFailedRecords.incrementAndGet();
                    }
                }
                return result;
            }).join();

            // 批量插入数据库
            BatchProcessResult result = batchInsertRecords(allNewRecords, taskId);
            
            // 更新任务统计
            long duration = System.currentTimeMillis() - startTime;
            taskStats.setDuration(duration);
            taskStats.setProcessedRecords(result.getSuccessCount());
            taskStats.setStatus("COMPLETED");
            
            logger.info("任务完成: {}, 耗时: {}ms, 成功: {}, 失败: {}", 
                    taskId, duration, result.getSuccessCount(), result.getFailedCount());
            
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            logger.error("处理任务失败: {}", taskId, e);
            totalFailedRecords.incrementAndGet();
            taskStats.setStatus("FAILED");
            taskStats.setErrorMessage(e.getMessage());
            
            BatchProcessResult errorResult = new BatchProcessResult();
            errorResult.setTaskId(taskId);
            errorResult.setSuccessCount(0);
            errorResult.setFailedCount(0);
            errorResult.setErrorMessage(e.getMessage());
            
            return CompletableFuture.completedFuture(errorResult);
        } finally {
            activeTasks.decrementAndGet();
            taskStatsMap.remove(taskId);
        }
    }

    /**
     * 同步版本，用于测试和小批量数据
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
        stats.setTotalFailedRecords(totalFailedRecords.get());
        stats.setActiveTasks(activeTasks.get());
        stats.setCompletedTasks(taskStatsMap.size());
        stats.setCurrentTime(LocalDateTime.now());
        return stats;
    }

    /**
     * 获取任务状态
     */
    public TaskStats getTaskStatus(String taskId) {
        return taskStatsMap.get(taskId);
    }

    /**
     * 处理单个池类型的数据
     */
    private List<CharacterPoolRecord> processPoolType(Map<String, String> urlParams, String poolType, String uid, String serverId) {
        List<CharacterPoolRecord> newRecords = new ArrayList<>();
        
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
                    newRecords.add(record);
                    poolSeqId = dto.getSeqId();
                }
                
                pageCount++;
                
                // 添加小延迟避免API限制
                if (hasMore) {
                    Thread.sleep(50);
                }
                
            }
            
        } catch (Exception e) {
            logger.error("处理池类型失败: {}", poolType, e);
        }
        
        return newRecords;
    }

    /**
     * 过滤已存在的记录
     */
    private List<CharacterPoolRecord> filterExistingRecords(List<CharacterPoolRecord> records) {
        if (records.isEmpty()) {
            return records;
        }
        
        Set<String> existingIndexes = new HashSet<>();
        String uid = records.get(0).getUid();
        
        // 批量查询已存在的记录
        for (int i = 0; i < records.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, records.size());
            List<CharacterPoolRecord> batch = records.subList(i, endIndex);
            
            LambdaQueryWrapper<CharacterPoolRecord> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CharacterPoolRecord::getUid, uid);
            
            // 这里简化处理，实际应该根据业务逻辑构建查询条件
            List<CharacterPoolRecord> existingRecords = characterPoolRecordMapper.selectList(queryWrapper);
            
            for (CharacterPoolRecord existing : existingRecords) {
                existingIndexes.add(existing.getPoolName() + existing.getSeqId());
            }
        }
        
        // 过滤掉已存在的记录
        return records.stream()
                .filter(record -> !existingIndexes.contains(record.getPoolName() + record.getSeqId()))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 批量插入记录
     */
    @Transactional
    public BatchProcessResult batchInsertRecords(List<CharacterPoolRecord> records, String taskId) {
        BatchProcessResult result = new BatchProcessResult();
        result.setTaskId(taskId);
        
        if (records.isEmpty()) {
            result.setSuccessCount(0);
            result.setFailedCount(0);
            result.setMessage("没有新记录需要插入");
            return result;
        }
        
        int successCount = 0;
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
                
                // 批量插入
                characterPoolRecordMapper.batchInsert(batch);
                successCount += batch.size();
                totalProcessedRecords.addAndGet(batch.size());
                
                logger.debug("批处理插入完成: {}/{}", successCount, records.size());
                
            } catch (Exception e) {
                logger.error("批处理插入失败: {}-{}", i, endIndex, e);
                failedCount += batch.size();
                totalFailedRecords.addAndGet(batch.size());
                errorMessages.add("批次 " + (i/BATCH_SIZE + 1) + " 插入失败: " + e.getMessage());
                
                // 单条重试
                for (CharacterPoolRecord record : batch) {
                    try {
                        characterPoolRecordMapper.insert(record);
                        successCount++;
                        failedCount--;
                    } catch (Exception singleError) {
                        logger.error("单条记录插入失败: {}", record.getSeqId(), singleError);
                    }
                }
            }
        }
        
        result.setSuccessCount(successCount);
        result.setFailedCount(failedCount);
        result.setErrorMessages(errorMessages);
        result.setMessage(String.format("插入完成 - 成功: %d, 失败: %d", successCount, failedCount));
        
        return result;
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
     * 转换DTO到实体
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
        record.setLang(Lang);
        record.setPoolType(poolType);
        record.setServerId(serverId);
        return record;
    }

    /**
     * 请求API
     */
    private CharacterPoolRecordResponseDTO requestCharacterPoolRecordAPI(Map<String, String> urlParams, String poolType, String seqId) {
        String token = urlParams.get("token");
        String server_id = urlParams.get("server_id");
        
        String encodeToken = smartUrlEncode(token);
        String url = CHARACTER_RECORD_API + "?lang=" + Lang + "&pool_type=" + poolType + "&token=" + encodeToken + "&server_id=" + server_id;
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
     * 生成任务ID
     */
    private String generateTaskId() {
        return "TASK_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
    }

    /**
     * 获取用户角色卡池记录（同步版本）
     */
    public List<CharacterPoolRecord> getCharacterPoolRecordData(HttpServletRequest httpServletRequest, String uid) {
        LambdaQueryWrapper<CharacterPoolRecord> queryWrapper = new LambdaQueryWrapper<>();
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
        private String taskId;
        private int successCount;
        private int failedCount;
        private String message;
        private String errorMessage;
        private List<String> errorMessages;

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        
        public int getFailedCount() { return failedCount; }
        public void setFailedCount(int failedCount) { this.failedCount = failedCount; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public List<String> getErrorMessages() { return errorMessages; }
        public void setErrorMessages(List<String> errorMessages) { this.errorMessages = errorMessages; }
    }

    public static class TaskStats {
        private String taskId;
        private String uid;
        private long startTime;
        private long duration;
        private int processedRecords;
        private String status;
        private String errorMessage;

        public TaskStats(String taskId, String uid, long startTime) {
            this.taskId = taskId;
            this.uid = uid;
            this.startTime = startTime;
            this.status = "RUNNING";
        }

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        
        public String getUid() { return uid; }
        public void setUid(String uid) { this.uid = uid; }
        
        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }
        
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        
        public int getProcessedRecords() { return processedRecords; }
        public void setProcessedRecords(int processedRecords) { this.processedRecords = processedRecords; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public static class PerformanceStats {
        private long totalProcessedRecords;
        private long totalFailedRecords;
        private int activeTasks;
        private int completedTasks;
        private LocalDateTime currentTime;

        // Getters and Setters
        public long getTotalProcessedRecords() { return totalProcessedRecords; }
        public void setTotalProcessedRecords(long totalProcessedRecords) { this.totalProcessedRecords = totalProcessedRecords; }
        
        public long getTotalFailedRecords() { return totalFailedRecords; }
        public void setTotalFailedRecords(long totalFailedRecords) { this.totalFailedRecords = totalFailedRecords; }
        
        public int getActiveTasks() { return activeTasks; }
        public void setActiveTasks(int activeTasks) { this.activeTasks = activeTasks; }
        
        public int getCompletedTasks() { return completedTasks; }
        public void setCompletedTasks(int completedTasks) { this.completedTasks = completedTasks; }
        
        public LocalDateTime getCurrentTime() { return currentTime; }
        public void setCurrentTime(LocalDateTime currentTime) { this.currentTime = currentTime; }
    }

    // 线程工厂构建器
    private static class ThreadFactoryBuilder {
        private String nameFormat = "pool-%d";

        public ThreadFactoryBuilder setNameFormat(String nameFormat) {
            this.nameFormat = nameFormat;
            return this;
        }

        public ThreadFactory build() {
            return new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, String.format(nameFormat, counter.getAndIncrement()));
                    thread.setDaemon(true);
                    return thread;
                }
            };
        }
    }

    // Supplier函数式接口
    @FunctionalInterface
    private interface Supplier<T> {
        T get() throws Exception;
    }
}