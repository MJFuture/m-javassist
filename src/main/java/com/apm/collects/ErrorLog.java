package com.apm.collects;

import com.apm.init.NotProguard;

/**
 * 错误信息统计
 */
@NotProguard
public class ErrorLog {
    private String logType;
    private String statck;
    private String errorMsg;
    private String errorType;
    private String ip;
    private String keyId;
    private Long createTime;

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public String getStatck() {
        return statck;
    }

    public void setStatck(String statck) {
        this.statck = statck;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }
}
