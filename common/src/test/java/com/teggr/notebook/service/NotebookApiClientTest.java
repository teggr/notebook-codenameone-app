package com.teggr.notebook.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NotebookApiClientTest {

    @Test
    void buildNoteWriteBodyIncludesTitleAndContent() {
        String body = NotebookApiClient.buildNoteWriteBody("# Todo\n\n* note");

        assertTrue(body.contains("\"title\":\"Todo\""));
        assertTrue(body.contains("\"content\":\"# Todo\\n\\n* note\""));
    }

    @Test
    void deriveTitleFallsBackToUntitled() {
        String title = NotebookApiClient.deriveTitle("   \n\n");

        assertTrue("Untitled".equals(title));
    }
}
