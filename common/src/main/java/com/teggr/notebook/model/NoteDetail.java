package com.teggr.notebook.model;

public class NoteDetail {
    private final String id;
    private final String title;
    private final String content;
    private final String lastModified;

    public NoteDetail(String id, String title, String content, String lastModified) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.lastModified = lastModified;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getLastModified() {
        return lastModified;
    }
}
