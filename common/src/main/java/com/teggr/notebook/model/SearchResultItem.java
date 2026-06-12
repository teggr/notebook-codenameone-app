package com.teggr.notebook.model;

public class SearchResultItem {
    private final String id;
    private final String title;
    private final String snippet;
    private final String lastModifiedFormatted;

    public SearchResultItem(String id, String title, String snippet, String lastModifiedFormatted) {
        this.id = id;
        this.title = title;
        this.snippet = snippet;
        this.lastModifiedFormatted = lastModifiedFormatted;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public String getLastModifiedFormatted() {
        return lastModifiedFormatted;
    }
}
