package org.yituliu.service;

import org.springframework.stereotype.Service;
import org.yituliu.entity.dto.GachaResponseDTO;
import org.yituliu.entity.po.CharacterPoolRecord;
import org.yituliu.mapper.CharacterPoolRecordMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.yituliu.util.UrlParser;

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


        Map<String, String> map = UrlParser.parseEndfieldGachaUrl(url);



        return null;
    }





}
