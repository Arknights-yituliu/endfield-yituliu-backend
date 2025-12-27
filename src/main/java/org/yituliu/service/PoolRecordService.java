package org.yituliu.service;

import org.springframework.stereotype.Service;
import org.yituliu.entity.dto.GachaResponseDTO;
import org.yituliu.entity.po.CharacterPoolRecord;
import org.yituliu.mapper.CharacterPoolRecordMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PoolRecordService {

    private final CharacterPoolRecordMapper characterPoolRecordMapper;


    /**
     * 构造函数
     *
     * @param characterPoolRecordMapper 角色卡池记录Mapper
     * @param httpClientUtil            HTTP客户端工具类
     */
    public PoolRecordService(CharacterPoolRecordMapper characterPoolRecordMapper) {
        this.characterPoolRecordMapper = characterPoolRecordMapper;

    }

    public String savePoolRecord(String url, String uid) {
        LambdaQueryWrapper<CharacterPoolRecord> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(CharacterPoolRecord::getUid, uid);
        queryWrapper.eq(CharacterPoolRecord::getPoolId, url);
        List<CharacterPoolRecord> characterPoolRecordList = characterPoolRecordMapper.selectList(queryWrapper);

        GachaResponseDTO characterPoolRecordRequest = createCharacterPoolRecordRequest(url);
        System.out.println(characterPoolRecordRequest);
        return null;
    }

    private GachaResponseDTO createCharacterPoolRecordRequest(String url) {


        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json, text/plain, */*");
        headers.put("accept-encoding", "gzip, deflate, br, zstd");
        headers.put("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
        headers.put("if-none-match", "W/\"3db-dAnLSmaymYp4yw2FTM2q/Oca/kc\"");
        headers.put("priority", "u=1, i");
        headers.put("referer", url);
        headers.put("sec-ch-ua", "\"Microsoft Edge\";v=\"143\", \"Chromium\";v=\"143\", \"Not A(Brand\";v=\"24\"");
        headers.put("sec-ch-ua-mobile", "?0");
        headers.put("sec-ch-ua-platform", "\"Windows\"");
        headers.put("sec-fetch-dest", "empty");
        headers.put("sec-fetch-mode", "cors");
        headers.put("sec-fetch-site", "same-origin");

        // 使用注入的HttpClientUtil实例发送请求


        return null;
    }


}
