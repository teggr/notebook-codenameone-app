package com.teggr.notebook.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotebookUiStateTest {

    @Test
    void mutationSetsUnsyncedAndSearchStaleWhenQueryExists() {
        NotebookUiState state = new NotebookUiState();
        state.rememberSearchQuery("term");
        state.markMutationOptimistic();

        assertTrue(state.hasUnsyncedChanges());
        assertTrue(state.isSearchStale());
        assertTrue(state.isNotesStale());
    }

    @Test
    void notesAndSearchFlagsCanBeClearedAfterRefresh() {
        NotebookUiState state = new NotebookUiState();
        state.rememberSearchQuery("term");
        state.markNeedsRefresh();

        state.markNotesRefreshed();
        state.markSearchRefreshed();

        assertFalse(state.isNotesStale());
        assertFalse(state.isSearchStale());
    }

    @Test
    void syncClearsUnsyncedAndMarksForRefresh() {
        NotebookUiState state = new NotebookUiState();
        state.rememberSearchQuery("term");
        state.markMutationOptimistic();

        state.markSynced();

        assertFalse(state.hasUnsyncedChanges());
        assertTrue(state.isNotesStale());
        assertTrue(state.isSearchStale());
    }
}
