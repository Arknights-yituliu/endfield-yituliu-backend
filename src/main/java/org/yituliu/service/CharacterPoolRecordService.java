package org.yituliu.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.yituliu.common.enums.ResultCode;
import org.yituliu.common.exception.ServiceException;
import org.yituliu.common.utils.IdGenerator;
import org.yituliu.common.utils.JsonMapper;
import org.yituliu.common.utils.LogUtils;
import org.yituliu.entity.dto.pool.record.CharacterPoolRecordDTO;
import org.yituliu.entity.dto.pool.record.CharacterPoolRecordResponseDTO;
import org.yituliu.entity.log.BatchProcessResult;
import org.yituliu.entity.po.CharacterPoolRecord;
import org.yituliu.entity.dto.pool.record.EndfieldUserInfoDTO;
import org.yituliu.entity.po.EndministratorInfo;
import org.yituliu.entity.po.PlayerPoolRecordTask;
import org.yituliu.mapper.CharacterPoolRecordMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.yituliu.common.utils.OkHttpUtil;
import org.yituliu.mapper.EndministratorInfoMapper;
import org.yituliu.mapper.PlayerPoolRecordTaskMapper;


import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.yituliu.common.utils.UrlEncodeUtil.smartUrlEncode;

@Service
public class CharacterPoolRecordService {


    private static final Set<String> POOL_TYPES = Set.of(
            "E_CharacterGachaPoolType_Special",
            "E_CharacterGachaPoolType_Standard",
            "E_CharacterGachaPoolType_Beginner"
    );

    private static final int BATCH_SIZE = 200; // 批处理大小

    private static final int RETRY_COUNT = 3; // 重试次数
    private static final long RETRY_DELAY_MS = 1000; // 重试延迟

    //    private final String CHARACTER_RECORD_API = "http://127.0.0.1:10010/character_pool_record";

    private final String LANG = "zh-cn";

    private final String SERVER_ID = "1";
    private final String SERVER = "1";

    //以上为常量部分-------------------------------------------
    private final CharacterPoolRecordMapper characterPoolRecordMapper;
    private final IdGenerator idGenerator;
    private final ThreadPoolExecutor asyncExecutor;
    private final PlayerPoolRecordTaskMapper playerPoolRecordTaskMapper;
    private final EndministratorInfoMapper endministratorInfoMapper;

    // 性能监控相关计数器
    // 总处理记录数：原子长整型，线程安全地记录已处理的记录总数
    private final AtomicLong totalProcessedRecords = new AtomicLong(0);
    // 总重复记录数：原子长整型，线程安全地记录检测到的重复记录总数
    private final AtomicLong totalDuplicatedRecords = new AtomicLong(0);
    // 总失败记录数：原子长整型，线程安全地记录处理失败的记录总数
    private final AtomicLong totalFailedRecords = new AtomicLong(0);
    // 活跃任务数：原子整型，线程安全地记录当前正在执行的任务数量
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    public CharacterPoolRecordService(CharacterPoolRecordMapper characterPoolRecordMapper, PlayerPoolRecordTaskMapper playerPoolRecordTaskMapper, EndministratorInfoMapper endministratorInfoMapper) {
        this.characterPoolRecordMapper = characterPoolRecordMapper;
        this.playerPoolRecordTaskMapper = playerPoolRecordTaskMapper;
        this.endministratorInfoMapper = endministratorInfoMapper;
        this.idGenerator = new IdGenerator(1L);

        // 配置线程池
        this.asyncExecutor = new ThreadPoolExecutor(
                4, // 核心线程数
                8, // 最大线程数
                30L, // 空闲线程存活时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000), // 队列容量
                new ThreadFactoryBuilder().setNameFormat("character-pool-async-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略：使用调用者运行
        );
    }

    public String createTask(HttpServletRequest httpServletRequest,String hgToken){
        PlayerPoolRecordTask playerPoolRecordTask = new PlayerPoolRecordTask();
        playerPoolRecordTask.setToken(hgToken);
        playerPoolRecordTask.setStartFlag(false);
        playerPoolRecordTask.setCompleteFlag(false);
        playerPoolRecordTask.setCreateTime(new Date());
        String taskId = "task"+idGenerator.nextId();
        playerPoolRecordTask.setTaskId(taskId);
        playerPoolRecordTaskMapper.insert(playerPoolRecordTask);
        return taskId;
    }

    public EndministratorInfo checkTask(String taskId){
        PlayerPoolRecordTask playerPoolRecordTask = playerPoolRecordTaskMapper.selectById(taskId);
        if(playerPoolRecordTask.getCompleteFlag()){
            String roleId = playerPoolRecordTask.getRoleId();
            LambdaQueryWrapper<EndministratorInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(EndministratorInfo::getRoleId,roleId);
            return endministratorInfoMapper.selectOne(queryWrapper);
        }

        throw new ServiceException(ResultCode.TASK_NOT_COMPLETED);

    }

    /**
     * 高并发处理角色卡池记录
     * 支持批量异步处理，优化数据库操作
     */
    @Async("asyncExecutor")
    public void saveCharacterPoolRecordAsync(PlayerPoolRecordTask playerPoolRecordTask) {
        // 记录任务开始时间，用于计算处理耗时

        long startTime = System.currentTimeMillis();
        // 记录任务开始日志，包含用户ID和URL信息

        String logNickName = "";
        String hgToken = playerPoolRecordTask.getToken();

        try {
            HashMap<String, String> map = new HashMap<>();
            map.put("hgToken",hgToken);
            // 尝试使用表单格式发送请求
            String localServiceResponseText = OkHttpUtil.postForm("http://127.0.0.1:10086/token", map);
            LogUtils.info("本地服务响应：{}",localServiceResponseText);
            JsonNode jsonNode = JsonMapper.parseJSONObject(localServiceResponseText);

            EndfieldUserInfoDTO endfieldUserInfoDTO = JsonMapper.parseObject(jsonNode.get("data").toString(), EndfieldUserInfoDTO.class);

            String nickName = endfieldUserInfoDTO.getNickName();
            String uid = endfieldUserInfoDTO.getUid();
            String roleId = endfieldUserInfoDTO.getRoleId();
            String u8Token = endfieldUserInfoDTO.getU8Token();

            logNickName = endfieldUserInfoDTO.getNickName();
            LogUtils.info("开始处理角色抽卡记录导入任务:  用户: {}", roleId);


            ArrayList<CharacterPoolRecord> characterPoolRecordList = new ArrayList<>();


            Integer existingMaxSeqId = characterPoolRecordMapper.getMaxSeqIdNumber(roleId);


            // 并发处理所有池类型：为每个卡池类型创建异步任务
            List<CompletableFuture<CharacterPoolRecordResponseDTO>> futures = new ArrayList<>();

            debuglog("roleId: {} 开始获取三种卡池的初始seqId,耗时: {} ms", roleId, System.currentTimeMillis() - startTime);

            int characterPoolRecordListSize = 10;
            // 遍历所有卡池类型，为每种类型创建异步处理任务
            for (String poolType : POOL_TYPES) {
                CharacterPoolRecordResponseDTO characterPoolRecordResponseDTO = requestCharacterPoolRecordAPI(u8Token, poolType, null);
                List<CharacterPoolRecordDTO> characterPoolRecordDTOList = characterPoolRecordResponseDTO.getData().getList();
                characterPoolRecordDTOList.forEach(e -> {
                    characterPoolRecordList.add(convertToEntity(e, roleId, SERVER_ID, poolType));
                });
                characterPoolRecordListSize += characterPoolRecordDTOList.size();
                String lastSeqId = characterPoolRecordDTOList.get(characterPoolRecordDTOList.size() - 1).getSeqId();
                debuglog("roleId: {}  {}最大的seq_id: {} 耗时: {} ms", roleId, poolType, lastSeqId, System.currentTimeMillis() - startTime);
                List<String> seqIdList = initSeqIdList(lastSeqId, existingMaxSeqId);
                characterPoolRecordListSize += seqIdList.size() * 5;
                for (String seqId : seqIdList) {
                    CompletableFuture<CharacterPoolRecordResponseDTO> future = CompletableFuture
                            .supplyAsync(() -> requestCharacterPoolRecordAPI(u8Token, poolType, seqId), asyncExecutor);
                    futures.add(future);
                }
            }

            debuglog("roleId: {} 三种卡池的初始seqId获取完毕,耗时: {} ms", roleId, System.currentTimeMillis() - startTime);

            // 预分配列表容量，避免频繁扩容
            characterPoolRecordList.ensureCapacity(characterPoolRecordListSize);

            // 等待所有池类型处理完成：使用CompletableFuture.allOf等待所有任务完成
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));


            debuglog("roleId: {} 开始执行异步请求 耗时: {} ms", roleId, System.currentTimeMillis() - startTime);

            /*
              处理所有异步任务的结果并合并到一个列表中
              核心功能：等待所有异步任务完成后，收集并合并所有结果
             */
            allFutures
                    // thenApply：当所有异步任务完成后执行的回调函数
                    // v参数：allFutures完成后的结果（此处为void，因为是allOf）
                    .thenApply(v -> {
                        // 创建用于存储所有结果的列表


                        // 遍历所有异步任务的future对象
                        for (CompletableFuture<CharacterPoolRecordResponseDTO> future : futures) {
                            try {
                                // future.get()：阻塞获取单个异步任务的结果
                                // 这里每个future的结果是List<CharacterPoolRecordDTO>类型
                                CharacterPoolRecordResponseDTO futureResult = future.get();
                                if (0 != futureResult.getCode()) {
                                    continue;
                                }
                                List<CharacterPoolRecordDTO> characterPoolRecordDTOList = futureResult.getData().getList();
                                // 将单个任务的结果合并到总结果列表中
                                // 使用普通for循环代替流处理，减少流的开销
                                for (CharacterPoolRecordDTO dto : characterPoolRecordDTOList) {
                                    characterPoolRecordList.add(convertToEntity(dto, roleId, SERVER_ID, futureResult.getPoolType()));
                                }
                                debuglog("roleId: {} 单个请求格式化完成 耗时: {} ms", roleId, System.currentTimeMillis() - startTime);
                            } catch (InterruptedException e) {
                                // 处理线程中断异常
                                LogUtils.error("异步任务被中断", e);
                                // 恢复中断状态，以便上层处理
                                Thread.currentThread().interrupt();
                            } catch (ExecutionException e) {
                                // 处理异步任务执行异常
                                LogUtils.error("异步任务执行失败", e.getCause());
                            } catch (Exception e) {
                                // 处理其他未知异常
                                LogUtils.error("获取异步任务结果失败", e);
                            }
                        }

                        // 返回合并后的总结果
                        return characterPoolRecordList.size();
                    })
                    // join()：阻塞等待所有结果处理完成并获取最终结果
                    // 注意：join()与get()类似，但不会抛出受检异常
                    .join();

            debuglog("roleId: {} 异步请求全部完成 耗时: {} ms", roleId, System.currentTimeMillis() - startTime);


            // 批量插入数据库 - 利用数据库唯一约束：将收集到的所有记录批量插入数据库
            batchInsertWithUniqueIndex(characterPoolRecordList);

            playerPoolRecordTask.setCompleteFlag(true);
            playerPoolRecordTask.setRoleId(roleId);

            LambdaQueryWrapper<EndministratorInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(EndministratorInfo::getRoleId,roleId);
            if(!endministratorInfoMapper.exists(queryWrapper)){
                EndministratorInfo endministratorInfo = new EndministratorInfo();
                endministratorInfo.setUid(uid);
                endministratorInfo.setRoleId(roleId);
                endministratorInfo.setNickName(nickName);
                endministratorInfoMapper.insert(endministratorInfo);
            }

            playerPoolRecordTaskMapper.updateById(playerPoolRecordTask);

            debuglog("roleId: {} 全部数据插入数据 耗时: {} ms", roleId, System.currentTimeMillis() - startTime);




        } catch (Exception e) {
            // 处理异常情况：记录错误日志并返回错误结果
            LogUtils.error("处理任务失败: {}", logNickName, e);
            // 增加失败记录计数器
            totalFailedRecords.incrementAndGet();

        } finally {
            // 减少活跃任务计数器，确保计数器正确释放
            activeTasks.decrementAndGet();
        }
    }


    /**
     * 获取用户角色卡池记录（同步版本）
     */
    public List<CharacterPoolRecord> getCharacterPoolRecordData(HttpServletRequest httpServletRequest, String taskId) {
        PlayerPoolRecordTask playerPoolRecordTask = playerPoolRecordTaskMapper.selectById(taskId);
        if(playerPoolRecordTask==null){
            throw new ServiceException(ResultCode.TOKEN_EXPIRATION);
        }
        LambdaQueryWrapper<CharacterPoolRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CharacterPoolRecord::getRoleId, playerPoolRecordTask.getRoleId());
        return characterPoolRecordMapper.selectList(queryWrapper);
    }

    @Scheduled(cron = "* * * * * ?")
    private void loadingPoolRecord(){
        PlayerPoolRecordTask playerPoolRecordTask = playerPoolRecordTaskMapper.selectFirstByStartFlagFalseOrderByCreateTimeDesc();
        if(playerPoolRecordTask==null){
//            LogUtils.error("当前无任务");
            return;
        }
        playerPoolRecordTask.setStartFlag(true);
        playerPoolRecordTask.setUpdateTime(new Date());
        playerPoolRecordTaskMapper.updateById(playerPoolRecordTask);
        saveCharacterPoolRecordAsync(playerPoolRecordTask);
    }

    private void debuglog(String format, Object... arguments) {

        // 记录任务完成日志，包含处理统计信息
        LogUtils.info(format, arguments);
    }

    private BatchProcessResult batchInsertWithUniqueIndex(List<CharacterPoolRecord> characterPoolRecordList) {
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
//                LogUtils.error("批处理插入遇到重复数据或其他错误: {}-{}, 原因: {}", i, endIndex, e.getMessage());

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
//                            LogUtils.info("检测到重复记录: {}-{}", record.getPoolName(), record.getSeqId());
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
            existingSeqId = 0;
        }

        int lastSeqId = Integer.parseInt(lastSeqIdStr.trim());

//        if (lastSeqId < existingSeqId) {
//            return Collections.emptyList();
//        }

        List<String> seqIdList = new ArrayList<>();
        for (int i = lastSeqId; i >= existingSeqId; i -= 5) {
            seqIdList.add(String.valueOf(i));
        }
        return seqIdList;
    }


    /**
     * 请求API
     */
    private CharacterPoolRecordResponseDTO requestCharacterPoolRecordAPI(String u8Token, String poolType, String seqId) {
        // 记录任务开始时间，用于计算处理耗时
        long startTime = System.currentTimeMillis();
        String encodeToken = smartUrlEncode(u8Token);
//        String encodeToken =    token;

        String CHARACTER_RECORD_API = "https://ef-webview.hypergryph.com/api/record/char";
        String url = CHARACTER_RECORD_API + "?lang=" + LANG + "&pool_type=" + poolType + "&token=" + encodeToken + "&server_id=" + SERVER_ID;
        if (seqId != null) {
            url += "&seq_id=" + seqId;
        }

//        // 打印URL中?之后的参数字符串
//        if (url.contains("?")) {
//            String queryString = url.substring(url.indexOf("?") + 1);
//            LogUtils.info("API请求URL参数: {}", queryString);
//        }


        try {
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/json, text/plain, */*");
            headers.put("Accept-Encoding", "gzip, deflate, br, zstd");
            headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
//            headers.put("Cache-Control","no-cache");
//            headers.put("Connection", "keep-alive");
//            headers.put("Cookie","_ga=GA1.2.1904220515.1705644583; _ga_VMLPESWL6R=GS1.1.1737293308.86.1.1737293335.33.0.0");
            headers.put("Host", "ef-webview.hypergryph.com");
//            headers.put("Pragma", "no-cache");
//            headers.put("Priority", "u=0");


            headers.put("Sec-Fetch-Dest", "empty");
            headers.put("Sec-Fetch-Mode", "cors");
            headers.put("Sec-Fetch-Site", "same-origin");
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:146.0) Gecko/20100101 Firefox/146.0");


            String response = OkHttpUtil.getWithHeaders(url, headers);

            //            ObjectMapper objectMapper = new ObjectMapper();
            //            return objectMapper.readValue(response, CharacterPoolRecordResponseDTO.class);

            CharacterPoolRecordResponseDTO characterPoolRecordResponseDTO = JsonMapper.parseObject(response, new TypeReference<>() {
            });



            if (characterPoolRecordResponseDTO == null || characterPoolRecordResponseDTO.getData() == null
                    || characterPoolRecordResponseDTO.getData().getList() == null || characterPoolRecordResponseDTO.getData().getList().isEmpty()) {
                return new CharacterPoolRecordResponseDTO(500, null, "请求终末地角色抽卡记录失败", poolType);
            }


            characterPoolRecordResponseDTO.setPoolType(poolType);
            return characterPoolRecordResponseDTO;
        } catch (IOException e) {
            LogUtils.error("API请求失败: {}", url, e);
            throw new RuntimeException("API请求失败", e);
        }
    }


    /**
     * 转换DTO到实体
     */
    private CharacterPoolRecord convertToEntity(CharacterPoolRecordDTO dto, String roleId, String serverId, String poolType) {
        // 优化：使用全参构造器代替13次set方法调用，减少方法调用开销
        return new CharacterPoolRecord(
                idGenerator.nextId(),  // ID
                roleId,                 // roleId
                dto.getPoolId(),     // poolId
                dto.getPoolName(),   // poolName
                dto.getCharId(),     // charId
                dto.getCharName(),   // charName
                dto.getRarity(),     // rarity
                dto.getIsFree(),     // isFree
                dto.getIsNew(),      // isNew
                dto.getGachaTs(),    // gachaTs
                dto.getSeqId(),      // seqId
                LANG,               // lang
                poolType,           // poolType
                serverId            // serverId
        );
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