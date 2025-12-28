package org.yituliu.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

public class JsonMapper {

    /**
     * 线程安全的ObjectMapper实例
     * 注意：ObjectMapper本身是线程安全的，静态单例模式可以提高性能
     */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 时间日期格式
    private static final String STANDARD_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 静态初始化块，配置ObjectMapper的默认行为
     */
    static {
        // 对象的所有字段全部列入序列化
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 取消默认转换timestamps形式
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // 忽略空Bean转json的错误
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 所有的日期格式都统一为以下的格式，即yyyy-MM-dd HH:mm:ss
        objectMapper.setDateFormat(new SimpleDateFormat(STANDARD_FORMAT));
        // 忽略在json字符串中存在，但在java对象中不存在对应属性的情况
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 允许未引用的字段通过
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 允许处理数值的前导零
//        objectMapper.configure(DeserializationFeature.ACCEPT_LEADING_ZEROS_FOR_NUMBERS, true);
    }

    /**
     * 私有构造函数，防止实例化
     */
    private JsonMapper() {
    }

    /**===========================以下是从JSON中获取对象====================================*/
    /**
     * 将JSON字符串转换为指定类型的对象
     * @param jsonString JSON字符串
     * @param type 目标类型引用
     * @param <T> 返回类型
     * @return 转换后的对象
     */
    public static <T> T parseObject(String jsonString, TypeReference<T> type) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            LogUtils.error("JSON字符串为空，无法转换为对象");
            return null;
        }
        if (type == null) {
            LogUtils.error("类型引用为空，无法转换");
            return null;
        }
        try {
            return objectMapper.readValue(jsonString, type);
        } catch (JsonProcessingException e) {
            LogUtils.error("JsonString转为自定义对象失败，JSON内容：{}, 错误原因：{}", jsonString, e.getMessage());
            return null;
        }
    }

    /**
     * 将JSON字符串转换为指定类的对象
     * @param jsonString JSON字符串
     * @param clazz 目标类
     * @param <T> 返回类型
     * @return 转换后的对象
     */
    public static <T> T parseObject(String jsonString, Class<T> clazz) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            LogUtils.error("JSON字符串为空，无法转换为对象");
            return null;
        }
        if (clazz == null) {
            LogUtils.error("目标类为空，无法转换");
            return null;
        }
        try {
            return objectMapper.readValue(jsonString, clazz);
        } catch (JsonProcessingException e) {
            LogUtils.error("JsonString转为对象[{}]失败：{}", clazz.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 从文件中读取JSON并转换为指定类的对象
     * @param file JSON文件
     * @param clazz 目标类
     * @param <T> 返回类型
     * @return 转换后的对象
     */
    public static <T> T parseObject(File file, Class<T> clazz) {
        if (file == null || !file.exists() || !file.isFile()) {
            LogUtils.error("文件不存在或不是有效文件：{}", file != null ? file.getPath() : "null");
            return null;
        }
        if (clazz == null) {
            LogUtils.error("目标类为空，无法转换");
            return null;
        }
        try {
            return objectMapper.readValue(file, clazz);
        } catch (IOException e) {
            LogUtils.error("从文件[{}]中读取JSON转为对象[{}]失败：{}", file.getPath(), clazz.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 将JSON数组字符串转换为指定类型的集合
     * @param jsonArray JSON数组字符串
     * @param reference 目标类型引用
     * @param <T> 返回类型
     * @return 转换后的集合
     */
    public static <T> T parseJSONArray(String jsonArray, TypeReference<T> reference) {
        if (jsonArray == null || jsonArray.trim().isEmpty()) {
            LogUtils.error("JSON数组字符串为空，无法转换");
            return null;
        }
        if (reference == null) {
            LogUtils.error("类型引用为空，无法转换");
            return null;
        }
        try {
            return objectMapper.readValue(jsonArray, reference);
        } catch (JsonProcessingException e) {
            LogUtils.error("JSONArray转为{}类型失败：{}", reference.getType().getTypeName(), e.getMessage());
            return null;
        }
    }

    /**=================================以下是将对象转为JSON=====================================*/
    /**
     * 将对象转换为JSON字符串
     * @param object 要转换的对象
     * @return JSON字符串
     */
    public static String toJSONString(Object object) {
        if (object == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LogUtils.error("对象[{}]转JSON字符串失败：{}", object.getClass().getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 将对象转换为JSON字节数组
     * @param object 要转换的对象
     * @return JSON字节数组
     */
    public static byte[] toByteArray(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            LogUtils.error("对象[{}]转字节数组失败：{}", object.getClass().getName(), e.getMessage());
            return null;
        }
    }

    /**
     * 将对象写入文件
     * @param file 目标文件
     * @param object 要写入的对象
     * @return 是否写入成功
     */
    public static boolean objectToFile(File file, Object object) {
        if (file == null) {
            LogUtils.error("目标文件为空，无法写入");
            return false;
        }
        if (object == null) {
            LogUtils.error("要写入的对象为空");
            return false;
        }
        
        try {
            // 确保父目录存在
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean mkdirsResult = parentDir.mkdirs();
                if (!mkdirsResult) {
                    LogUtils.error("创建父目录失败：{}", parentDir.getPath());
                    return false;
                }
            }
            
            objectMapper.writeValue(file, object);
            return true;
        } catch (JsonProcessingException e) {
            LogUtils.error("Object写入文件失败：{}", e.getMessage());
            return false;
        } catch (IOException e) {
            LogUtils.error("Object写入文件IO异常：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 将对象转换为格式化的JSON字符串
     * @param object 要转换的对象
     * @return 格式化的JSON字符串
     */
    public static String toPrettyJSONString(Object object) {
        if (object == null) {
            return "null";
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LogUtils.error("对象转格式化JSON失败：{}", e.getMessage());
            return null;
        }
    }

    /**=============================以下是与JsonNode相关的=======================================*/
    public static JsonNode parseJSONObject(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            LogUtils.error("JSON字符串为空，无法转换为JsonNode");
            return null;
        }
        JsonNode jsonNode = null;
        try {
            jsonNode = objectMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            LogUtils.error("JSONString转为JsonNode失败：{}", e.getMessage());
        }
        return jsonNode;
    }

    public static JsonNode parseJSONObject(Object object) {
        if (object == null) {
            LogUtils.error("对象为空，无法转换为JsonNode");
            return null;
        }
        return objectMapper.valueToTree(object);
    }

    public static String toJSONString(JsonNode jsonNode) {
        if (jsonNode == null) {
            LogUtils.error("JsonNode为空，无法转换为JSON字符串");
            return null;
        }
        String jsonString = null;
        try {
            jsonString = objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            LogUtils.error("JsonNode转JSONString失败：{}", e.getMessage());
        }
        return jsonString;
    }
    
    /**
     * 美化JSON字符串，格式化输出
     * @param jsonString 原始JSON字符串
     * @return 格式化后的JSON字符串
     */
    public static String prettyPrint(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            LogUtils.error("JSON字符串为空，无法美化");
            return jsonString;
        }
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
        } catch (JsonProcessingException e) {
            LogUtils.error("美化JSON字符串失败：{}", e.getMessage());
            return jsonString;
        }
    }
    
    /**
     * 获取当前使用的ObjectMapper实例（用于自定义配置）
     * @return ObjectMapper实例
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    //JsonNode是一个抽象类，不能实例化，创建JSON树形模型，得用JsonNode的子类ObjectNode，用法和JSONObject大同小异
    public static ObjectNode newJSONObject() {
        return objectMapper.createObjectNode();
    }

    //创建JSON数组对象，就像JSONArray一样用
    public static ArrayNode newJSONArray() {
        return objectMapper.createArrayNode();
    }


}





