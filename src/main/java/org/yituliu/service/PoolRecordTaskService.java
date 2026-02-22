package org.yituliu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.yituliu.common.annotation.RedisCacheable;
import org.yituliu.common.enums.ResultCode;
import org.yituliu.common.exception.ServiceException;
import org.yituliu.common.utils.IdGenerator;
import org.yituliu.common.utils.JsonMapper;
import org.yituliu.common.utils.LogUtils;
import org.yituliu.common.utils.OkHttpUtil;
import org.yituliu.entity.dto.pool.record.CharacterPoolRecordResponseDTO;
import org.yituliu.entity.dto.pool.record.EndfieldUserInfoDTO;
import org.yituliu.entity.po.CharacterPoolRecord;
import org.yituliu.entity.po.EndministratorInfo;
import org.yituliu.entity.po.PlayerPoolRecordTask;
import org.yituliu.entity.po.WeaponPoolRecord;
import org.yituliu.entity.vo.PoolRecordVO;
import org.yituliu.mapper.CharacterPoolRecordMapper;
import org.yituliu.mapper.EndministratorInfoMapper;
import org.yituliu.mapper.PlayerPoolRecordTaskMapper;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PoolRecordTaskService {

    private static String SPECIAL_POOL_TYPE = "E_CharacterGachaPoolType_Special";
    private static String STANDARD_POOL_TYPE = "E_CharacterGachaPoolType_Standard";
    private static String BEGINNER_POOL_TYPE = "E_CharacterGachaPoolType_Beginner";

    // 性能监控相关计数器
    // 总处理记录数：原子长整型，线程安全地记录已处理的记录总数
    private final AtomicLong totalProcessedRecords = new AtomicLong(0);
    // 总重复记录数：原子长整型，线程安全地记录检测到的重复记录总数
    private final AtomicLong totalDuplicatedRecords = new AtomicLong(0);
    // 总失败记录数：原子长整型，线程安全地记录处理失败的记录总数
    private final AtomicLong totalFailedRecords = new AtomicLong(0);
    // 活跃任务数：原子整型，线程安全地记录当前正在执行的任务数量
    private final AtomicInteger activeTasks = new AtomicInteger(0);

    private final Long TEN_MINUTE = 60 * 10 * 1000L;

    private final IdGenerator idGenerator;

    private final PlayerPoolRecordTaskMapper playerPoolRecordTaskMapper;
    private final EndministratorInfoMapper endministratorInfoMapper;
    private final CharacterPoolRecordService characterPoolRecordService;
    private final WeaponPoolRecordService weaponPoolRecordService;
    private final RedisTemplate<String, Object> redisTemplate;

    public PoolRecordTaskService(PlayerPoolRecordTaskMapper playerPoolRecordTaskMapper,
                                 EndministratorInfoMapper endministratorInfoMapper,
                                 CharacterPoolRecordService characterPoolRecordService,
                                 WeaponPoolRecordService weaponPoolRecordService,
                                 RedisTemplate<String, Object> redisTemplate) {

        this.playerPoolRecordTaskMapper = playerPoolRecordTaskMapper;
        this.endministratorInfoMapper = endministratorInfoMapper;
        this.characterPoolRecordService = characterPoolRecordService;
        this.weaponPoolRecordService = weaponPoolRecordService;
        this.redisTemplate = redisTemplate;
        this.idGenerator = new IdGenerator(1L);


    }

    /**
     * 创建任务，以hgToken为key进行限流
     * 限制：每个hgToken每分钟最多创建10个任务
     */
    public String createTask(HttpServletRequest httpServletRequest, String hgToken) {
        // 限流逻辑：每个hgToken每分钟最多创建10个任务
        String limitKey = "rate_limit:create_task." + hgToken;
        long currentCount = redisTemplate.opsForValue().increment(limitKey);

        String taskId = "task" + idGenerator.nextId();

        // 如果是第一次请求，设置过期时间为1分钟
        if (currentCount == 1) {
            redisTemplate.expire(limitKey, 60, TimeUnit.SECONDS);
        }
        
        if (currentCount > 1) {
            throw new ServiceException(ResultCode.TOO_MANY_REQUESTS);
        }

        PlayerPoolRecordTask playerPoolRecordTask = new PlayerPoolRecordTask();
        playerPoolRecordTask.setToken(hgToken);
        playerPoolRecordTask.setStartFlag(false);
        playerPoolRecordTask.setCompleteFlag(false);
        playerPoolRecordTask.setCreateTime(new Date());

        playerPoolRecordTask.setTaskId(taskId);
        playerPoolRecordTaskMapper.insert(playerPoolRecordTask);
        return taskId;
    }

    public EndministratorInfo checkTask(String taskId) {
        PlayerPoolRecordTask playerPoolRecordTask = playerPoolRecordTaskMapper.selectById(taskId);
        if (playerPoolRecordTask.getCompleteFlag()) {
            String roleId = playerPoolRecordTask.getRoleId();
            LambdaQueryWrapper<EndministratorInfo> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(EndministratorInfo::getRoleId, roleId);
            return endministratorInfoMapper.selectOne(queryWrapper);
        }

        throw new ServiceException(ResultCode.TASK_NOT_COMPLETED);
    }


    @Async("asyncExecutor")
    public void savePoolRecordAsync(PlayerPoolRecordTask playerPoolRecordTask) {
        // 记录任务开始时间，用于计算处理耗时
        long startTime = System.currentTimeMillis();

        EndfieldUserInfoDTO endfieldUserInfoDTO = getPlayerInfo(playerPoolRecordTask);

        String nickName = endfieldUserInfoDTO.getNickName();
        String roleId = endfieldUserInfoDTO.getRoleId();
        String uid = endfieldUserInfoDTO.getUid();
        LogUtils.info("roleId: {} 开始干员寻访记录导入任务 耗时: {} ms", roleId, System.currentTimeMillis() - startTime);
        characterPoolRecordService.saveCharacterPoolRecordAsync(endfieldUserInfoDTO);
        LogUtils.info("roleId: {} 开始武器寻访记录导入任务 耗时: {} ms", roleId, System.currentTimeMillis() - startTime);
        weaponPoolRecordService.saveWeaponPoolRecordAsync(endfieldUserInfoDTO);

        playerPoolRecordTask.setCompleteFlag(true);
        playerPoolRecordTask.setRoleId(roleId);

        LambdaQueryWrapper<EndministratorInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EndministratorInfo::getRoleId, roleId);
        if (!endministratorInfoMapper.exists(queryWrapper)) {
            EndministratorInfo endministratorInfo = new EndministratorInfo();
            endministratorInfo.setUid(uid);
            endministratorInfo.setRoleId(roleId);
            endministratorInfo.setNickName(nickName);
            endministratorInfoMapper.insert(endministratorInfo);
        }

        playerPoolRecordTaskMapper.updateById(playerPoolRecordTask);
        LogUtils.info("roleId: {} 全部寻访记录导入任务完成 耗时: {} ms", roleId, System.currentTimeMillis() - startTime);

    }


    /**
     * 获取用户角色卡池记录（同步版本）
     */

    public PoolRecordVO getCharacterPoolRecordData(HttpServletRequest httpServletRequest, String taskId) {
        PlayerPoolRecordTask playerPoolRecordTask = playerPoolRecordTaskMapper.selectById(taskId);
        if (playerPoolRecordTask == null) {
            throw new ServiceException(ResultCode.TOKEN_EXPIRATION);
        }

        List<CharacterPoolRecord> characterPoolRecordByTaskId = characterPoolRecordService.getCharacterPoolRecordByTaskId(playerPoolRecordTask.getRoleId());
        List<WeaponPoolRecord> weaponPoolRecordByTaskId = weaponPoolRecordService.getWeaponPoolRecordByTaskId(playerPoolRecordTask.getRoleId());

        return new PoolRecordVO(characterPoolRecordByTaskId, weaponPoolRecordByTaskId);

    }


    /**
     * 定时处理待执行的角色池记录任务
     * 每秒钟执行一次，优先处理最早创建的未开始任务
     */
    @Scheduled(cron = "* * * * * ?")
    protected void loadingPoolRecord() {
        PlayerPoolRecordTask playerPoolRecordTask = null;
        try {
            playerPoolRecordTask = playerPoolRecordTaskMapper.selectFirstByStartFlagFalseOrderByCreateTimeAsc();
            if (playerPoolRecordTask == null) {
                //            LogUtils.error("当前无任务");
                return;
            }
            playerPoolRecordTask.setStartFlag(true);
            playerPoolRecordTask.setUpdateTime(new Date());
            playerPoolRecordTaskMapper.updateById(playerPoolRecordTask);
            if (new Date().getTime() - playerPoolRecordTask.getCreateTime().getTime() < TEN_MINUTE) {
                savePoolRecordAsync(playerPoolRecordTask);
            }
            LogUtils.info("当前执行：{}", playerPoolRecordTask.getTaskId());
        } catch (Exception e) {
            String taskId = "unknown";
            if (playerPoolRecordTask != null) {
                taskId = playerPoolRecordTask.getTaskId();
                playerPoolRecordTask.setRoleId("ERROR_TASK");
                playerPoolRecordTaskMapper.updateById(playerPoolRecordTask);
            }
            LogUtils.error("处理任务失败，任务ID: {}", taskId, e);
        }
    }


    @RedisCacheable(key = "TASK:ID", paramOrMethod = "getTaskId")
    public EndfieldUserInfoDTO getPlayerInfo(PlayerPoolRecordTask playerPoolRecordTask) {
        //本地创建token的服务请求参数
        HashMap<String, String> tokenServiceParams = new HashMap<>();
        tokenServiceParams.put("hgToken", playerPoolRecordTask.getToken());
        // 使用表单格式向本地创建token的服务发送请求
        String localServiceResponseText = OkHttpUtil.postForm("http://127.0.0.1:10086/token", tokenServiceParams);
        LogUtils.info("本地服务响应：{}", localServiceResponseText);
        JsonNode jsonNode = JsonMapper.parseJSONObject(localServiceResponseText);
        //服务返回的信息，包括roleName，roleId，uid，u8token
        return JsonMapper.parseObject(jsonNode.get("data").toString(), EndfieldUserInfoDTO.class);
    }


    @RedisCacheable(key = "TASK:ID", paramOrMethod = "getTaskId")
    public String test() {
        return "taskId:14141";
    }

}
