package com.teggr.notebook.ui;

import static com.codename1.ui.CN.callSerially;

import com.codename1.components.InfiniteProgress;
import com.codename1.l10n.L10NManager;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.util.UITimer;
import com.teggr.notebook.model.NoteDetail;
import com.teggr.notebook.model.NoteSummary;
import com.teggr.notebook.model.SearchResultItem;
import com.teggr.notebook.model.SettingsData;
import com.teggr.notebook.model.SyncResult;
import com.teggr.notebook.service.NotebookApiClient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AppShellForm extends Form {
    private final NotebookApiClient api;
    private final NotebookUiState uiState = new NotebookUiState();

    private final Tabs tabs;
    private final Container notesList;
    private final Label notesStatus;
    private final Label syncHint;
    private final Button notesRetryButton;
    private final TextField notesSearchField;

    private final TextField searchField;
    private final Button searchCancelButton;
    private final Container searchResults;
    private final Label searchStatus;
    private final Button searchRetryButton;

    private final TextField remoteUrlField;
    private final TextField tokenField;
    private final Label settingsStatus;
    private final Label lastSyncLabel;
    private final Button settingsRetryButton;

    private final List<NoteSummary> currentNotes = new ArrayList<NoteSummary>();
    private boolean notesRefreshInFlight;
    private boolean syncingSearchFields;
    private UITimer searchDebounceTimer;

    public AppShellForm(NotebookApiClient api) {
        super("All Notes", new BorderLayout());
        this.api = api;
        getToolbar().setTitleCentered(true);

        tabs = new Tabs();
        tabs.setTabPlacement(Component.BOTTOM);
        tabs.setUIID("NotebookTabs");

        notesList = BoxLayout.encloseY();
        notesList.setUIID("NotebookSection");
        notesStatus = new Label("Loading notes...");
        notesStatus.setUIID("StatusInfo");
        syncHint = new Label("");
        syncHint.setUIID("StatusInfo");
        notesRetryButton = new Button("Retry Notes");
        notesRetryButton.setUIID("SecondaryActionButton");
        notesRetryButton.setVisible(false);
        notesRetryButton.addActionListener(e -> loadNotes());
        notesSearchField = new TextField("", "Search notes or tags", 24, TextField.ANY);
        notesSearchField.setUIID("NotebookSearchField");
        Container notesTab = buildNotesTab();

        searchField = new TextField("", "Search notes or tags", 24, TextField.ANY);
        searchField.setUIID("NotebookSearchField");
        searchCancelButton = new Button("Cancel");
        searchCancelButton.setUIID("SearchCancelButton");
        searchCancelButton.setVisible(false);
        searchCancelButton.setHidden(true);
        searchResults = BoxLayout.encloseY();
        searchResults.setUIID("NotebookSection");
        searchStatus = new Label("Search note title and content");
        searchStatus.setUIID("StatusInfo");
        searchRetryButton = new Button("Retry Search");
        searchRetryButton.setUIID("SecondaryActionButton");
        searchRetryButton.setVisible(false);
        searchRetryButton.addActionListener(e -> runSearch(uiState.getLastSearchQuery(), false));
        Container searchTab = buildSearchTab();

        remoteUrlField = new TextField("", "https://github.com/user/repo.git", 30, TextField.URL);
        remoteUrlField.setUIID("NotebookTextField");
        tokenField = new TextField("", "ghp_...", 24, TextField.PASSWORD);
        tokenField.setUIID("NotebookTextField");
        settingsStatus = new Label("");
        settingsStatus.setUIID("StatusInfo");
        lastSyncLabel = new Label("Last sync: never");
        lastSyncLabel.setUIID("StatusInfo");
        settingsRetryButton = new Button("Retry Settings Load");
        settingsRetryButton.setUIID("SecondaryActionButton");
        settingsRetryButton.setVisible(false);
        settingsRetryButton.addActionListener(e -> loadSettings());
        Container settingsTab = buildSettingsTab();

        tabs.addTab("Notes", notesTab);
        tabs.addTab("Search", searchTab);
        tabs.addTab("Settings", settingsTab);

        tabs.addSelectionListener((oldSelected, selected) -> {
            applyShellTitle(selected);
            if (selected == 0 && uiState.isNotesStale()) {
                loadNotes();
            }
            if (selected == 1 && uiState.isSearchStale() && uiState.hasSearchQuery()) {
                runSearch(uiState.getLastSearchQuery(), true);
            }
            if (selected == 2) {
                loadSettings();
            }
        });

        add(BorderLayout.CENTER, tabs);

        installToolbarCommands();
        wireSearchInteractions();

        updateUnsyncedHint();
        loadNotes();
    }

    private void installToolbarCommands() {
        getToolbar().addMaterialCommandToLeftBar("", FontImage.MATERIAL_MENU, e ->
                Dialog.show("Notebook", "Use the tabs below to switch sections.", "OK", null));
        getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_CREATE, e -> openEditor(null));
        getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_SYNC, e -> syncNow());
        applyShellTitle(tabs.getSelectedIndex());
    }

    private void applyShellTitle(int selectedTab) {
        if (selectedTab == 2) {
            setTitle("Settings");
        } else {
            setTitle("All Notes");
        }
    }

    private void wireSearchInteractions() {
        notesSearchField.addActionListener(e -> activateSearch(notesSearchField.getText(), true));
        notesSearchField.addDataChangedListener((type, index) -> {
            if (syncingSearchFields) {
                return;
            }
            String query = notesSearchField.getText();
            if (query != null && query.trim().length() > 0) {
                activateSearch(query, true);
            }
        });

        searchField.addActionListener(e -> runSearch(searchField.getText(), false));
        searchField.addDataChangedListener((type, index) -> {
            if (syncingSearchFields) {
                return;
            }
            String query = searchField.getText();
            syncSearchFieldText(notesSearchField, query);
            updateSearchCancelButton();
            scheduleSearch(query);
        });

        searchCancelButton.addActionListener(e -> {
            clearSearchState();
            tabs.setSelectedIndex(0);
        });
    }

    private Container buildNotesTab() {
        Container searchRow = createSearchRow(notesSearchField, null);

        Container content = BoxLayout.encloseY(searchRow, syncHint, notesStatus, notesRetryButton, notesList);
        content.setUIID("NotebookTab");
        content.setScrollableY(true);
        return content;
    }

    private Container buildSearchTab() {
        Container searchRow = createSearchRow(searchField, searchCancelButton);

        Container content = BoxLayout.encloseY(searchRow, searchStatus, searchRetryButton, searchResults);
        content.setUIID("NotebookTab");
        content.setScrollableY(true);
        return content;
    }

    private Container createSearchRow(TextField field, Button cancelButton) {
        Label icon = new Label("\uD83D\uDD0D");
        icon.setUIID("SearchIcon");

        Container searchShell = new Container(new BorderLayout());
        searchShell.setUIID("SearchFieldShell");
        searchShell.add(BorderLayout.WEST, icon);
        searchShell.add(BorderLayout.CENTER, field);

        Container row = new Container(new BorderLayout());
        row.setUIID("NotebookSearchRow");
        row.add(BorderLayout.CENTER, searchShell);
        if (cancelButton != null) {
            row.add(BorderLayout.EAST, cancelButton);
        }
        return row;
    }

    private Container buildSettingsTab() {
        Button saveSettings = new Button("Save Settings");
        saveSettings.setUIID("PrimaryActionButton");
        saveSettings.addActionListener(e -> saveSettings());

        Button syncButton = new Button("Sync");
        syncButton.setUIID("SecondaryActionButton");
        syncButton.addActionListener(e -> syncNow());

        Label remoteLabel = new Label("Remote URL");
        remoteLabel.setUIID("FieldLabel");
        Label tokenLabel = new Label("Token");
        tokenLabel.setUIID("FieldLabel");

        Container content = BoxLayout.encloseY(
                remoteLabel,
                remoteUrlField,
                tokenLabel,
                tokenField,
                GridLayout.encloseIn(2, saveSettings, syncButton),
                settingsStatus,
                lastSyncLabel,
                settingsRetryButton
        );
        content.setUIID("NotebookTab");
        content.setScrollableY(true);
        return content;
    }

    private void activateSearch(String query, boolean switchToSearchTab) {
        syncSearchFieldText(searchField, query);
        updateSearchCancelButton();
        if (switchToSearchTab && tabs.getSelectedIndex() != 1) {
            tabs.setSelectedIndex(1);
        }
        scheduleSearch(query);
    }

    private void scheduleSearch(String query) {
        if (searchDebounceTimer != null) {
            searchDebounceTimer.cancel();
            searchDebounceTimer = null;
        }
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() == 0) {
            runSearch("", false);
            return;
        }
        searchDebounceTimer = new UITimer(() -> runSearch(trimmed, false));
        searchDebounceTimer.schedule(260, false, this);
    }

    private void clearSearchState() {
        syncSearchFieldText(searchField, "");
        syncSearchFieldText(notesSearchField, "");
        updateSearchCancelButton();
        runSearch("", false);
        Display.getInstance().hideNativeKeyboard();
    }

    private void syncSearchFieldText(TextField target, String value) {
        String normalized = value == null ? "" : value;
        if (normalized.equals(target.getText())) {
            return;
        }
        syncingSearchFields = true;
        target.setText(normalized);
        syncingSearchFields = false;
    }

    private void updateSearchCancelButton() {
        String query = searchField.getText();
        boolean visible = query != null && query.trim().length() > 0;
        searchCancelButton.setVisible(visible);
        searchCancelButton.setHidden(!visible);
        revalidate();
    }

    private void loadNotes() {
        updateUnsyncedHint();
        notesRetryButton.setVisible(false);
        setStatus(notesStatus, "Loading notes...", "StatusInfo");
        notesList.removeAll();
        revalidate();

        fetchNotes(() -> {
        }, true);
    }

    private void refreshNotesSilently() {
        if (notesRefreshInFlight) {
            return;
        }
        fetchNotes(() -> {
        }, false);
    }

    private void fetchNotes(Runnable completion, boolean showErrorInStatus) {
        notesRefreshInFlight = true;
        Display.getInstance().startThread(() -> {
            try {
                List<NoteSummary> rows = api.listNotes();
                callSerially(() -> {
                    currentNotes.clear();
                    currentNotes.addAll(rows);
                    renderNotesList();
                    uiState.markNotesRefreshed();
                    notesRefreshInFlight = false;
                    completion.run();
                });
            } catch (Exception ex) {
                callSerially(() -> {
                    notesRefreshInFlight = false;
                    completion.run();
                    if (showErrorInStatus) {
                        setStatus(notesStatus, friendlyError("Failed to load notes", ex), "StatusError");
                        notesRetryButton.setVisible(true);
                        revalidate();
                    }
                });
            }
        }, "load-notes").start();
    }

    private void renderNotesList() {
        notesList.removeAll();
        if (currentNotes.isEmpty()) {
            setStatus(notesStatus, "No notes yet.", "StatusInfo");
        } else {
            setStatus(notesStatus, "", "StatusInfo");
            for (NoteSummary row : currentNotes) {
                notesList.add(createListRow(row.getTitle(), row.getLastModifiedFormatted(), row.getId()));
            }
        }
        revalidate();
    }

    private Container createListRow(String title, String subtitle, String noteId) {
        Label titleLabel = new Label(title == null || title.trim().length() == 0 ? "Untitled" : title.trim());
        titleLabel.setUIID("ListRowTitle");
        titleLabel.setEndsWith3Points(true);
        titleLabel.setTickerEnabled(false);

        Label subtitleLabel = new Label(shorten(subtitle, 92));
        subtitleLabel.setUIID("ListRowSubtitle");
        subtitleLabel.setEndsWith3Points(true);
        subtitleLabel.setTickerEnabled(false);

        Container textBox = BoxLayout.encloseY(titleLabel, subtitleLabel);
        textBox.setUIID("ListRowText");

        Label chevron = new Label(">");
        chevron.setUIID("RowChevron");

        Container row = new Container(new BorderLayout());
        row.setUIID("ListRow");
        row.add(BorderLayout.CENTER, textBox);
        row.add(BorderLayout.EAST, chevron);

        Container separator = new Container();
        separator.setUIID("RowSeparator");

        Container wrappedRow = BoxLayout.encloseY(row, separator);
        wrappedRow.setUIID("ListRowWrap");

        Runnable openAction = () -> openEditor(noteId);
        row.addPointerReleasedListener(e -> openAction.run());
        titleLabel.addPointerReleasedListener(e -> openAction.run());
        subtitleLabel.addPointerReleasedListener(e -> openAction.run());
        chevron.addPointerReleasedListener(e -> openAction.run());
        wrappedRow.addPointerReleasedListener(e -> openAction.run());

        return wrappedRow;
    }

    private void runSearch(String query, boolean isBackgroundRefresh) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.length() == 0) {
            searchResults.removeAll();
            setStatus(searchStatus, "Search note title and content", "StatusInfo");
            searchRetryButton.setVisible(false);
            uiState.rememberSearchQuery(null);
            uiState.markSearchRefreshed();
            revalidate();
            return;
        }

        uiState.rememberSearchQuery(normalized);
        searchRetryButton.setVisible(false);

        setStatus(searchStatus, isBackgroundRefresh ? "Refreshing search..." : "Searching...", "StatusInfo");
        searchResults.removeAll();
        revalidate();

        Display.getInstance().startThread(() -> {
            try {
                List<SearchResultItem> rows = api.search(normalized);
                callSerially(() -> {
                    searchResults.removeAll();
                    if (rows.isEmpty()) {
                        setStatus(searchStatus, "No matches found.", "StatusInfo");
                    } else {
                        setStatus(searchStatus, "", "StatusInfo");
                        for (SearchResultItem row : rows) {
                            String subtitle = shorten(row.getSnippet(), 110) + "  " + row.getLastModifiedFormatted();
                            searchResults.add(createListRow(row.getTitle(), subtitle, row.getId()));
                        }
                    }
                    uiState.markSearchRefreshed();
                    revalidate();
                });
            } catch (Exception ex) {
                callSerially(() -> {
                    setStatus(searchStatus, friendlyError("Search failed", ex), "StatusError");
                    searchRetryButton.setVisible(true);
                    revalidate();
                });
            }
        }, "search-notes").start();
    }

    private void loadSettings() {
        settingsRetryButton.setVisible(false);
        setStatus(settingsStatus, "Loading settings...", "StatusInfo");
        revalidate();

        Display.getInstance().startThread(() -> {
            try {
                SettingsData settings = api.getSettings();
                callSerially(() -> {
                    remoteUrlField.setText(settings.getRemoteUrl());
                    tokenField.setText(settings.getToken());
                    setStatus(settingsStatus, "Settings loaded", "StatusSuccess");
                    revalidate();
                });
            } catch (Exception ex) {
                callSerially(() -> {
                    setStatus(settingsStatus, friendlyError("Failed to load settings", ex), "StatusError");
                    settingsRetryButton.setVisible(true);
                    revalidate();
                });
            }
        }, "load-settings").start();
    }

    private void saveSettings() {
        setStatus(settingsStatus, "Saving settings...", "StatusInfo");
        revalidate();

        Display.getInstance().startThread(() -> {
            try {
                api.saveSettings(remoteUrlField.getText(), tokenField.getText());
                callSerially(() -> {
                    setStatus(settingsStatus, "Settings saved", "StatusSuccess");
                    settingsRetryButton.setVisible(false);
                    revalidate();
                });
            } catch (Exception ex) {
                callSerially(() -> {
                    setStatus(settingsStatus, friendlyError("Failed to save settings", ex), "StatusError");
                    revalidate();
                });
            }
        }, "save-settings").start();
    }

    private void syncNow() {
        Dialog progress = new InfiniteProgress().showInifiniteBlocking();
        Display.getInstance().startThread(() -> {
            try {
                SyncResult result = api.sync();
                callSerially(() -> {
                    progress.dispose();
                    setStatus(settingsStatus, result.getMessage(), result.isOk() ? "StatusSuccess" : "StatusError");
                    if (result.isOk()) {
                        uiState.markSynced();
                        lastSyncLabel.setText("Last sync: " + L10NManager.getInstance().formatDateTime(new Date()));
                        updateUnsyncedHint();
                        refreshNotesSilently();
                    }
                    if (result.isOk()) {
                        Dialog.show("Sync", result.getMessage(), "OK", null);
                    } else {
                        Dialog.show("Sync Error", result.getMessage(), "OK", null);
                    }
                });
            } catch (Exception ex) {
                callSerially(() -> {
                    progress.dispose();
                    Dialog.show("Sync failed", friendlyError("Unable to sync", ex), "OK", null);
                });
            }
        }, "sync").start();
    }

    private void openEditor(String noteId) {
        try {
            NoteEditorForm editor = new NoteEditorForm(this, api, noteId, new NoteEditorForm.SaveListener() {
                @Override
                public void onSaved(NoteDetail note, boolean created) {
                    applyOptimisticUpsert(note);
                    uiState.markMutationOptimistic();
                    updateUnsyncedHint();
                    refreshNotesSilently();
                }

                @Override
                public void onDeleted(String deletedNoteId) {
                    applyOptimisticDelete(deletedNoteId);
                    uiState.markMutationOptimistic();
                    updateUnsyncedHint();
                    refreshNotesSilently();
                }
            });
            editor.show();
        } catch (RuntimeException ex) {
            Dialog.show("Open note failed", ex.toString(), "OK", null);
        }
    }

    private void applyOptimisticUpsert(NoteDetail note) {
        if (note == null) {
            return;
        }
        int existingIndex = indexOfNote(note.getId());
        if (existingIndex >= 0) {
            currentNotes.remove(existingIndex);
        }
        currentNotes.add(0, new NoteSummary(note.getId(), note.getTitle(), "Just now"));
        uiState.markNotesRefreshed();
        if (tabs.getSelectedIndex() == 0) {
            renderNotesList();
        }
    }

    private void applyOptimisticDelete(String noteId) {
        int existingIndex = indexOfNote(noteId);
        if (existingIndex >= 0) {
            currentNotes.remove(existingIndex);
            uiState.markNotesRefreshed();
            if (tabs.getSelectedIndex() == 0) {
                renderNotesList();
            }
        }
    }

    private int indexOfNote(String noteId) {
        for (int i = 0; i < currentNotes.size(); i++) {
            if (currentNotes.get(i).getId().equals(noteId)) {
                return i;
            }
        }
        return -1;
    }

    private void updateUnsyncedHint() {
        if (uiState.hasUnsyncedChanges()) {
            setStatus(syncHint, "Local changes pending sync", "StatusWarning");
        } else {
            setStatus(syncHint, "", "StatusInfo");
        }
    }

    private static String friendlyError(String prefix, Exception ex) {
        String message = ex == null ? "Unknown error" : ex.getMessage();
        if (message == null || message.length() == 0) {
            return prefix + ": unknown error";
        }
        if (message.indexOf("HTTP 404") >= 0) {
            return prefix + ": item not found";
        }
        if (message.indexOf("HTTP 400") >= 0) {
            return prefix + ": invalid request";
        }
        if (message.indexOf("HTTP 401") >= 0 || message.indexOf("HTTP 403") >= 0) {
            return prefix + ": access denied";
        }
        if (message.indexOf("HTTP 500") >= 0 || message.indexOf("HTTP 502") >= 0 || message.indexOf("HTTP 503") >= 0) {
            return prefix + ": server error";
        }
        if (message.indexOf("HTTP 0") >= 0 || message.indexOf("Connection") >= 0 || message.indexOf("timeout") >= 0) {
            return prefix + ": API unavailable, check localhost:8080";
        }
        return prefix + ": " + message;
    }

    private static String shorten(String value, int max) {
        String text = value == null ? "" : value.trim();
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max - 1) + "...";
    }

    private static void setStatus(Label label, String text, String uiid) {
        label.setText(text == null ? "" : text);
        label.setUIID(uiid);
    }
}
