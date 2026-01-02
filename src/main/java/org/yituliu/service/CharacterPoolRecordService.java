package org.yituliu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.yituliu.common.enums.ResultCode;
import org.yituliu.common.exception.ServiceException;
import org.yituliu.common.utils.IdGenerator;
import org.yituliu.common.utils.JsonMapper;
import org.yituliu.common.utils.LogUtils;
import org.yituliu.entity.dto.pool.record.response.CharacterPoolRecordDTO;
import org.yituliu.entity.dto.pool.record.response.CharacterPoolRecordResponseDTO;
import org.yituliu.entity.po.CharacterPoolRecord;
import org.yituliu.mapper.CharacterPoolRecordMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.yituliu.util.OkHttpUtil;
import org.yituliu.util.UrlParser;

import java.io.IOException;
import java.util.*;

import static org.yituliu.util.UrlEncodeUtil.smartUrlEncode;

@Service
public class CharacterPoolRecordService {

    /**
     * 卡池类型常量集合
     * 包含三种卡池类型，静态初始化确保只存在一个实例
     */
    private static final Set<String> POOL_TYPES = Set.of(
            "E_CharacterGachaPoolType_Special",
            "E_CharacterGachaPoolType_Standard",
            "E_CharacterGachaPoolType_Beginner"
    );

    private final String CHARACTER_RECORD_API = "https://endfield.hypergryph.com/webview/api/record/char";
//    private final String CHARACTER_RECORD_API = "http://127.0.0.1:10010/character_pool_record";

    private final String Lang = "zh-cn";

    private final CharacterPoolRecordMapper characterPoolRecordMapper;

    private final IdGenerator idGenerator;

    /**
     * 构造函数
     *
     * @param characterPoolRecordMapper 角色卡池记录Mapper
     */
    public CharacterPoolRecordService(CharacterPoolRecordMapper characterPoolRecordMapper) {
        this.characterPoolRecordMapper = characterPoolRecordMapper;

        this.idGenerator = new IdGenerator(1L);
    }

    public String saveCharacterPoolRecord(HttpServletRequest httpServletRequest, String uid, String url) {
        // 记录任务开始时间，用于计算处理耗时
        long startTime = System.currentTimeMillis();
//        String uid = httpServletRequest.getHeader("uid");
        if (uid == null) {
            throw new ServiceException(ResultCode.USER_NOT_EXIST);
        }


        Map<String, String> urlParams = UrlParser.parseEndfieldGachaUrl(url);
        String serverId = urlParams.get("server_id");
        //要准备存入数据库的新角色卡池记录
        List<CharacterPoolRecord> newCharacterPoolRecordList = new ArrayList<>();

        //根据卡池名称进行分组，他的value依旧是一个map，map的key是卡池名称和seqId组合成的一个临时联合索引，用于判断这条角色抽卡记录是否已经存在
        Map<String, Map<String, Integer>> existingCharacterPoolRecordGroupByPoolName = new HashMap<>();

        getDuration(startTime,"开始从终末地API数据获取",uid);
        for (String poolType : POOL_TYPES) {

            Boolean hasMore = true;
            String poolSeqId = null;
            int maxExisting = 0;

            while (hasMore && maxExisting < 10) {
                //从终末地API返回的角色抽卡记录
                CharacterPoolRecordResponseDTO characterPoolRecordResponseDTO = requestCharacterPoolRecordAPI(urlParams, poolType, poolSeqId);

                hasMore = characterPoolRecordResponseDTO.getData().getHasMore();

                List<CharacterPoolRecordDTO> characterPoolRecordListDTO = characterPoolRecordResponseDTO.getData().getList();

                for (CharacterPoolRecordDTO characterPoolRecordDTO : characterPoolRecordListDTO) {
                    String poolName = characterPoolRecordDTO.getPoolName();
                    String seqId = characterPoolRecordDTO.getSeqId();
                    String tmpCharacterPoolRecordIndex = poolName + seqId;
                    poolSeqId = seqId;


                    if (!existingCharacterPoolRecordGroupByPoolName.containsKey(poolName)) {
                        //角色抽卡记录表查询构造器

                        LambdaQueryWrapper<CharacterPoolRecord> queryWrapper = new LambdaQueryWrapper<>();
                        queryWrapper.eq(CharacterPoolRecord::getUid, uid);
                        queryWrapper.eq(CharacterPoolRecord::getPoolName, poolName);
                        List<CharacterPoolRecord> characterPoolRecordList = characterPoolRecordMapper.selectList(queryWrapper);
                        HashMap<String, Integer> poolNameAndSeqIdMap = new HashMap<>();
                        for (CharacterPoolRecord characterPoolRecord : characterPoolRecordList) {
                            poolNameAndSeqIdMap.put(characterPoolRecord.getPoolName() + characterPoolRecord.getSeqId(), 1);
                        }
                        existingCharacterPoolRecordGroupByPoolName.put(poolName, poolNameAndSeqIdMap);
                    }

                    Map<String, Integer> existingPoolNameAndSeqIdMap = existingCharacterPoolRecordGroupByPoolName.get(poolName);


                    if (existingPoolNameAndSeqIdMap.containsKey(tmpCharacterPoolRecordIndex)) {

                        maxExisting++;
                        continue;
                    }

                    CharacterPoolRecord characterPoolRecord = new CharacterPoolRecord();
                    characterPoolRecord.setId(idGenerator.nextId());
                    characterPoolRecord.setEndfieldUid("");
                    characterPoolRecord.setUid(uid);
                    // 将DTO属性赋值到PO对象
                    characterPoolRecord.setPoolId(characterPoolRecordDTO.getPoolId());
                    characterPoolRecord.setPoolName(characterPoolRecordDTO.getPoolName());
                    characterPoolRecord.setCharId(characterPoolRecordDTO.getCharId());
                    characterPoolRecord.setCharName(characterPoolRecordDTO.getCharName());
                    characterPoolRecord.setRarity(characterPoolRecordDTO.getRarity());
                    characterPoolRecord.setIsFree(characterPoolRecordDTO.getIsFree());
                    characterPoolRecord.setIsNew(characterPoolRecordDTO.getIsNew());
                    characterPoolRecord.setGachaTs(characterPoolRecordDTO.getGachaTs());
                    characterPoolRecord.setSeqId(characterPoolRecordDTO.getSeqId());
                    characterPoolRecord.setLang(Lang);
                    characterPoolRecord.setPoolType(poolType);
                    characterPoolRecord.setServerId(serverId);


                    // 设置额外字段（需要从其他地方获取）
                    characterPoolRecord.setUid(uid); // 使用传入的uid参数
                    // endfieldUid、language、poolType、serverId等字段需要根据业务逻辑设置

                    newCharacterPoolRecordList.add(characterPoolRecord);
                }
            }

            getDuration(startTime,poolType+"数据获取",uid);
        }

        getDuration(startTime,"全部数据获取",uid);

        if(!newCharacterPoolRecordList.isEmpty()){
            characterPoolRecordMapper.batchInsert(newCharacterPoolRecordList);
        }

        getDuration(startTime,"全部数据插入",uid);

        return "本次导入"+newCharacterPoolRecordList.size()+"条角色卡池记录";
    }

    private void getDuration(Long startTime,String taskName,String uid){
        // 计算处理耗时
        long duration = System.currentTimeMillis() - startTime;
        // 记录任务完成日志，包含处理统计信息
        LogUtils.info("当前进度{} :uid {}, 耗时: {}ms",
                taskName,uid, duration);
    }


    private CharacterPoolRecordResponseDTO requestCharacterPoolRecordAPI(Map<String, String> urlParams, String poolType, String seqId) {
        // 记录任务开始时间，用于计算处理耗时
        long startTime = System.currentTimeMillis();
        String token = urlParams.get("token");
        String server_id = urlParams.get("server_id");

        // 判断token是否已经是URL地址栏格式，如果不是则转换为地址栏格式
        String encodeToken = smartUrlEncode(token);

        String url = CHARACTER_RECORD_API + "?lang=" + Lang + "&pool_type=" + poolType + "&token=" + encodeToken + "&server_id=" + server_id;
        if (seqId != null) {
            url += "&seq_id=" + seqId;
        }

        try {
            // 请求URL（示例使用JSONPlaceholder的测试API）
            // 创建请求头
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


            // 使用Jackson将JSON字符串映射到GachaResponseDTO实体类


            CharacterPoolRecordResponseDTO characterPoolRecordResponseDTO = JsonMapper.parseObject(response, CharacterPoolRecordResponseDTO.class);

            getDuration(startTime,"请求结束",url);
            return characterPoolRecordResponseDTO;

        } catch (IOException e) {
            e.printStackTrace();

        }

        return null;
    }


    public List<CharacterPoolRecord> getCharacterPoolRecordData(HttpServletRequest httpServletRequest, String uid) {
        LambdaQueryWrapper<CharacterPoolRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CharacterPoolRecord::getUid, uid);

        return characterPoolRecordMapper.selectList(queryWrapper);
    }
}
