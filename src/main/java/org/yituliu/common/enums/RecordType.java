package org.yituliu.common.enums;

/**
 * 统计记录状态枚举
 * 用于标识预聚合统计表中的数据是否为当前有效的展示数据
 */
public enum RecordType {

    /**
     * 展示数据：接口查询时只读取该状态的数据
     */
    DISPLAY(1, "展示数据"),

    /**
     * 过期数据：重复统计后旧数据被标记为该状态，等待定时任务清理
     */
    EXPIRE(-1, "过期数据");

    private final Integer code;

    private final String recordType;

    RecordType(Integer code, String recordType) {
        this.code = code;
        this.recordType = recordType;
    }

    /**
     * 获取状态码
     *
     * @return 状态码
     */
    public Integer code() {
        return code;
    }

    /**
     * 获取状态描述
     *
     * @return 状态描述
     */
    public String recordType() {
        return recordType;
    }
}
