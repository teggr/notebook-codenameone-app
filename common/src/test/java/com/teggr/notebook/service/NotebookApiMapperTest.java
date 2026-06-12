package com.teggr.notebook.service;

import com.teggr.notebook.model.NoteDetail;
import com.teggr.notebook.model.NoteSummary;
import com.teggr.notebook.model.SearchResultItem;
import com.teggr.notebook.model.SettingsData;
import com.teggr.notebook.model.SyncResult;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotebookApiMapperTest {

    @Test
    void mapsNoteSummaries() {
        Map row = new HashMap();
        row.put("id", "n-1");
        row.put("title", "My Note");
        row.put("lastModifiedFormatted", "Jun 12, 2026");

        List<NoteSummary> summaries = NotebookApiMapper.toNoteSummaries(Arrays.asList(row));

        assertEquals(1, summaries.size());
        assertEquals("n-1", summaries.get(0).getId());
        assertEquals("My Note", summaries.get(0).getTitle());
        assertEquals("Jun 12, 2026", summaries.get(0).getLastModifiedFormatted());
    }

    @Test
    void mapsNoteDetailSettingsAndSync() {
        Map noteMap = new HashMap();
        noteMap.put("id", "n-2");
        noteMap.put("title", "Details");
        noteMap.put("content", "# Details");
        noteMap.put("lastModified", "2026-06-12T15:00:00Z");

        Map settingsMap = new HashMap();
        settingsMap.put("remoteUrl", "https://example/repo.git");
        settingsMap.put("token", "abc");

        Map syncMap = new HashMap();
        syncMap.put("status", "ok");
        syncMap.put("message", "Synced successfully");

        NoteDetail note = NotebookApiMapper.toNoteDetail(noteMap);
        SettingsData settings = NotebookApiMapper.toSettings(settingsMap);
        SyncResult syncResult = NotebookApiMapper.toSyncResult(syncMap);

        assertEquals("n-2", note.getId());
        assertEquals("# Details", note.getContent());
        assertEquals("https://example/repo.git", settings.getRemoteUrl());
        assertTrue(syncResult.isOk());
        assertEquals("Synced successfully", syncResult.getMessage());
    }

    @Test
    void mapsSearchResults() {
        Map row = new HashMap();
        row.put("id", "n-3");
        row.put("title", "Find Me");
        row.put("snippet", "...search text...");
        row.put("lastModifiedFormatted", "Jun 1, 2026");

        List<SearchResultItem> rows = NotebookApiMapper.toSearchResults(Arrays.asList(row));

        assertEquals(1, rows.size());
        assertEquals("n-3", rows.get(0).getId());
        assertEquals("Find Me", rows.get(0).getTitle());
        assertEquals("...search text...", rows.get(0).getSnippet());
    }
}
