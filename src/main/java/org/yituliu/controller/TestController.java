package org.yituliu.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yituliu.common.utils.JsonMapper;
import org.yituliu.common.utils.Result;
import org.yituliu.entity.dto.pool.record.CharacterPoolRecordDTO;
import org.yituliu.entity.dto.pool.record.CharacterPoolRecordDataDTO;
import org.yituliu.entity.dto.pool.record.CharacterPoolRecordResponseDTO;
import org.yituliu.common.utils.FileUtil;

import java.util.List;


@RestController
public class TestController {
    @GetMapping("/")
    public Result<String> startStatus() {
        return Result.success("后端启动成功");
    }


    @GetMapping("/character_pool_record")
    public CharacterPoolRecordResponseDTO getCharacterPoolRecordTest(@RequestParam String pool_type, @RequestParam(required = false) String seq_id) {

        String poolType = "限定池";
        if ("E_CharacterGachaPoolType_Special".equals(pool_type)) {
            poolType = "限定池";
        } else if ("E_CharacterGachaPoolType_Standard".equals(pool_type)) {
            poolType = "普通池";
        }else  if ("E_CharacterGachaPoolType_Beginner".equals(pool_type)){
            poolType = "启程池";
        }

        String poolType1 = poolType;
        List<CharacterPoolRecordDTO> collect = getTestCharacterPoolRecord().stream()
                .filter(e -> poolType1.equals(e.getPoolId()))
                .toList();
 
        List<CharacterPoolRecordDTO> resultList;
        
        if (seq_id == null) {
            // 如果没有传入seq_id，返回前5个元素
            resultList = collect.stream().limit(5).toList();
        } else {
            // 将传入的seq_id转换为数字
            int targetSeqId = Integer.parseInt(seq_id);
            
            // 查找比传入seq_id小的元素（数值更小的seqId）
            resultList = collect.stream()
                    .filter(e -> {
                        int currentSeqId = Integer.parseInt(e.getSeqId());
                        return currentSeqId < targetSeqId; // 只保留比目标seq_id小的元素
                    })
                    .limit(5) // 截取前5个元素
                    .toList();
        }
        
        // 检查是否还有更多数据
        boolean hasMore = true;
//        if (seq_id != null) {
//            int targetSeqId = Integer.parseInt(seq_id);
//            long smallerCount = collect.stream()
//                    .filter(e -> Integer.parseInt(e.getSeqId()) < targetSeqId)
//                    .count();
//            hasMore = smallerCount > 5;
//        } else {
//            hasMore = collect.size() > 5;
//        }
        System.out.println("seq_id:"+seq_id+" list:"+resultList);
        // 构建响应对象
        CharacterPoolRecordResponseDTO response = new CharacterPoolRecordResponseDTO();
        CharacterPoolRecordDataDTO data = new CharacterPoolRecordDataDTO();
        data.setList(resultList);
        data.setHasMore(!resultList.isEmpty()); // 检查是否还有更多数据
        response.setData(data);
        response.setCode(0);
        response.setMsg("成功");
        
        return response;
    }

//    @RedisCacheable(key = "character_pool_record")
    private List<CharacterPoolRecordDTO> getTestCharacterPoolRecord() {
        String s = FileUtil.readFileToString("src/test/resources/local/character_pool_record.json");

        return JsonMapper.parseJSONArray(s, new TypeReference<>() {
        });
    }


}
