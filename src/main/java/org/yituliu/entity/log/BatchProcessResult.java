package org.yituliu.entity.log;



import java.util.List;


public class BatchProcessResult {
    private int successCount;
    private int duplicatedCount;
    private int failedCount;
    private String message;
    private String errorMessage;
    private List<String> errorMessages;

    public BatchProcessResult() {
    }

    public BatchProcessResult(int successCount, int duplicatedCount, int failedCount, String message, String errorMessage, List<String> errorMessages) {
        this.successCount = successCount;
        this.duplicatedCount = duplicatedCount;
        this.failedCount = failedCount;
        this.message = message;
        this.errorMessage = errorMessage;
        this.errorMessages = errorMessages;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getDuplicatedCount() {
        return duplicatedCount;
    }

    public void setDuplicatedCount(int duplicatedCount) {
        this.duplicatedCount = duplicatedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    @Override
    public String toString() {
        return "BatchProcessResult{" +
                "successCount=" + successCount +
                ", duplicatedCount=" + duplicatedCount +
                ", failedCount=" + failedCount +
                ", message='" + message + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorMessages=" + errorMessages +
                '}';
    }
}