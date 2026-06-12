package com.teggr.notebook.service;

import com.codename1.io.CharArrayReader;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.util.StringUtil;
import com.teggr.notebook.model.NoteDetail;
import com.teggr.notebook.model.NoteSummary;
import com.teggr.notebook.model.SearchResultItem;
import com.teggr.notebook.model.SettingsData;
import com.teggr.notebook.model.SyncResult;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class NotebookApiClient {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private final String baseUrl;

    public NotebookApiClient() {
        this(DEFAULT_BASE_URL);
    }

    public NotebookApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<NoteSummary> listNotes() throws IOException {
        String json = sendJson("GET", "/api/notes", null, false);
        List rows = parseArray(json);
        return NotebookApiMapper.toNoteSummaries(rows);
    }

    public NoteDetail getNote(String id) throws IOException {
        String json = sendJson("GET", "/api/notes/" + encodePath(id), null, false);
        Map map = parseObject(json);
        return NotebookApiMapper.toNoteDetail(map);
    }

    public NoteDetail createNote(String content) throws IOException {
        String body = "{\"content\":\"" + escapeJson(defaultContent(content)) + "\"}";
        String json = sendJson("POST", "/api/notes", body, false);
        Map map = parseObject(json);
        return NotebookApiMapper.toNoteDetail(map);
    }

    public NoteDetail updateNote(String id, String content) throws IOException {
        String body = "{\"content\":\"" + escapeJson(defaultContent(content)) + "\"}";
        String json = sendJson("PUT", "/api/notes/" + encodePath(id), body, false);
        Map map = parseObject(json);
        return NotebookApiMapper.toNoteDetail(map);
    }

    public void deleteNote(String id) throws IOException {
        sendJson("DELETE", "/api/notes/" + encodePath(id), null, true);
    }

    public List<SearchResultItem> search(String query) throws IOException {
        if (query == null || query.trim().length() == 0) {
            return Collections.emptyList();
        }
        String encodedQuery = Util.encodeUrl(query.trim());
        String json = sendJson("GET", "/api/notes/search?q=" + encodedQuery + "&limit=10", null, false);
        List rows = parseArray(json);
        return NotebookApiMapper.toSearchResults(rows);
    }

    public SettingsData getSettings() throws IOException {
        String json = sendJson("GET", "/api/settings", null, false);
        Map map = parseObject(json);
        return NotebookApiMapper.toSettings(map);
    }

    public SettingsData saveSettings(String remoteUrl, String token) throws IOException {
        String body = "{\"remoteUrl\":\"" + escapeJson(defaultString(remoteUrl))
                + "\",\"token\":\"" + escapeJson(defaultString(token)) + "\"}";
        String json = sendJson("PUT", "/api/settings", body, false);
        Map map = parseObject(json);
        return NotebookApiMapper.toSettings(map);
    }

    public SyncResult sync() throws IOException {
        String json = sendJson("POST", "/api/git/sync", "{}", false);
        Map map = parseObject(json);
        return NotebookApiMapper.toSyncResult(map);
    }

    private String sendJson(String method, String path, String jsonBody, boolean allowNoContent) throws IOException {
        ConnectionRequest req = new ConnectionRequest();
        req.setFailSilently(true);
        req.setHttpMethod(method);
        req.setUrl(baseUrl + path);
        req.setTimeout(15000);
        if (jsonBody != null) {
            req.setContentType("application/json");
            req.setRequestBody(jsonBody);
        }

        NetworkManager.getInstance().addToQueueAndWait(req);

        int code = req.getResponseCode();
        byte[] bytes = req.getResponseData();
        String payload = bytes == null ? "" : new String(bytes, "UTF-8");

        if (code >= 200 && code < 300) {
            if (allowNoContent || payload.length() == 0) {
                return "{}";
            }
            return payload;
        }

        if (payload.length() == 0) {
            throw new IOException("HTTP " + code);
        }

        throw new IOException("HTTP " + code + ": " + payload);
    }

    private Map parseObject(String json) throws IOException {
        JSONParser parser = new JSONParser();
        return parser.parseJSON(new CharArrayReader(json.toCharArray()));
    }

    private List parseArray(String json) throws IOException {
        JSONParser parser = new JSONParser();
        Map root = parser.parseJSON(new CharArrayReader(json.toCharArray()));
        Object value = root.get("root");
        if (value instanceof List) {
            return (List) value;
        }
        return Collections.emptyList();
    }

    private static String defaultString(String value) {
        return value == null ? "" : value;
    }

    private static String defaultContent(String value) {
        if (value == null || value.trim().length() == 0) {
            return "# Untitled\n\n";
        }
        return value;
    }

    private static String escapeJson(String value) {
        return StringUtil.replaceAll(
                StringUtil.replaceAll(
                        StringUtil.replaceAll(
                                StringUtil.replaceAll(value, "\\", "\\\\"),
                                "\"", "\\\""
                        ),
                        "\n", "\\n"
                ),
                "\r", ""
        );
    }

    private static String encodePath(String value) {
        return Util.encodeUrl(defaultString(value));
    }
}
