package com.teggr.notebook;

import com.codename1.system.Lifecycle;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.teggr.notebook.service.NotebookApiClient;
import com.teggr.notebook.ui.AppShellForm;

public class NotebookCodenameOneApp extends Lifecycle {
    @Override
    public void runApp() {
        try {
            new AppShellForm(new NotebookApiClient()).show();
        } catch (RuntimeException ex) {
            Form fallback = new Form("Startup Error", BoxLayout.y());
            fallback.add(new Label("App failed to start."));
            fallback.add(new Label(ex.getClass().getSimpleName()));
            fallback.add(new Label(ex.getMessage() == null ? "No message" : ex.getMessage()));
            fallback.show();
            Dialog.show("Startup Error", ex.toString(), "OK", null);
        }
    }
}
