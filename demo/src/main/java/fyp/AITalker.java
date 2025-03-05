package fyp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.ui.JBColor;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.*;

public class AITalker implements DocumentListener {
    private static final String HUGGING_FACE_API_URL = "https://api-inference.huggingface.co/models/FYPFAST/Llama-3.2-8B-Instruct-PEP8-Vulnerability-Python";
    private static final String HUGGING_FACE_API_KEY = System.getenv("HUGGING_FACE_API_KEY"); // Use environment variable
    private static final Logger log = LoggerFactory.getLogger(AITalker.class);

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledTask = null;

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        scheduledTask = scheduler.schedule(() -> processDocumentChange(event), 5, TimeUnit.SECONDS);
    }

    private void processDocumentChange(DocumentEvent event) {
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(event.getDocument());
        if (virtualFile == null) return;

        Project project = ProjectManager.getInstance().getOpenProjects()[0];
        String code = event.getDocument().getText();

        try {
            String response = analyzeCodeWithModel(code);
            saveResponseToFile(project, response);
            AnalysisResult result = parseAnalysisResult(response);
            if (result.corrected_code != null && !result.corrected_code.isEmpty()) {
                updateDocument(project, result.corrected_code);
            }
            highlightViolations(project, result.violations);
            highlightVulnerabilities(project, result.vulnerabilities);
        } catch (IOException e) {
            log.error("Error processing document change: ", e);
        }
    }

    private String analyzeCodeWithModel(String code) throws IOException {
        OkHttpClient client = new OkHttpClient();
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

        String prompt = "Analyze the following Python code for PEP8 violations and security vulnerabilities. " +
                "Return the corrected code and a list of issues found:\n\n" + code;

        String json = "{"
                + "\"inputs\": \"" + prompt.replace("\"", "\\\"") + "\","
                + "\"parameters\": {\"max_new_tokens\": 500, \"temperature\": 0.5}"
                + "}";

        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url(HUGGING_FACE_API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + HUGGING_FACE_API_KEY)
                .build();

        int retries = 3;
        while (retries > 0) {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    return response.body() != null ? response.body().string() : "";
                } else if (response.code() == 503) {
                    log.warn("API unavailable. Retrying...");
                    retries--;
                    Thread.sleep(5000); // Wait 5 seconds before retrying
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    log.error("API Error {}: {}", response.code(), errorBody);
                    return "";
                }
            } catch (InterruptedException e) {
                log.error("Retry interrupted: ", e);
                return "";
            }
        }
        return "";
    }

    private void saveResponseToFile(Project project, String response) {
        String projectBasePath = project.getBasePath();
        if (projectBasePath == null) return;
        String filePath = Paths.get(projectBasePath, "huggingface_response.txt").toString();

        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(response);
        } catch (IOException e) {
            log.error("Error saving response to file", e);
        }
    }

    private AnalysisResult parseAnalysisResult(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, AnalysisResult.class);
        } catch (IOException e) {
            log.error("Error parsing analysis result: ", e);
            return new AnalysisResult();
        }
    }

    private void updateDocument(Project project, String correctedCode) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) return;

        editor.getDocument().setText(correctedCode);
    }

    private void highlightViolations(Project project, List<Violation> violations) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) return;

        for (Violation violation : violations) {
            int startOffset = editor.getDocument().getLineStartOffset(violation.line - 1);
            int endOffset = editor.getDocument().getLineEndOffset(violation.line - 1);
            TextAttributes attributes = new TextAttributes(null, JBColor.YELLOW, null, null, Font.PLAIN);
            RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(
                    startOffset, endOffset, HighlighterLayer.ERROR, attributes, HighlighterTargetArea.EXACT_RANGE
            );
            highlighter.setErrorStripeTooltip("PEP 8 Violation: " + violation.message);
        }
    }

    private void highlightVulnerabilities(Project project, List<Vulnerability> vulnerabilities) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) return;

        for (Vulnerability vulnerability : vulnerabilities) {
            int startOffset = editor.getDocument().getLineStartOffset(vulnerability.line - 1);
            int endOffset = editor.getDocument().getLineEndOffset(vulnerability.line - 1);
            TextAttributes attributes = new TextAttributes(null, JBColor.RED, null, null, Font.PLAIN);
            RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(
                    startOffset, endOffset, HighlighterLayer.ERROR, attributes, HighlighterTargetArea.EXACT_RANGE
            );
            highlighter.setErrorStripeTooltip("Vulnerability: " + vulnerability.message);
        }
    }

    private static class AnalysisResult {
        String corrected_code;
        List<Violation> violations;
        List<Vulnerability> vulnerabilities;
    }

    private static class Violation {
        int line, column;
        String message;
    }

    private static class Vulnerability {
        int line, column;
        String message;
    }
}