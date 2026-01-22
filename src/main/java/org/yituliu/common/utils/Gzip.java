package org.yituliu.common.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

public class Gzip {

    /**
     * 解压 gzip 压缩的数据
     * @param compressedData 压缩的字节数组
     * @return 解压后的字符串
     * @throws IOException 解压失败时抛出
     */
    public static String decompressGzip(byte[] compressedData) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
        GZIPInputStream gis = new GZIPInputStream(bis);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] buffer = new byte[1024];
        int len;
        while ((len = gis.read(buffer)) != -1) {
            bos.write(buffer, 0, len);
        }

        bos.close();
        gis.close();
        bis.close();

        return bos.toString(); // 默认使用 UTF-8 编码
    }

    /**
     * 检测并解压可能的 gzip 数据
     * @param data 输入字节数组
     * @return 处理后的字符串
     * @throws IOException 处理失败时抛出
     */
    public static String handlePossibleGzipData(byte[] data) throws IOException {
        // 检查是否为 gzip 压缩（gzip 魔术数字：0x1F 0x8B）
        if (data.length >= 2 && (data[0] & 0xFF) == 0x1F && (data[1] & 0xFF) == 0x8B) {
            // 是 gzip 压缩数据，解压
            return decompressGzip(data);
        } else {
            // 不是压缩数据，直接转换为字符串
            return new String(data, "UTF-8");
        }
    }
}
