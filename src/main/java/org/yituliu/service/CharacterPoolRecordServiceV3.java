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
@Service // Spring服务组件注解，标识这是一个Spring管理的服务类
public class CharacterPoolRecordServiceV3 {

    // 日志记录器实例，用于记录服务运行时的日志信息
    private static final Logger logger = LoggerFactory.getLogger(CharacterPoolRecordServiceV3.class);

    /**
     * 卡池类型常量集合
     * 包含三种卡池类型，静态初始化确保只存在一个实例
     */
    private static final Set<String> POOL_TYPES = Set.of(
            "E_CharacterGachaPoolType_Special",    // 特殊卡池类型
            "E_CharacterGachaPoolType_Standard",   // 标准卡池类型
            "E_CharacterGachaPoolType_Beginner"    // 新手卡池类型
    );

    // 批处理大小：每次批量插入数据库的记录数量，设置为200条记录
    private static final int BATCH_SIZE = 200;
    // 重试次数：API请求失败时的最大重试次数，设置为3次
    private static final int RETRY_COUNT = 3;
    // 重试延迟：每次重试之间的等待时间，单位为毫秒，设置为500毫秒
    private static final long RETRY_DELAY_MS = 500;
    // API调用间隔：连续API调用之间的最小间隔时间，单位为毫秒，设置为50毫秒，避免触发API限流
    private static final int API_DELAY_MS = 50;

    // 角色卡池记录API的URL地址，指向鹰角网络的官方API接口
    private final String CHARACTER_RECORD_API = "https://endfield.hypergryph.com/webview/api/record/char";
    // 语言参数：指定API返回的语言为简体中文
    private final String LANG = "zh-cn";

    // 角色卡池记录数据访问层接口，用于数据库操作
    private final CharacterPoolRecordMapper characterPoolRecordMapper;
    // ID生成器实例，用于生成唯一的主键ID
    private final IdGenerator idGenerator;
    // 异步执行线程池，用于处理并发任务
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

    /**
     * 构造函数
     * 初始化服务实例，配置线程池和依赖组件
     *
     * @param characterPoolRecordMapper 角色卡池记录Mapper，用于数据库操作
     */
    public CharacterPoolRecordServiceV3(CharacterPoolRecordMapper characterPoolRecordMapper) {
        // 注入角色卡池记录数据访问层接口
        this.characterPoolRecordMapper = characterPoolRecordMapper;
        // 创建ID生成器实例，使用工作节点ID为1
        this.idGenerator = new IdGenerator(1L);
        
        // 配置线程池 - 针对2核服务器优化
        this.asyncExecutor = new ThreadPoolExecutor(
                4, // 核心线程数：针对2核CPU，设置为4个线程，充分利用CPU资源
                8, // 最大线程数：最大8个线程，为突发流量提供缓冲
                60L, // 空闲线程存活时间：设置为60秒，平衡资源利用和响应速度
                TimeUnit.SECONDS, // 时间单位：秒
                new LinkedBlockingQueue<>(500), // 任务队列：队列容量增加到500，支持更多并发任务
                r -> {
                    // 线程工厂：自定义线程创建逻辑
                    // 创建新线程，设置线程名称为"character-pool-v3-"加上当前时间戳，便于调试和监控
                    Thread thread = new Thread(r, "character-pool-v3-" + System.currentTimeMillis());
                    // 设置为守护线程，当JVM中只有守护线程运行时，JVM会自动退出
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：当线程池和队列都满时，由调用者线程直接执行任务
        );
    }

    /**
     * 保存角色卡池记录 - 异步版本
     * 核心优化：移除内存检查，直接利用数据库唯一索引处理重复
     * 该方法使用异步方式处理卡池记录，提高系统吞吐量
     *
     * @param httpServletRequest HTTP请求对象，包含请求相关信息
     * @param uid 用户ID，唯一标识用户
     * @param url API URL，包含token和server_id等参数
     * @return CompletableFuture<BatchProcessResult> 异步处理结果，包含处理统计信息
     */
    @Async("asyncExecutor") // 使用自定义的异步线程池执行此方法
    public CompletableFuture<BatchProcessResult> saveCharacterPoolRecordAsync(HttpServletRequest httpServletRequest, String uid, String url) {
        // 记录任务开始时间，用于计算处理耗时
        long startTime = System.currentTimeMillis();
        // 增加活跃任务计数器，用于监控系统负载
        activeTasks.incrementAndGet();
        
        try {
            // 记录任务开始日志，包含用户ID和URL信息
            logger.info("开始处理角色抽卡记录导入任务:  用户: {}", uid);
            
            // 参数校验：检查用户ID是否为空
            if (uid == null) {
                throw new ServiceException(ResultCode.USER_NOT_EXIST);
            }

            // 解析URL参数，提取token和server_id等信息
            Map<String, String> urlParams = UrlParser.parseEndfieldGachaUrl(url);
            // 从URL参数中获取服务器ID
            String serverId = urlParams.get("server_id");
            
            // 并发处理所有池类型：为每个卡池类型创建异步任务
            List<CompletableFuture<List<CharacterPoolRecord>>> futures = new ArrayList<>();
            
            // 遍历所有卡池类型，为每种类型创建异步处理任务
            for (String poolType : POOL_TYPES) {
                // 使用CompletableFuture异步执行池类型处理
                CompletableFuture<List<CharacterPoolRecord>> future = CompletableFuture
                        .supplyAsync(() -> processPoolType(urlParams, poolType, uid, serverId), asyncExecutor);
                // 将异步任务添加到列表中
                futures.add(future);
            }

            // 等待所有池类型处理完成：使用CompletableFuture.allOf等待所有任务完成
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            
            // 合并所有池类型的结果：在所有任务完成后，收集各个任务的结果
            List<CharacterPoolRecord> allRecords = allFutures.thenApply(v -> {
                List<CharacterPoolRecord> result = new ArrayList<>();
                // 遍历所有异步任务，获取处理结果
                for (CompletableFuture<List<CharacterPoolRecord>> future : futures) {
                    try {
                        // 获取单个任务的结果并合并到总结果中
                        result.addAll(future.get());
                    } catch (Exception e) {
                        // 记录获取结果失败的错误日志
                        logger.error("获取处理结果失败", e);
                    }
                }
                return result;
            }).join(); // 阻塞等待所有任务完成

            // 批量插入数据库 - 利用数据库唯一约束：将收集到的所有记录批量插入数据库
            BatchProcessResult result = batchInsertWithUniqueIndex(allRecords, uid);
            
            // 计算处理耗时
            long duration = System.currentTimeMillis() - startTime;
            // 记录任务完成日志，包含处理统计信息
            logger.info("任务完成: {}, 耗时: {}ms, 新增: {}, 重复: {}, 失败: {}", 
                    uid, duration, result.getSuccessCount(), result.getDuplicatedCount(), result.getFailedCount());
            
            // 返回异步处理结果
            return CompletableFuture.completedFuture(result);
            
        } catch (Exception e) {
            // 处理异常情况：记录错误日志并返回错误结果
            logger.error("处理任务失败: {}", uid, e);
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

    /**
     * 同步版本 - 适用于测试和小批量数据
     * 该方法提供同步调用方式，内部调用异步版本并等待结果
     * 适用于需要立即获取结果的场景，如单元测试或小批量数据处理
     *
     * @param httpServletRequest HTTP请求对象，包含请求相关信息
     * @param uid 用户ID，唯一标识用户
     * @param url API URL，包含token和server_id等参数
     * @return BatchProcessResult 同步处理结果，包含处理统计信息
     */
    public BatchProcessResult saveCharacterPoolRecordSync(HttpServletRequest httpServletRequest, String uid, String url) {
        // 调用异步版本并立即等待结果，实现同步调用效果
        return saveCharacterPoolRecordAsync(httpServletRequest, uid, url).join();
    }

    /**
     * 获取实时性能统计
     * 该方法返回当前服务的性能监控数据，用于系统监控和调试
     *
     * @return PerformanceStats 性能统计对象，包含各项监控指标
     */
    public PerformanceStats getPerformanceStats() {
        // 创建性能统计对象
        PerformanceStats stats = new PerformanceStats();
        // 设置总处理记录数：从原子计数器获取当前值
        stats.setTotalProcessedRecords(totalProcessedRecords.get());
        // 设置总重复记录数：从原子计数器获取当前值
        stats.setTotalDuplicatedRecords(totalDuplicatedRecords.get());
        // 设置总失败记录数：从原子计数器获取当前值
        stats.setTotalFailedRecords(totalFailedRecords.get());
        // 设置活跃任务数：从原子计数器获取当前值
        stats.setActiveTasks(activeTasks.get());
        // 设置当前时间：记录统计生成的时间点
        stats.setCurrentTime(new Date());
        // 返回完整的性能统计对象
        return stats;
    }

    /**
     * 处理单个池类型的数据
     * 简化逻辑：移除所有内存检查，直接返回所有记录
     * 该方法负责处理特定卡池类型的所有记录，通过分页方式获取完整数据
     *
     * @param urlParams URL参数映射，包含token和server_id等信息
     * @param poolType 卡池类型，如特殊池、标准池、新手池
     * @param uid 用户ID，唯一标识用户
     * @param serverId 服务器ID，标识用户所在的服务器
     * @return List<CharacterPoolRecord> 处理后的角色卡池记录列表
     */
    private List<CharacterPoolRecord> processPoolType(Map<String, String> urlParams, String poolType, String uid, String serverId) {
        // 创建记录列表，用于存储处理结果
        List<CharacterPoolRecord> records = new ArrayList<>();
        
        try {
            // 分页控制变量：标识是否还有更多数据需要获取
            Boolean hasMore = true;
            // 序列ID：用于分页请求，初始为null表示从第一页开始
            String poolSeqId = null;
            // 页面计数器：限制最大页数，防止无限循环
            int pageCount = 0;
            
            // 循环获取所有分页数据，直到没有更多数据或达到页数限制
            while (hasMore && pageCount < 30) { // 限制页数防止无限循环，最大30页
                // 将poolSeqId标记为final，以便在lambda表达式中使用
                String finalPoolSeqId = poolSeqId;
                // 使用重试机制调用API，获取卡池记录数据
                CharacterPoolRecordResponseDTO responseDTO = retryExecute(() ->
                    requestCharacterPoolRecordAPI(urlParams, poolType, finalPoolSeqId)
                );
                
                // 检查API响应是否有效
                if (responseDTO == null || responseDTO.getData() == null) {
                    break; // 响应无效，退出循环
                }
                
                // 更新是否有更多数据的标志
                hasMore = responseDTO.getData().getHasMore();
                // 获取当前页的记录列表
                List<CharacterPoolRecordDTO> recordDTOList = responseDTO.getData().getList();
                
                // 检查记录列表是否为空
                if (recordDTOList.isEmpty()) {
                    break; // 没有记录，退出循环
                }
                
                // 遍历当前页的所有记录，转换为实体对象并添加到结果列表
                for (CharacterPoolRecordDTO dto : recordDTOList) {
                    // 将DTO转换为实体对象
                    CharacterPoolRecord record = convertToEntity(dto, uid, serverId, poolType);
                    // 添加到结果列表
                    records.add(record);
                    // 更新序列ID，用于下一次分页请求
                    poolSeqId = dto.getSeqId();
                }
                
                // 增加页面计数
                pageCount++;
                
                // 添加小延迟避免API限制：如果还有更多数据，等待一段时间再请求下一页
                if (hasMore) {
                    Thread.sleep(API_DELAY_MS);
                }
                
            }
            
        } catch (Exception e) {
            // 记录处理失败的错误日志，但不中断整个流程
            logger.error("处理池类型失败: {}", poolType, e);
        }
        
        // 返回处理完成的记录列表
        return records;
    }

    /**
     * 批量插入记录，利用数据库唯一索引自动处理重复
     * 核心优化：移除复杂的内存检查，让数据库处理重复
     * 该方法将记录分批插入数据库，利用数据库的唯一约束自动处理重复记录
     *
     * @param records 需要插入的角色卡池记录列表
     * @param uid 用户ID，用于日志记录和错误处理
     * @return BatchProcessResult 批量处理结果，包含成功、重复、失败的记录统计
     */
    @Transactional // 开启事务管理，确保批量操作的原子性
    public BatchProcessResult batchInsertWithUniqueIndex(List<CharacterPoolRecord> records, String uid) {
        // 创建处理结果对象
        BatchProcessResult result = new BatchProcessResult();
        
        // 检查记录列表是否为空
        if (records.isEmpty()) {
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
        for (int i = 0; i < records.size(); i += BATCH_SIZE) {
            // 计算当前批次的结束索引
            int endIndex = Math.min(i + BATCH_SIZE, records.size());
            // 获取当前批次的数据子列表
            List<CharacterPoolRecord> batch = records.subList(i, endIndex);
            
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
                logger.debug("批处理插入完成: {}/{}", successCount, records.size());
                
            } catch (Exception e) {
                // 批量插入失败：记录警告日志，包含批次范围和错误原因
                logger.warn("批处理插入遇到重复数据或其他错误: {}-{}, 原因: {}", i, endIndex, e.getMessage());
                
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
                            logger.debug("检测到重复记录: {}-{}", record.getPoolName(), record.getSeqId());
                        } else {
                            // 其他错误处理：增加失败计数器
                            failedCount++;
                            // 更新总失败记录计数器
                            totalFailedRecords.incrementAndGet();
                            // 添加错误信息到列表
                            errorMessages.add("记录插入失败: " + record.getSeqId() + " - " + singleError.getMessage());
                            // 记录错误日志：显示失败记录和错误详情
                            logger.error("单条记录插入失败: {}", record.getSeqId(), singleError);
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
     * 带重试的API请求
     * 该方法提供重试机制，在API请求失败时自动重试指定次数
     * 使用指数退避策略，每次重试等待时间逐渐增加
     *
     * @param supplier API请求的供应商函数，返回CharacterPoolRecordResponseDTO
     * @return CharacterPoolRecordResponseDTO API响应数据，如果所有重试都失败则抛出异常
     */
    private CharacterPoolRecordResponseDTO retryExecute(Supplier<CharacterPoolRecordResponseDTO> supplier) {
        // 循环重试指定次数
        for (int i = 0; i < RETRY_COUNT; i++) {
            try {
                // 执行API请求并返回结果
                return supplier.get();
            } catch (Exception e) {
                // 检查是否为最后一次重试
                if (i == RETRY_COUNT - 1) {
                    // 最后一次重试失败，记录错误日志并抛出异常
                    logger.error("API请求最终失败", e);
                    throw new RuntimeException("API请求失败", e);
                }
                
                // 非最后一次重试，等待一段时间后继续重试
                try {
                    // 使用指数退避策略：等待时间 = 基础延迟 * (重试次数 + 1)
                    Thread.sleep(RETRY_DELAY_MS * (i + 1));
                } catch (InterruptedException ie) {
                    // 线程被中断，恢复中断状态并抛出异常
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("线程被中断", ie);
                }
            }
        }
        // 理论上不会执行到这里，因为最后一次重试失败会抛出异常
        return null;
    }

    /**
     * 转化DTO类为实体类
     * 该方法将API返回的数据传输对象转换为数据库实体对象
     * 负责数据格式的转换和字段映射
     *
     * @param dto API返回的角色卡池记录数据传输对象
     * @param uid 用户ID，用于标识记录所属用户
     * @param serverId 服务器ID，标识用户所在的服务器
     * @param poolType 卡池类型，如特殊池、标准池、新手池
     * @return CharacterPoolRecord 转换后的数据库实体对象
     */
    private CharacterPoolRecord convertToEntity(CharacterPoolRecordDTO dto, String uid, String serverId, String poolType) {
        // 创建新的实体对象
        CharacterPoolRecord record = new CharacterPoolRecord();
        // 设置Endfield用户ID，当前为空字符串
        record.setEndfieldUid("");
        // 设置用户ID
        record.setUid(uid);
        // 设置卡池ID，从DTO中获取
        record.setPoolId(dto.getPoolId());
        // 设置卡池名称，从DTO中获取
        record.setPoolName(dto.getPoolName());
        // 设置角色ID，从DTO中获取
        record.setCharId(dto.getCharId());
        // 设置角色名称，从DTO中获取
        record.setCharName(dto.getCharName());
        // 设置角色稀有度，从DTO中获取
        record.setRarity(dto.getRarity());
        // 设置是否为免费抽卡，从DTO中获取
        record.setIsFree(dto.getIsFree());
        // 设置是否为新角色，从DTO中获取
        record.setIsNew(dto.getIsNew());
        // 设置抽卡时间戳，从DTO中获取
        record.setGachaTs(dto.getGachaTs());
        // 设置序列ID，用于分页和唯一标识，从DTO中获取
        record.setSeqId(dto.getSeqId());
        // 设置语言，使用常量LANG（zh-cn）
        record.setLang(LANG);
        // 设置卡池类型
        record.setPoolType(poolType);
        // 设置服务器ID
        record.setServerId(serverId);
        // 返回完整的实体对象
        return record;
    }

    /**
     * 请求API - 保持原有逻辑
     * 该方法负责构造API请求URL和头部信息，发送HTTP请求并解析响应
     * 模拟浏览器行为，避免被服务器识别为爬虫
     *
     * @param urlParams URL参数映射，包含token和server_id等信息
     * @param poolType 卡池类型，如特殊池、标准池、新手池
     * @param seqId 序列ID，用于分页请求，null表示第一页
     * @return CharacterPoolRecordResponseDTO API响应数据
     */
    private CharacterPoolRecordResponseDTO requestCharacterPoolRecordAPI(Map<String, String> urlParams, String poolType, String seqId) {
        // 从URL参数中获取token
        String token = urlParams.get("token");
        // 从URL参数中获取服务器ID
        String server_id = urlParams.get("server_id");
        
        // 对token进行URL编码，确保特殊字符正确处理
        String encodeToken = smartUrlEncode(token);
        // 构造完整的API请求URL
        String url = CHARACTER_RECORD_API + "?lang=" + LANG + "&pool_type=" + poolType + "&token=" + encodeToken + "&server_id=" + server_id;
        // 如果提供了序列ID，添加到URL中用于分页
        if (seqId != null) {
            url += "&seq_id=" + seqId;
        }
        
        try {
            // 构造HTTP请求头部信息，模拟浏览器行为
            Map<String, String> headers = new HashMap<>();
            // 设置User-Agent，模拟Firefox浏览器
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:146.0) Gecko/20100101 Firefox/146.0");
            // 设置Accept头部，接受JSON格式响应
            headers.put("Accept", "application/json, text/plain, */*");
            // 设置语言偏好，优先中文
            headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
            // 设置编码方式
            headers.put("Accept-Encoding", "gzip, deflate, br, zstd");
            // 保持连接
            headers.put("Connection", "keep-alive");
            // 安全相关头部
            headers.put("Sec-Fetch-Dest", "empty");
            headers.put("Sec-Fetch-Mode", "cors");
            headers.put("Sec-Fetch-Site", "same-origin");
            headers.put("Priority", "u=0");
            // 设置Host头部
            headers.put("Host", "endfield.hypergryph.com");
            
            // 发送HTTP GET请求，获取响应内容
            String response = OkHttpUtil.getWithHeaders(url, headers);
            // 创建JSON对象映射器
            ObjectMapper objectMapper = new ObjectMapper();
            // 将JSON响应字符串转换为Java对象
            return objectMapper.readValue(response, CharacterPoolRecordResponseDTO.class);
            
        } catch (IOException e) {
            // 记录API请求失败的错误日志
            logger.error("API请求失败: {}", url, e);
            // 抛出运行时异常，由上层重试机制处理
            throw new RuntimeException("API请求失败", e);
        }
    }

    /**
     * 获取用户角色卡池记录（原有接口保持不变）
     * 该方法根据用户ID查询数据库中该用户的所有卡池记录
     * 使用MyBatis Plus的Lambda查询方式，提高代码可读性
     *
     * @param httpServletRequest HTTP请求对象，当前方法未使用但保持接口一致性
     * @param uid 用户ID，用于查询该用户的卡池记录
     * @return List<CharacterPoolRecord> 用户的所有角色卡池记录列表
     */
    public List<CharacterPoolRecord> getCharacterPoolRecordData(HttpServletRequest httpServletRequest, String uid) {
        // 创建Lambda查询包装器，用于构建类型安全的查询条件
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CharacterPoolRecord> queryWrapper = 
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        // 设置查询条件：用户ID等于指定值
        queryWrapper.eq(CharacterPoolRecord::getUid, uid);
        // 执行查询并返回结果列表
        return characterPoolRecordMapper.selectList(queryWrapper);
    }

    /**
     * 关闭资源
     * 该方法用于优雅关闭异步线程池，释放系统资源
     * 在应用关闭时调用，确保所有任务都已完成或超时
     */
    public void shutdown() {
        // 检查线程池是否存在且未关闭
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            // 发起关闭请求，不再接受新任务
            asyncExecutor.shutdown();
            try {
                // 等待线程池中的任务完成，最多等待60秒
                if (!asyncExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    // 如果超时，强制关闭所有正在执行的任务
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                // 线程被中断，强制关闭线程池
                asyncExecutor.shutdownNow();
                // 恢复中断状态
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