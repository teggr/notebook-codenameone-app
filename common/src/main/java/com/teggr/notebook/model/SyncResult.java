package com.teggr.notebook.model;

public class SyncResult {
    private final String status;
    private final String message;

    public SyncResult(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public boolean isOk() {
        return "ok".equalsIgnoreCase(status);
    }
}
