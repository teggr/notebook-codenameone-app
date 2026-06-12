package com.teggr.notebook.model;

public class NoteSummary {
    private final String id;
    private final String title;
    private final String lastModifiedFormatted;

    public NoteSummary(String id, String title, String lastModifiedFormatted) {
        this.id = id;
        this.title = title;
        this.lastModifiedFormatted = lastModifiedFormatted;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getLastModifiedFormatted() {
        return lastModifiedFormatted;
    }
}
