package com.teggr.notebook.ui;

class NotebookUiState {
    private boolean notesStale = true;
    private boolean searchStale;
    private boolean hasUnsyncedChanges;
    private String lastSearchQuery;

    boolean isNotesStale() {
        return notesStale;
    }

    boolean isSearchStale() {
        return searchStale;
    }

    boolean hasUnsyncedChanges() {
        return hasUnsyncedChanges;
    }

    String getLastSearchQuery() {
        return lastSearchQuery;
    }

    boolean hasSearchQuery() {
        return lastSearchQuery != null && lastSearchQuery.trim().length() > 0;
    }

    void rememberSearchQuery(String query) {
        if (query == null || query.trim().length() == 0) {
            lastSearchQuery = null;
            searchStale = false;
            return;
        }
        lastSearchQuery = query.trim();
    }

    void markMutationOptimistic() {
        hasUnsyncedChanges = true;
        searchStale = hasSearchQuery();
    }

    void markSynced() {
        hasUnsyncedChanges = false;
        markNeedsRefresh();
    }

    void markNeedsRefresh() {
        notesStale = true;
        searchStale = hasSearchQuery();
    }

    void markNotesRefreshed() {
        notesStale = false;
    }

    void markSearchRefreshed() {
        searchStale = false;
    }
}
