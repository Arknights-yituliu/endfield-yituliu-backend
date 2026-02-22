package org.yituliu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.yituliu.common.utils.*;
import org.yituliu.entity.dto.pool.record.*;
import org.yituliu.entity.po.WeaponPoolRecord;
import org.yituliu.mapper.WeaponPoolRecordMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.yituliu.common.utils.UrlEncodeUtil.smartUrlEncode;

@Service
public class WeaponPoolRecordService {
    private static final int BATCH_SIZE = 200; // 批处理大小

    private final String LANG = "zh-cn";

    private final String SERVER_ID = "1";
    private final String SERVER = "1";

    private final WeaponPoolRecordMapper weaponPoolRecordMapper;
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

    public WeaponPoolRecordService(WeaponPoolRecordMapper weaponPoolRecordMapper,
                                   @Qualifier("asyncExecutor") Executor asyncExecutor) {
        this.weaponPoolRecordMapper = weaponPoolRecordMapper;
        this.idGenerator = new IdGenerator(1L);
        this.asyncExecutor = asyncExecutor;  // 赋值给实例变量

    }

    @Async("asyncExecutor")
    public void saveWeaponPoolRecordAsync(EndfieldUserInfoDTO endfieldUserInfoDTO) {

        // 记录任务开始时间，用于计算处理耗时
        long startTime = System.currentTimeMillis();

        String roleId = endfieldUserInfoDTO.getRoleId();
        String u8Token = endfieldUserInfoDTO.getU8Token();
        String logNickName = endfieldUserInfoDTO.getNickName();

        Integer weaponPoolRecordMaxSeqId = weaponPoolRecordMapper.getMaxSeqIdNumber(roleId);
        //要持久化到数据库的干员寻访记录列表
        ArrayList<WeaponPoolRecord> weaponPoolRecordArrayList = new ArrayList<>();

        LogUtils.info("roleId：{} 开始处理武器抽卡记录导入任务，耗时：{} ms", roleId, System.currentTimeMillis() - startTime);

        // 并发处理所有池类型：为每个卡池类型创建异步任务
        List<CompletableFuture<WeaponPoolRecordResponseDTO>> weaponPoolRecordFutures = new ArrayList<>();

        LogUtils.info("roleId：{} 开始创建武器卡池异步请求任务，耗时：{} ms", roleId, System.currentTimeMillis() - startTime);

        int weaponPoolRecordListSize = 10;


        try {
            //不传入seq_id，获取第一页寻访记录的最后一个seq_id
            WeaponPoolRecordResponseDTO weaponPoolRecordResponseDTO = requestWeaponPoolRecordAPI(u8Token, null);

            List<WeaponPoolRecordDTO> weaponPoolRecordDTOList =
                    weaponPoolRecordResponseDTO.getData().getList();

            //将获取到寻访记录插入到持久化对象集合中
            weaponPoolRecordDTOList.forEach(e -> weaponPoolRecordArrayList.add(convertToEntity(e, roleId, SERVER_ID)));

            //持久化对象集合的长度增加
            weaponPoolRecordListSize += weaponPoolRecordDTOList.size();

            //获取第一页寻访记录的最后一个seq_id
            String lastSeqId = weaponPoolRecordDTOList
                    .get(weaponPoolRecordDTOList.size() - 1)
                    .getSeqId();

            LogUtils.info("roleId：{} ，最大的seq_id：{}，耗时：{} ms",
                    roleId, lastSeqId, System.currentTimeMillis() - startTime);

            //建一个集合，元素为seq_id每次递减5，用于接下来创建查询请求队列
            List<String> seqIdList = PoolRecordTaskUtil
                    .initSeqIdList(lastSeqId, weaponPoolRecordMaxSeqId);

            //持久化对象集合的长度增加
            weaponPoolRecordListSize += seqIdList.size() * 5;
            for (String seqId : seqIdList) {
                CompletableFuture<WeaponPoolRecordResponseDTO> future = CompletableFuture
                        .supplyAsync(() ->
                                requestWeaponPoolRecordAPI(u8Token, seqId), asyncExecutor);
                weaponPoolRecordFutures.add(future);
            }

            LogUtils.info("roleId：{} 武器卡池异步请求任务创建完成，耗时：{} ms", roleId, System.currentTimeMillis() - startTime);

            // 预分配列表容量，避免频繁扩容
            weaponPoolRecordArrayList.ensureCapacity(weaponPoolRecordListSize);
            //使用CompletableFuture.allOf等待所有任务完成
            CompletableFuture<Void> allWeaponPoolRecordFutures = CompletableFuture.allOf(weaponPoolRecordFutures.toArray(new CompletableFuture[0]));
            LogUtils.info("roleId：{} 开始执行武器卡池异步请求任务，耗时：{} ms", roleId, System.currentTimeMillis() - startTime);
  /*
              处理所有异步任务的结果并合并到一个列表中
              核心功能：等待所有异步任务完成后，收集并合并所有结果
             */
            allWeaponPoolRecordFutures
                    // thenApply：当所有异步任务完成后执行的回调函数
                    // v参数：allFutures完成后的结果（此处为void，因为是allOf）
                    .thenApply(v -> {
                        // 创建用于存储所有结果的列表
                        // 遍历所有异步任务的future对象
                        for (CompletableFuture<WeaponPoolRecordResponseDTO> future : weaponPoolRecordFutures) {
                            try {
                                // future.get()：阻塞获取单个异步任务的结果
                                // 这里每个future的结果是List<CharacterPoolRecordDTO>类型
                                WeaponPoolRecordResponseDTO futureResult = future.get();
                                if (0 != futureResult.getCode()) {
                                    continue;
                                }
                                List<WeaponPoolRecordDTO> characterPoolRecordDTOList = futureResult.getData().getList();
                                // 将单个任务的结果合并到总结果列表中
                                // 使用普通for循环代替流处理，减少流的开销
                                for (WeaponPoolRecordDTO dto : characterPoolRecordDTOList) {
                                    weaponPoolRecordArrayList.add(convertToEntity(dto, roleId, SERVER_ID));
                                }
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
                        return weaponPoolRecordArrayList.size();
                    })
                    // join()：阻塞等待所有结果处理完成并获取最终结果
                    // 注意：join()与get()类似，但不会抛出受检异常
                    .join();

            LogUtils.info("roleId：{} 异步请求全部完成，耗时：{} ms", roleId, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            // 处理异常情况：记录错误日志并返回错误结果
            LogUtils.error("处理任务失败: {}", logNickName, e);
            // 增加失败记录计数器
            totalFailedRecords.incrementAndGet();

        } finally {
            // 减少活跃任务计数器，确保计数器正确释放
            activeTasks.decrementAndGet();
        }

        LogUtils.info("roleId: {} 开始插入武器寻访记录数据到数据库 耗时: {} ms", roleId, System.currentTimeMillis() - startTime);
        // 批量插入数据库 - 利用数据库唯一约束：将收集到的所有记录批量插入数据库
        batchInsertWithUniqueIndex(weaponPoolRecordArrayList);
        LogUtils.info("roleId：{} 武器寻访记录数据全部插入 耗时: {} ms", roleId, System.currentTimeMillis() - startTime);

    }


    private void batchInsertWithUniqueIndex(List<WeaponPoolRecord> weaponPoolRecordList) {
        // 创建处理结果对象

        // 检查记录列表是否为空
        if (weaponPoolRecordList.isEmpty()) {
            // 设置空结果并返回
            return;
        }

        // 初始化计数器
        int successCount = 0; // 成功插入的记录数
        int duplicatedCount = 0; // 重复的记录数
        int failedCount = 0; // 失败的记录数


        // 分批插入：将大列表分割为小批次进行处理
        for (int i = 0; i < weaponPoolRecordList.size(); i += BATCH_SIZE) {
            // 计算当前批次的结束索引
            int endIndex = Math.min(i + BATCH_SIZE, weaponPoolRecordList.size());
            // 获取当前批次的数据子列表
            List<WeaponPoolRecord> batch = weaponPoolRecordList.subList(i, endIndex);

            try {
                // 生成ID：为当前批次的每条记录生成唯一主键ID
                for (WeaponPoolRecord record : batch) {
                    record.setId(idGenerator.nextId());
                }

                // 批量插入 - 利用数据库唯一约束：尝试批量插入当前批次
                weaponPoolRecordMapper.batchInsert(batch);
                // 更新成功计数器：增加当前批次的记录数
                successCount += batch.size();
                // 更新总处理记录计数器：原子操作增加当前批次的记录数
                totalProcessedRecords.addAndGet(batch.size());

                // 记录调试日志：显示批处理进度
                LogUtils.info("批处理插入完成: {}/{}", successCount, weaponPoolRecordList.size());

            } catch (Exception e) {
                // 批量插入失败：记录警告日志，包含批次范围和错误原因
//                LogUtils.error("批处理插入遇到重复数据或其他错误: {}-{}, 原因: {}", i, endIndex, e.getMessage());

                // 尝试逐条插入，捕获重复错误：当批量插入失败时，改为逐条插入
                for (WeaponPoolRecord record : batch) {
                    try {
                        // 尝试插入单条记录
                        weaponPoolRecordMapper.insert(record);
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
                            // 记录错误日志：显示失败记录和错误详情
                            LogUtils.error("单条记录插入失败: {}", record.getSeqId(), singleError);
                        }
                    }
                }
            }
        }

        LogUtils.info("插入完成 - 新增: {}条, 重复: {}条, 失败: {}条", successCount, duplicatedCount, failedCount);
    }

    public List<WeaponPoolRecord> getWeaponPoolRecordByTaskId(String roleId) {

        LambdaQueryWrapper<WeaponPoolRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WeaponPoolRecord::getRoleId, roleId);
        return weaponPoolRecordMapper.selectList(queryWrapper);
    }

    private WeaponPoolRecordResponseDTO requestWeaponPoolRecordAPI(String u8Token, String seqId) {
        String encodeToken = smartUrlEncode(u8Token);
        String CHARACTER_RECORD_API = "https://ef-webview.hypergryph.com/api/record/weapon";
        String url = CHARACTER_RECORD_API + "?lang=" + LANG + "&token=" + encodeToken + "&server_id=" + SERVER_ID;
        if (seqId != null) {
            url += "&seq_id=" + seqId;
        }

        Map<String, String> headers = PoolRecordTaskUtil.getHeader();
        try {
            String response = OkHttpUtil.getWithHeaders(url, headers);
            WeaponPoolRecordResponseDTO weaponPoolRecordResponseDTO = JsonMapper.parseObject(response, new TypeReference<>() {
            });

            if (weaponPoolRecordResponseDTO == null || weaponPoolRecordResponseDTO.getData() == null
                    || weaponPoolRecordResponseDTO.getData().getList() == null || weaponPoolRecordResponseDTO.getData().getList().isEmpty()) {
                return new WeaponPoolRecordResponseDTO(500, null, "请求终末地角色抽卡记录失败");
            }

            return weaponPoolRecordResponseDTO;
        } catch (IOException e) {
            LogUtils.error("API请求失败: {}", url, e);
            throw new RuntimeException("API请求失败", e);
        }
    }

    /**
     * 转换DTO到实体
     */
    private WeaponPoolRecord convertToEntity(WeaponPoolRecordDTO dto, String roleId, String serverId) {
        return new WeaponPoolRecord(
                idGenerator.nextId(),  // ID
                roleId,                 // roleId
                dto.getPoolId(),     // poolId
                dto.getPoolName(),   // poolName
                dto.getWeaponId(),     // weaponId
                dto.getWeaponName(),   // weaponName
                dto.getWeaponType(),   // weaponType
                dto.getRarity(),     // rarity
                dto.getIsNew(),      // isNew
                dto.getGachaTs(),    // gachaTs
                dto.getSeqId(),      // seqId
                LANG,               // lang
                serverId            // serverId
        );
    }


}
