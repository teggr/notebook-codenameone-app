package com.teggr.notebook.service;

import com.teggr.notebook.model.NoteDetail;
import com.teggr.notebook.model.NoteSummary;
import com.teggr.notebook.model.SearchResultItem;
import com.teggr.notebook.model.SettingsData;
import com.teggr.notebook.model.SyncResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class NotebookApiMapper {
    private NotebookApiMapper() {
    }

    static List<NoteSummary> toNoteSummaries(List rows) {
        if (rows == null) {
            return Collections.emptyList();
        }
        List<NoteSummary> out = new ArrayList<NoteSummary>();
        for (Object row : rows) {
            if (!(row instanceof Map)) {
                continue;
            }
            Map map = (Map) row;
            out.add(new NoteSummary(
                    asString(map.get("id")),
                    asString(map.get("title")),
                    asString(map.get("lastModifiedFormatted"))
            ));
        }
        return out;
    }

    static List<SearchResultItem> toSearchResults(List rows) {
        if (rows == null) {
            return Collections.emptyList();
        }
        List<SearchResultItem> out = new ArrayList<SearchResultItem>();
        for (Object row : rows) {
            if (!(row instanceof Map)) {
                continue;
            }
            Map map = (Map) row;
            out.add(new SearchResultItem(
                    asString(map.get("id")),
                    asString(map.get("title")),
                    asString(map.get("snippet")),
                    asString(map.get("lastModifiedFormatted"))
            ));
        }
        return out;
    }

    static NoteDetail toNoteDetail(Map map) {
        return new NoteDetail(
                asString(map.get("id")),
                asString(map.get("title")),
                asString(map.get("content")),
                asString(map.get("lastModified"))
        );
    }

    static SettingsData toSettings(Map map) {
        return new SettingsData(asString(map.get("remoteUrl")), asString(map.get("token")));
    }

    static SyncResult toSyncResult(Map map) {
        return new SyncResult(asString(map.get("status")), asString(map.get("message")));
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
