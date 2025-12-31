package org.yituliu.data;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.yituliu.common.utils.JsonMapper;
import org.yituliu.entity.dto.CharacterPoolRecordDTO;
import org.yituliu.util.FileUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SpringBootTest
public class CharacterPoolRecordServiceTest {

    @Test
    void createCharacterPoolRecordTestData() {
        // 生成 [0, 99] 之间的随机整数（包含0和99）
        int randomNum = ThreadLocalRandom.current().nextInt(100);

        List<CharacterPoolRecordDTO> testDataList = new ArrayList<>();
        long seqId = 1;
        for(int type = 0; type < 5000; type++) {
//            int num = ThreadLocalRandom.current().nextInt(100);
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


    @Test
    void seqIdListTest(){
        System.out.println(initSeqIdList("692",540));
    }

    /**
     * 初始化序列ID列表
     * 从lastSeqId开始，每次递减5，生成序列ID集合
     *
     * @param lastSeqIdStr 最后一个序列ID的字符串形式
     * @param minSeqId 最小序列ID（包含）
     * @return 序列ID字符串列表，按降序排列
     * @throws IllegalArgumentException 当输入参数无效时抛出
     */
    private List<String> initSeqIdList(String lastSeqIdStr, Integer minSeqId) {
        // 参数校验
        if (lastSeqIdStr == null || lastSeqIdStr.trim().isEmpty()) {
            throw new IllegalArgumentException("lastSeqIdStr不能为空");
        }
        if (minSeqId == null) {
            throw new IllegalArgumentException("minSeqId不能为空");
        }

        List<String> seqIdList = new ArrayList<>();

        try {
            // 解析并校验lastSeqId
            int lastSeqId = Integer.parseInt(lastSeqIdStr.trim());
            if (lastSeqId < minSeqId) {
                // 如果起始值小于最小值，直接返回空列表
                return seqIdList;
            }

            // 优化：预估集合大小，减少扩容开销
            int estimatedSize = ((lastSeqId - minSeqId) / 5) + 1;
            seqIdList = new ArrayList<>(estimatedSize);

            // 生成序列ID列表
            for (int i = lastSeqId; i >= minSeqId; i -= 5) {
                seqIdList.add(String.valueOf(i));
            }

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                String.format("无效的序列ID格式: lastSeqIdStr='%s'", lastSeqIdStr), e);
        }

        return seqIdList;
    }

    /**
     * 初始化序列ID列表（简化版本）
     * 适用于已知lastSeqId为正数且大于minSeqId的场景
     *
     * @param lastSeqId 最后一个序列ID
     * @param minSeqId 最小序列ID
     * @return 序列ID字符串列表
     */
    private List<String> initSeqIdListSimple(int lastSeqId, int minSeqId) {
        if (lastSeqId < minSeqId) {
            return Collections.emptyList();
        }

        List<String> seqIdList = new ArrayList<>((lastSeqId - minSeqId) / 5 + 3);
        for (int i = lastSeqId; i >= minSeqId; i -= 5) {
            seqIdList.add(String.valueOf(i));
        }
        return seqIdList;
    }
}
