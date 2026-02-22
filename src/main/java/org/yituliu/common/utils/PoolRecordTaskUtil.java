package org.yituliu.common.utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yituliu.common.annotation.RedisCacheable;
import org.yituliu.entity.dto.pool.record.EndfieldUserInfoDTO;
import org.yituliu.entity.po.PlayerPoolRecordTask;

import com.fasterxml.jackson.databind.JsonNode;

public class PoolRecordTaskUtil {

    public static Map<String, String> getHeader(){
        Map<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Accept-Encoding", "gzip, deflate, br, zstd");
        headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
        headers.put("Host", "ef-webview.hypergryph.com");
        headers.put("Sec-Fetch-Dest", "empty");
        headers.put("Sec-Fetch-Mode", "cors");
        headers.put("Sec-Fetch-Site", "same-origin");
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:146.0) Gecko/20100101 Firefox/146.0");

        return headers;
    }

    /**
     * 初始化序列ID列表，每页为5个记录，从最新序列id开始递减5，生成一个序列id表
     * 适用于已知lastSeqId为正数且大于minSeqId
     *
     * @param lastSeqIdStr  从终末地API返回的最新序列id（seqId)
     * @param existingSeqId 该用户保存在数据库中的记录的最后一个序列id 再-10，多查两页，以防出现问题
     * @return 序列ID字符串列表
     */
    public static List<String> initSeqIdList(String lastSeqIdStr, Integer existingSeqId) {
        // 参数校验
        if (lastSeqIdStr == null || lastSeqIdStr.trim().isEmpty()) {
            throw new IllegalArgumentException("lastSeqIdStr不能为空");
        }

        if (existingSeqId == null) {
            existingSeqId = 0;
        }

        if(existingSeqId>10){
            existingSeqId-=5;
        }

        int lastSeqId = Integer.parseInt(lastSeqIdStr.trim());

//        if (lastSeqId < existingSeqId) {
//            return Collections.emptyList();
//        }

        List<String> seqIdList = new ArrayList<>();
        for (int i = lastSeqId; i >= existingSeqId; i -= 5) {
            seqIdList.add(String.valueOf(i));
        }
        return seqIdList;
    }

    /**
     * 判断是否为重复键错误
     * 该方法检查异常是否为数据库唯一约束违反错误
     * 支持MySQL和SQLite等常见数据库的重复键错误判断
     *
     * @param error 需要检查的异常对象
     * @return boolean 如果为重复键错误返回true，否则返回false
     */
    public static boolean isDuplicateKeyError(Exception error) {
        // 检查是否为MyBatis系统异常
        if (error instanceof org.mybatis.spring.MyBatisSystemException) {
            // 获取异常的根本原因
            Throwable cause = error.getCause();
            // 检查根本原因是否为SQL异常
            if (cause instanceof java.sql.SQLException) {
                // 获取SQL状态码
                String sqlState = ((SQLException) cause).getSQLState();
                // MySQL重复键错误代码：23000
                return "23000".equals(sqlState);
            }
        }
        // 通过错误消息内容判断：检查错误消息中是否包含重复键相关的关键词
        return error.getMessage() != null &&
                (error.getMessage().contains("Duplicate entry") || // MySQL重复键错误消息
                        error.getMessage().contains("UNIQUE constraint failed")); // SQLite唯一约束失败错误消息
    }


   
}
