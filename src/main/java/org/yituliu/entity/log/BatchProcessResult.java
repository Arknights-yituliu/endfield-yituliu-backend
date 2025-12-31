package org.yituliu.entity.log;

import lombok.Data;

import java.util.List;

@Data
public class BatchProcessResult {
    private int successCount;
    private int duplicatedCount;
    private int failedCount;
    private String message;
    private String errorMessage;
    private List<String> errorMessages;
}
