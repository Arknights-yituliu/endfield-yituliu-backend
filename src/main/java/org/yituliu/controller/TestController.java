package org.yituliu.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yituliu.common.annotation.RedisCacheable;
import org.yituliu.common.utils.JsonMapper;
import org.yituliu.common.utils.Result;
import org.yituliu.entity.dto.CharacterPoolRecordDTO;
import org.yituliu.entity.dto.CharacterPoolRecordResponseDTO;
import org.yituliu.util.FileUtil;

import java.util.List;
import java.util.stream.Collectors;


@RestController
public class TestController {
    @GetMapping("/")
    public Result<String> startStatus() {
        return Result.success("后端启动成功");
    }


    public Result<CharacterPoolRecordResponseDTO> getCharacterPoolRecord()  {

        List<CharacterPoolRecordDTO> collect = getTestCharacterPoolRecord().stream().filter(e -> "限定池".equals(e.getPoolId())).toList();

        return null;
    }

    @RedisCacheable(key = "character_pool_record")
    private List<CharacterPoolRecordDTO> getTestCharacterPoolRecord(){
        String s = FileUtil.readFileToString("src/test/resources/local/character_pool_record.json");

        return JsonMapper.parseJSONArray(s, new TypeReference<>() {
        });
    }

}
