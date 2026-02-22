package org.yituliu.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.yituliu.common.enums.ResultCode;
import org.yituliu.common.exception.ServiceException;
import org.yituliu.common.utils.*;
import org.yituliu.entity.dto.pool.record.CharacterPoolRecordDTO;
import org.yituliu.entity.dto.pool.record.CharacterPoolRecordResponseDTO;
import org.yituliu.entity.po.CharacterPoolRecord;
import org.yituliu.entity.dto.pool.record.EndfieldUserInfoDTO;
import org.yituliu.entity.po.EndministratorInfo;
import org.yituliu.entity.po.PlayerPoolRecordTask;
import org.yituliu.mapper.CharacterPoolRecordMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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


    private static String SPECIAL_POOL_TYPE = "E_CharacterGachaPoolType_Special";
    private static String STANDARD_POOL_TYPE = "E_CharacterGachaPoolType_Standard";
    private static String BEGINNER_POOL_TYPE = "E_CharacterGachaPoolType_Beginner";


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
    private final Executor asyncExecutor;


    // 性能监控相关计数器
    // 总处理记录数：原子长整型，线程安全地记录已处理的记录总数
    private final AtomicLong totalProcessedRecords = new AtomicLong(0);
    // 总重复记录数：原子长整型，线程安全地记录检测到的重复记录总数
    private final AtomicLong totalDuplicatedRecords = new AtomicLong(0);
    // 总失败记录数：原子长整型，线程安全地记录处理失败的记录总数
    private final AtomicLong totalFailedRecords = new AtomicLong(0);
    // 活跃任务数：原子整型，线程安全地记录当前正在执行的任务数量
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    public CharacterPoolRecordService(CharacterPoolRecordMapper characterPoolRecordMapper,
                                      @Qualifier("asyncExecutor") Executor asyncExecutor) {
        this.characterPoolRecordMapper = characterPoolRecordMapper;
        this.idGenerator = new IdGenerator(1L);
        this.asyncExecutor = asyncExecutor;  // 赋值给实例变量

    }


    /**
     * 并发处理角色卡池记录
     */

    public void saveCharacterPoolRecordAsync(EndfieldUserInfoDTO endfieldUserInfoDTO) {
        // 记录任务开始时间，用于计算处理耗时
        long startTime = System.currentTimeMillis();

        String nickName = endfieldUserInfoDTO.getNickName();
        String uid = endfieldUserInfoDTO.getUid();
        String roleId = endfieldUserInfoDTO.getRoleId();
        String u8Token = endfieldUserInfoDTO.getU8Token();
        String logNickName = endfieldUserInfoDTO.getNickName();

        LogUtils.info("roleId：{} 开始处理角色抽卡记录导入任务，耗时：{} ms", roleId, System.currentTimeMillis() - startTime);
        //要持久化到数据库的干员寻访记录列表
        ArrayList<CharacterPoolRecord> characterPoolRecordList = new ArrayList<>();
        //查询当前uid的历史干员寻访记录的最大seq_id，减少重复导入的情况
        Integer characterPoolRecordExistingMaxSeqId = characterPoolRecordMapper.getMaxSeqIdNumber(roleId);

        // 并发处理所有池类型：为每个卡池类型创建异步任务
        List<CompletableFuture<CharacterPoolRecordResponseDTO>> characterPoolRecordFutures = new ArrayList<>();


        LambdaQueryWrapper<CharacterPoolRecord> beginnerPoolWrapper = new LambdaQueryWrapper<>();
        beginnerPoolWrapper.eq(CharacterPoolRecord::getRoleId, roleId);
        beginnerPoolWrapper.eq(CharacterPoolRecord::getPoolType, BEGINNER_POOL_TYPE);
        Long beginnerPoolRecordCount = characterPoolRecordMapper.selectCount(beginnerPoolWrapper);

        int characterPoolRecordListSize = 10;
        // 创建 List
        List<String> poolTypes = new ArrayList<>();
        poolTypes.add(STANDARD_POOL_TYPE);
        poolTypes.add(SPECIAL_POOL_TYPE);
        if (beginnerPoolRecordCount < 40) {
            poolTypes.add(BEGINNER_POOL_TYPE);
        }

        try {
            LogUtils.info("roleId：{} 开始创建干员卡池异步请求任务，耗时：{} ms", roleId, System.currentTimeMillis() - startTime);
            // 遍历所有卡池类型，为每种类型创建异步处理任务
            for (String poolType : poolTypes) {
                //不传入seq_id，获取第一页寻访记录的最后一个seq_id
                CharacterPoolRecordResponseDTO characterPoolRecordResponseDTO =
                        requestCharacterPoolRecordAPI(u8Token, poolType, null);

                List<CharacterPoolRecordDTO> characterPoolRecordDTOList =
                        characterPoolRecordResponseDTO.getData().getList();

                //将获取到寻访记录插入到持久化对象集合中
                characterPoolRecordDTOList.forEach(e -> {
                    characterPoolRecordList.add(convertToEntity(e, roleId, SERVER_ID, poolType));
                });

                //持久化对象集合的长度增加
                characterPoolRecordListSize += characterPoolRecordDTOList.size();

                //获取第一页寻访记录的最后一个seq_id
                String lastSeqId = characterPoolRecordDTOList
                        .get(characterPoolRecordDTOList.size() - 1)
                        .getSeqId();

                LogUtils.info("roleId：{} ，{}最大的seq_id：{}，耗时：{} ms",
                        roleId, poolType, lastSeqId, System.currentTimeMillis() - startTime);

                //建一个集合，元素为seq_id每次递减5，用于接下来创建查询请求队列
                List<String> seqIdList = PoolRecordTaskUtil
                        .initSeqIdList(lastSeqId, characterPoolRecordExistingMaxSeqId);
                //持久化对象集合的长度增加
                characterPoolRecordListSize += seqIdList.size() * 5;
                //创建查询请求队列
                for (String seqId : seqIdList) {
                    CompletableFuture<CharacterPoolRecordResponseDTO> future = CompletableFuture
                            .supplyAsync(() ->
                                    requestCharacterPoolRecordAPI(u8Token, poolType, seqId), asyncExecutor);
                    characterPoolRecordFutures.add(future);
                }
            }

            LogUtils.info("roleId：{} 干员卡池异步请求任务创建完成，耗时：{} ms", roleId, System.currentTimeMillis() - startTime);
            // 预分配列表容量，避免频繁扩容
            characterPoolRecordList.ensureCapacity(characterPoolRecordListSize);
            // 等待所有池类型处理完成：使用CompletableFuture.allOf等待所有任务完成
            CompletableFuture<Void> allCharacterPoolRecordFutures = CompletableFuture.allOf(characterPoolRecordFutures.toArray(new CompletableFuture[0]));
            LogUtils.info("roleId：{} 开始执行干员卡池异步请求任务，耗时：{} ms", roleId, System.currentTimeMillis() - startTime);

            /*
              处理所有异步任务的结果并合并到一个列表中
              核心功能：等待所有异步任务完成后，收集并合并所有结果
             */
            allCharacterPoolRecordFutures
                    // thenApply：当所有异步任务完成后执行的回调函数
                    // v参数：allFutures完成后的结果（此处为void，因为是allOf）
                    .thenApply(v -> {
                        // 创建用于存储所有结果的列表
                        // 遍历所有异步任务的future对象
                        for (CompletableFuture<CharacterPoolRecordResponseDTO> future : characterPoolRecordFutures) {
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

                            } catch (InterruptedException e) {
                                // 处理线程中断异常
                                LogUtils.error("roleId：{} 异步任务被中断", roleId, e);
                                // 恢复中断状态，以便上层处理
                                Thread.currentThread().interrupt();
                            } catch (ExecutionException e) {
                                // 处理异步任务执行异常
                                LogUtils.error("roleId：{} 异步任务执行失败", roleId, e.getCause());
                            } catch (Exception e) {
                                // 处理其他未知异常
                                LogUtils.error("roleId：{} 获取异步任务结果失败", roleId, e);
                            }
                        }

                        // 返回合并后的总结果
                        return characterPoolRecordList.size();
                    })
                    // join()：阻塞等待所有结果处理完成并获取最终结果
                    // 注意：join()与get()类似，但不会抛出受检异常
                    .join();

            LogUtils.info("roleId：{} 异步请求全部完成，耗时：{} ms", roleId, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            // 处理异常情况：记录错误日志并返回错误结果
            LogUtils.error("roleId：{} 处理任务失败: {}", roleId, logNickName, e);
            // 增加失败记录计数器
            totalFailedRecords.incrementAndGet();

        } finally {
            // 减少活跃任务计数器，确保计数器正确释放
            activeTasks.decrementAndGet();
        }

        LogUtils.info("roleId: {} 开始插入干员寻访记录数据到数据库 耗时: {} ms", roleId, System.currentTimeMillis() - startTime);
        // 批量插入数据库 - 利用数据库唯一约束：将收集到的所有记录批量插入数据库
        batchInsertWithUniqueIndex(characterPoolRecordList);
        LogUtils.info("roleId：{} 干员寻访记录数据全部插入 耗时: {} ms", roleId, System.currentTimeMillis() - startTime);
    }


    public List<CharacterPoolRecord> getCharacterPoolRecordByTaskId(String roleId){

        LambdaQueryWrapper<CharacterPoolRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CharacterPoolRecord::getRoleId, roleId);
        return characterPoolRecordMapper.selectList(queryWrapper);
    }


    private void batchInsertWithUniqueIndex(List<CharacterPoolRecord> characterPoolRecordList) {
        // 创建处理结果对象

        // 检查记录列表是否为空
        if (characterPoolRecordList.isEmpty()) {
            // 设置空结果并返回

            return;
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
                        if (PoolRecordTaskUtil.isDuplicateKeyError(singleError)) {
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

        LogUtils.info("插入完成 - 新增: {}条, 重复: {}条, 失败: {}条", successCount, duplicatedCount, failedCount);
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

        Map<String, String> headers = PoolRecordTaskUtil.getHeader();

        try {
            String response = OkHttpUtil.getWithHeaders(url, headers);
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


}