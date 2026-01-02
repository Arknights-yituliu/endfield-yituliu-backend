package org.yituliu.data;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.yituliu.common.utils.JsonMapper;
import org.yituliu.entity.dto.pool.record.response.CharacterPoolRecordDTO;
import org.yituliu.service.CharacterPoolRecordService;
import org.yituliu.service.CharacterPoolRecordServiceV2;
import org.yituliu.util.FileUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootTest
public class CharacterPoolRecordServiceTest {


    @Test
    void createCharacterPoolRecordTestData() {
        // 生成 [0, 99] 之间的随机整数（包含0和99）
        int randomNum = ThreadLocalRandom.current().nextInt(100);

        List<CharacterPoolRecordDTO> testDataList = new ArrayList<>();
        long seqId = 1200;
        for (int i = 0; i < 300; i++) {
            int num = ThreadLocalRandom.current().nextInt(100);
            CharacterPoolRecordDTO characterPoolRecordDTO = new CharacterPoolRecordDTO();

            int poolIdNum = ThreadLocalRandom.current().nextInt(4);
            if (num < 5) {
                characterPoolRecordDTO.setPoolId("启程池");
                characterPoolRecordDTO.setPoolName("启程卡池");
            } else if(num < 10){
                characterPoolRecordDTO.setPoolId("普通池");
                characterPoolRecordDTO.setPoolName("普池卡池"+poolIdNum);
            }else {
                characterPoolRecordDTO.setPoolId("限定池");
                characterPoolRecordDTO.setPoolName("限定卡池"+poolIdNum);
            }

            characterPoolRecordDTO.setCharId("chr_0" + num);
            characterPoolRecordDTO.setCharName("测试角色" + num);
            characterPoolRecordDTO.setRarity(5);
            characterPoolRecordDTO.setIsFree(true);
            characterPoolRecordDTO.setIsNew(true);
            long l = System.currentTimeMillis();
            characterPoolRecordDTO.setGachaTs("" + l);
            characterPoolRecordDTO.setSeqId(seqId-i + "");

            testDataList.add(characterPoolRecordDTO);
        }

        // 帮我把这个testDataList存为json到src/test/resources/character_pool_record.json
        FileUtil.saveStringToFile(JsonMapper.toJSONString(testDataList), "src/test/resources/local/character_pool_record.json");

    }





}
