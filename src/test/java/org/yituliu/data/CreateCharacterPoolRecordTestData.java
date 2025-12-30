package org.yituliu.data;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.yituliu.common.utils.JsonMapper;
import org.yituliu.entity.dto.CharacterPoolRecordDTO;
import org.yituliu.util.FileUtil;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class CreateCharacterPoolRecordTestData {

    @Test
    void createCharacterPoolRecordTestData() {

        List<CharacterPoolRecordDTO> testDataList = new ArrayList<>();
        long seqId = 1;
        for(int type = 0; type < 5; type++) {
            for (int num = 0; num < 880; num++) {
                CharacterPoolRecordDTO characterPoolRecordDTO = new CharacterPoolRecordDTO();
                characterPoolRecordDTO.setPoolId("special_1_0_"+type);
                characterPoolRecordDTO.setPoolName("测试卡池"+type);
                characterPoolRecordDTO.setCharId("chr_0"+num);
                characterPoolRecordDTO.setCharName("测试角色"+num);
                characterPoolRecordDTO.setRarity(5);
                characterPoolRecordDTO.setIsFree(true);
                characterPoolRecordDTO.setIsNew(true);
                long l = System.currentTimeMillis();
                characterPoolRecordDTO.setGachaTs(""+l);
                characterPoolRecordDTO.setSeqId(seqId+ "");
                seqId++;
                testDataList.add(characterPoolRecordDTO);
            }
        }
        
        // 帮我把这个testDataList存为json到src/test/resources/character_pool_record.json
        FileUtil.saveStringToFile(JsonMapper.toJSONString(testDataList), "src/test/resources/character_pool_record.json");
    
    }
}
