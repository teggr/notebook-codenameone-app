package com.teggr.notebook.ui;

import static com.codename1.ui.CN.callSerially;

import com.codename1.components.ToastBar;
import com.codename1.ui.Display;
import com.codename1.ui.Button;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import com.codename1.ui.layouts.BorderLayout;
import com.teggr.notebook.model.NoteDetail;
import com.teggr.notebook.service.NotebookApiClient;

public class NoteEditorForm extends Form {
    public interface SaveListener {
        void onSaved(NoteDetail note, boolean created);

        void onDeleted(String noteId);
    }

    private final NotebookApiClient api;
    private final SaveListener saveListener;
    private final String noteId;
    private final TextArea contentArea;

    private boolean dirty;
    private boolean loading;
    private boolean saving;

    public NoteEditorForm(Form backForm, NotebookApiClient api, String noteId, SaveListener saveListener) {
        super(noteId == null ? "New" : "Edit", new BorderLayout());
        this.api = api;
        this.noteId = noteId;
        this.saveListener = saveListener;
        getToolbar().setTitleCentered(false);

        contentArea = new TextArea();
        contentArea.setRows(20);
        contentArea.setGrowByContent(true);
        contentArea.setUIID("NotebookEditorTextArea");
        contentArea.addDataChangedListener((type, index) -> dirty = true);
        contentArea.addPointerPressedListener(e -> {
            if (contentArea.isEditable()) {
                contentArea.requestFocus();
                contentArea.startEditingAsync();
            }
        });

        add(BorderLayout.CENTER, contentArea);

        getToolbar().setBackCommand("Back", e -> {
            if (dirty) {
                boolean discard = Dialog.show("Discard changes?", "You have unsaved changes.", "Discard", "Cancel");
                if (!discard) {
                    return;
                }
            }
            backForm.showBack();
        });

        getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_SAVE, e -> save(backForm, contentArea));
        if (noteId != null) {
            getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_DELETE, e -> deleteNote(backForm));
        }

        if (noteId == null) {
            contentArea.setText("# Untitled\n\n");
            dirty = false;
            Display.getInstance().callSerially(() -> focusEditor(contentArea));
        } else {
            contentArea.setText("Loading note...\n");
            contentArea.setEditable(false);
            Display.getInstance().callSerially(() -> loadExisting(contentArea));
        }
    }

    private void loadExisting(TextArea field) {
        loading = true;
        field.setEditable(false);

        Display.getInstance().startThread(() -> {
            try {
                NoteDetail note = api.getNote(noteId);
                callSerially(() -> {
                    field.setText(note.getContent());
                    field.setEditable(true);
                    focusEditor(field);
                    dirty = false;
                    loading = false;
                });
            } catch (Exception ex) {
                callSerially(() -> {
                    loading = false;
                    field.setEditable(true);
                    Dialog.show("Error", friendlyError("Unable to load note", ex), "OK", null);
                });
            }
        }, "load-note").start();
    }

    private void save(Form backForm, TextArea field) {
        if (loading || saving) {
            return;
        }

        saving = true;

        String content = field.getText();
        com.codename1.ui.Display.getInstance().startThread(() -> {
            try {
                NoteDetail saved;
                if (noteId == null) {
                    saved = api.createNote(content);
                } else {
                    saved = api.updateNote(noteId, content);
                }
                boolean created = noteId == null;
                callSerially(() -> {
                    saving = false;
                    dirty = false;
                    if (saveListener != null) {
                        saveListener.onSaved(saved, created);
                    }
                    ToastBar.showInfoMessage("Saved");
                    backForm.showBack();
                });
            } catch (Exception ex) {
                callSerially(() -> {
                    saving = false;
                    Dialog.show("Save failed", friendlyError("Save failed", ex), "OK", null);
                });
            }
        }, "save-note").start();
    }

    private void deleteNote(Form backForm) {
        if (noteId == null) {
            return;
        }

        boolean confirmed = Dialog.show("Delete note", "This action cannot be undone.", "Delete", "Cancel");
        if (!confirmed) {
            return;
        }

        Dialog progress = new InfiniteProgress().showInifiniteBlocking();
        com.codename1.ui.Display.getInstance().startThread(() -> {
            try {
                api.deleteNote(noteId);
                callSerially(() -> {
                    progress.dispose();
                    dirty = false;
                    if (saveListener != null) {
                        saveListener.onDeleted(noteId);
                    }
                    ToastBar.showInfoMessage("Deleted");
                    backForm.showBack();
                });
            } catch (Exception ex) {
                callSerially(() -> {
                    progress.dispose();
                    Dialog.show("Delete failed", friendlyError("Delete failed", ex), "OK", null);
                });
            }
        }, "delete-note").start();
    }

    private static String friendlyError(String prefix, Exception ex) {
        String message = ex == null ? "Unknown error" : ex.getMessage();
        if (message == null || message.length() == 0) {
            return prefix + ": unknown error";
        }
        if (message.indexOf("HTTP 404") >= 0) {
            return prefix + ": note not found";
        }
        if (message.indexOf("HTTP 400") >= 0) {
            return prefix + ": invalid note content";
        }
        if (message.indexOf("HTTP 0") >= 0 || message.indexOf("Connection") >= 0 || message.indexOf("timeout") >= 0) {
            return prefix + ": API unavailable, check localhost:8080";
        }
        return prefix + ": " + message;
    }

    private void focusEditor(TextArea field) {
        if (!field.isEditable()) {
            return;
        }
        field.requestFocus();
        field.startEditingAsync();
    }
}
