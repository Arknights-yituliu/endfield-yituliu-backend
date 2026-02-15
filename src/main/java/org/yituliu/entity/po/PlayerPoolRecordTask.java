package org.yituliu.entity.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@TableName("player_pool_record_task")
@Data
public class PlayerPoolRecordTask {
    @TableId
    private String taskId;
    private String token;
    private Boolean completeFlag;
    private String roleId;
    private Boolean startFlag;
    private Date createTime;
    private Date updateTime;

}
