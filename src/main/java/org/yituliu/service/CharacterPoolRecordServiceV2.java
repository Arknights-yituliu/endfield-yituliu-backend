package org.yituliu.service;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.yituliu.common.enums.ResultCode;
import org.yituliu.common.exception.ServiceException;
import org.yituliu.common.utils.IdGenerator;
import org.yituliu.common.utils.JsonMapper;
import org.yituliu.common.utils.LogUtils;
import org.yituliu.entity.dto.CharacterPoolRecordDTO;
import org.yituliu.entity.dto.CharacterPoolRecordDataDTO;
import org.yituliu.entity.dto.CharacterPoolRecordResponseDTO;
import org.yituliu.entity.log.BatchProcessResult;
import org.yituliu.entity.po.CharacterPoolRecord;
import org.yituliu.mapper.CharacterPoolRecordMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.yituliu.util.OkHttpUtil;
import org.yituliu.util.UrlParser;


import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.yituliu.util.UrlEncodeUtil.smartUrlEncode;

@Service
public class CharacterPoolRecordServiceV2 {



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

    // 性能监控相关计数器
    // 总处理记录数：原子长整型，线程安全地记录已处理的记录总数
    private final AtomicLong totalProcessedRecords = new AtomicLong(0);
    // 总重复记录数：原子长整型，线程安全地记录检测到的重复记录总数
    private final AtomicLong totalDuplicatedRecords = new AtomicLong(0);
    // 总失败记录数：原子长整型，线程安全地记录处理失败的记录总数
    private final AtomicLong totalFailedRecords = new AtomicLong(0);
    // 活跃任务数：原子整型，线程安全地记录当前正在执行的任务数量
    private final AtomicInteger activeTasks = new AtomicInteger(0);

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
        // 记录任务开始时间，用于计算处理耗时
        long startTime = System.currentTimeMillis();
        // 记录任务开始日志，包含用户ID和URL信息
        LogUtils.info("开始处理角色抽卡记录导入任务:  用户: {}", uid);
        try {
        // 参数校验：检查用户ID是否为空
        if (uid == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }

        // 解析URL参数，提取token和server_id等信息
        Map<String, String> urlParams = UrlParser.parseEndfieldGachaUrl(url);

        // 从URL参数中获取服务器ID
        String serverId = urlParams.get("server_id");

        List<CharacterPoolRecord> characterPoolRecordList = new ArrayList<>();

        // 并发处理所有池类型：为每个卡池类型创建异步任务
        List<CompletableFuture<List<CharacterPoolRecordDTO>>> futures = new ArrayList<>();

        // 遍历所有卡池类型，为每种类型创建异步处理任务
        for (String poolType : POOL_TYPES) {
            List<CharacterPoolRecordDTO> characterPoolRecordDTOList = requestCharacterPoolRecordAPI(urlParams, poolType, null);
            String lastSeqId = characterPoolRecordDTOList.get(characterPoolRecordDTOList.size() - 1).getSeqId();
            List<String> seqIdList = initSeqIdList(lastSeqId, 1);

            for (String seqId : seqIdList) {
                CompletableFuture<List<CharacterPoolRecordDTO>> future = CompletableFuture
                        .supplyAsync(() -> requestCharacterPoolRecordAPI(urlParams, poolType, seqId), asyncExecutor);
                futures.add(future);
            }


            // 等待所有池类型处理完成：使用CompletableFuture.allOf等待所有任务完成
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

            List<CharacterPoolRecordDTO> allFuturesResult = allFutures.thenApply(v -> {
                List<CharacterPoolRecordDTO> result = new ArrayList<>();
                for (CompletableFuture<List<CharacterPoolRecordDTO>> future : futures) {
                    try {
                        // 获取单个任务的结果并合并到总结果中
                        result.addAll(future.get());
                    } catch (Exception e) {
                        // 记录获取结果失败的错误日志
                        LogUtils.error("获取处理结果失败", e);
                    }
                }
                return result;
            }).join();

            allFuturesResult.forEach(e -> {
//                characterPoolRecordList.add(convertToEntity(e, uid, serverId, poolType));
            });
        }

       // 批量插入数据库 - 利用数据库唯一约束：将收集到的所有记录批量插入数据库
       BatchProcessResult batchProcessResult = batchInsertWithUniqueIndex(characterPoolRecordList);

        // 计算处理耗时
        long duration = System.currentTimeMillis() - startTime;
        // 记录任务完成日志，包含处理统计信息
       LogUtils.info("任务完成:uid {}, 耗时: {}ms, 新增: {}, 重复: {}, 失败: {}",
                uid, duration, batchProcessResult.getSuccessCount(), batchProcessResult.getDuplicatedCount(), batchProcessResult.getFailedCount());
        return CompletableFuture.completedFuture(batchProcessResult);
        } catch (Exception e) {
            // 处理异常情况：记录错误日志并返回错误结果
            LogUtils.error("处理任务失败: {}", uid, e);
            // 增加失败记录计数器
            totalFailedRecords.incrementAndGet();
            // 创建错误结果对象
            BatchProcessResult errorResult = new BatchProcessResult();
            errorResult.setSuccessCount(0);
            errorResult.setDuplicatedCount(0);
            errorResult.setFailedCount(0);
            errorResult.setErrorMessage(e.getMessage());

            // 返回错误结果
            return CompletableFuture.completedFuture(errorResult);
        } finally {
            // 减少活跃任务计数器，确保计数器正确释放
            activeTasks.decrementAndGet();
        }
    }


    private BatchProcessResult batchInsertWithUniqueIndex(List<CharacterPoolRecord> characterPoolRecordList){
        // 创建处理结果对象
        BatchProcessResult result = new BatchProcessResult();
        // 检查记录列表是否为空
        if (characterPoolRecordList.isEmpty()) {
            // 设置空结果并返回
            result.setSuccessCount(0);
            result.setDuplicatedCount(0);
            result.setFailedCount(0);
            result.setMessage("没有记录需要插入");
            return result;
        }

        // 初始化计数器
        int successCount = 0; // 成功插入的记录数
        int duplicatedCount = 0; // 重复的记录数
        int failedCount = 0; // 失败的记录数
        List<String> errorMessages = new ArrayList<>(); // 错误信息列表

        // 分批插入：将大列表分割为小批次进行处理
        for (int i = 0; i < characterPoolRecordList.size(); i += BATCH_SIZE) {
            // 计算当前批次的结束索引
            int endIndex = Math.min(i + BATCH_SIZE, characterPoolRecordList.size());
            // 获取当前批次的数据子列表
            List<CharacterPoolRecord> batch = characterPoolRecordList.subList(i, endIndex);

            try {
                // 生成ID：为当前批次的每条记录生成唯一主键ID
                for (CharacterPoolRecord record : batch) {
                    record.setId(idGenerator.nextId());
                }

                // 批量插入 - 利用数据库唯一约束：尝试批量插入当前批次
                characterPoolRecordMapper.batchInsert(batch);
                // 更新成功计数器：增加当前批次的记录数
                successCount += batch.size();
                // 更新总处理记录计数器：原子操作增加当前批次的记录数
                totalProcessedRecords.addAndGet(batch.size());

                // 记录调试日志：显示批处理进度
                LogUtils.info("批处理插入完成: {}/{}", successCount, characterPoolRecordList.size());

            } catch (Exception e) {
                // 批量插入失败：记录警告日志，包含批次范围和错误原因
                LogUtils.error("批处理插入遇到重复数据或其他错误: {}-{}, 原因: {}", i, endIndex, e.getMessage());

                // 尝试逐条插入，捕获重复错误：当批量插入失败时，改为逐条插入
                for (CharacterPoolRecord record : batch) {
                    try {
                        // 尝试插入单条记录
                        characterPoolRecordMapper.insert(record);
                        // 插入成功，增加成功计数器
                        successCount++;
                        // 更新总处理记录计数器
                        totalProcessedRecords.incrementAndGet();
                    } catch (Exception singleError) {
                        // 检查是否为重复键错误
                        if (isDuplicateKeyError(singleError)) {
                            // 重复记录处理：增加重复计数器
                            duplicatedCount++;
                            // 更新总重复记录计数器
                            totalDuplicatedRecords.incrementAndGet();
                            // 记录调试日志：显示重复记录信息
                            LogUtils.info("检测到重复记录: {}-{}", record.getPoolName(), record.getSeqId());
                        } else {
                            // 其他错误处理：增加失败计数器
                            failedCount++;
                            // 更新总失败记录计数器
                            totalFailedRecords.incrementAndGet();
                            // 添加错误信息到列表
                            errorMessages.add("记录插入失败: " + record.getSeqId() + " - " + singleError.getMessage());
                            // 记录错误日志：显示失败记录和错误详情
                            LogUtils.error("单条记录插入失败: {}", record.getSeqId(), singleError);
                        }
                    }
                }
            }
        }

        // 设置处理结果
        result.setSuccessCount(successCount);
        result.setDuplicatedCount(duplicatedCount);
        result.setFailedCount(failedCount);
        result.setErrorMessages(errorMessages);
        // 生成处理结果消息
        result.setMessage(String.format("插入完成 - 新增: %d, 重复: %d, 失败: %d", successCount, duplicatedCount, failedCount));

        // 返回处理结果
        return result;
    }

    /**
     * 判断是否为重复键错误
     * 该方法检查异常是否为数据库唯一约束违反错误
     * 支持MySQL和SQLite等常见数据库的重复键错误判断
     *
     * @param error 需要检查的异常对象
     * @return boolean 如果为重复键错误返回true，否则返回false
     */
    private boolean isDuplicateKeyError(Exception error) {
        // 检查是否为MyBatis系统异常
        if (error instanceof org.mybatis.spring.MyBatisSystemException) {
            // 获取异常的根本原因
            Throwable cause = error.getCause();
            // 检查根本原因是否为SQL异常
            if (cause instanceof java.sql.SQLException) {
                // 获取SQL状态码
                String sqlState = ((SQLException) cause).getSQLState();
                // MySQL重复键错误代码：23000
                return "23000".equals(sqlState);
            }
        }
        // 通过错误消息内容判断：检查错误消息中是否包含重复键相关的关键词
        return error.getMessage() != null &&
                (error.getMessage().contains("Duplicate entry") || // MySQL重复键错误消息
                        error.getMessage().contains("UNIQUE constraint failed")); // SQLite唯一约束失败错误消息
    }

    /**
     * 初始化序列ID列表，每页为5个记录，从最新序列id开始递减5，生成一个序列id表
     * 适用于已知lastSeqId为正数且大于minSeqId
     *
     * @param lastSeqIdStr  从终末地API返回的最新序列id（seqId)
     * @param existingSeqId 该用户保存在数据库中的记录的最后一个序列id 再-10，多查两页，以防出现问题
     * @return 序列ID字符串列表
     */
    private List<String> initSeqIdList(String lastSeqIdStr, Integer existingSeqId) {
        // 参数校验
        if (lastSeqIdStr == null || lastSeqIdStr.trim().isEmpty()) {
            throw new IllegalArgumentException("lastSeqIdStr不能为空");
        }

        if (existingSeqId == null) {
            throw new IllegalArgumentException("minSeqId不能为空");
        }

        int lastSeqId = Integer.parseInt(lastSeqIdStr.trim());

//        if (lastSeqId < existingSeqId) {
//            return Collections.emptyList();
//        }

        List<String> seqIdList = new ArrayList<>((lastSeqId - existingSeqId) / 5 + 3);
        for (int i = lastSeqId; i >= existingSeqId; i -= 5) {
            seqIdList.add(String.valueOf(i));
        }
        return seqIdList;
    }






    /**
     * 请求API
     */
    private List<CharacterPoolRecordDTO> requestCharacterPoolRecordAPI(Map<String, String> urlParams, String poolType, String seqId) {
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
            //            ObjectMapper objectMapper = new ObjectMapper();
            //            return objectMapper.readValue(response, CharacterPoolRecordResponseDTO.class);
            CharacterPoolRecordResponseDTO characterPoolRecordResponseDTO = JsonMapper.parseObject(response, new TypeReference<>() {
            });


            if (characterPoolRecordResponseDTO != null && 0 == characterPoolRecordResponseDTO.getCode()) {
                CharacterPoolRecordDataDTO data = characterPoolRecordResponseDTO.getData();
                if (data != null) {
                    return data.getList();
                }
            }


            return Collections.emptyList();
        } catch (IOException e) {
            LogUtils.error("API请求失败: {}", url, e);
            throw new RuntimeException("API请求失败", e);
        }
    }


    /**
     * 转换DTO到实体
     */
    private CharacterPoolRecord convertToEntity(CharacterPoolRecordDTO dto, String uid, String serverId, String poolType) {
        CharacterPoolRecord record = new CharacterPoolRecord();
        record.setId(idGenerator.nextId());
        record.setEndfieldUid("no data");
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

    public List<CharacterPoolRecord> pullCharacterPoolRecordData(Map<String, String> urlParams, String poolType, String seqId) {
        // 创建记录列表，用于存储处理结果
        List<CharacterPoolRecord> records = new ArrayList<>();

        try {
            // 分页控制变量：标识是否还有更多数据需要获取
            Boolean hasMore = true;
            // 序列ID：用于分页请求，初始为null表示从第一页开始
            String poolSeqId = null;
            // 页面计数器：限制最大页数，防止无限循环
            int pageCount = 0;


        } catch (Exception e) {
            // 记录处理失败的错误日志，但不中断整个流程
            LogUtils.error("处理池类型失败: {}", poolType, e);
        }

        // 返回处理完成的记录列表
        return records;

    }

    /**
     * 同步版本，用于测试和小批量数据
     */
    public BatchProcessResult saveCharacterPoolRecordSync(HttpServletRequest httpServletRequest, String uid, String url) {
        return saveCharacterPoolRecordAsync(httpServletRequest, uid, url).join();
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
                    LogUtils.error("API请求最终失败", e);
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