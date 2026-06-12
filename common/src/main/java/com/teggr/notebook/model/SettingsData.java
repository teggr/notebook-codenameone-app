package com.teggr.notebook.model;

public class SettingsData {
    private final String remoteUrl;
    private final String token;

    public SettingsData(String remoteUrl, String token) {
        this.remoteUrl = remoteUrl;
        this.token = token;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public String getToken() {
        return token;
    }
}
