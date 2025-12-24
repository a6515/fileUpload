package com.seeyon.apps.pdfCatchButton.vo;

import java.io.Serializable;

public class TaskProgress implements Serializable {
    private String status;  // 状态: RUNNING, SUCCESS, ERROR
    private int percent;    // 进度百分比: 0-100
    private String message; // 当前提示信息

    public TaskProgress() {}

    public TaskProgress(String status, int percent, String message) {
        this.status = status;
        this.percent = percent;
        this.message = message;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getPercent() { return percent; }
    public void setPercent(int percent) { this.percent = percent; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}