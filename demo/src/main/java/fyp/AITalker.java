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
    private static final Logger log = LoggerFactory.getLogger(AITalker.class);
    private static String REMOTE_MODEL_URL = "https://flying-pig-skin-journals.trycloudflare.com/generate";
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> scheduledTask = null;

    public static void setRemoteModelURL(String url) {
        REMOTE_MODEL_URL = url;
        log.info("Updated Remote Model URL: " + url);
    }

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
            String prompt = "Analyze the following Python code for PEP8 violations and security vulnerabilities:\n\n" + code;
            String response = analyzeCodeWithModel(prompt);
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

    private String analyzeCodeWithModel(String prompt) throws IOException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();

        String json = objectMapper.writeValueAsString(new RequestPayload(prompt));
        RequestBody body = RequestBody.create(json, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(REMOTE_MODEL_URL).post(body).build();

        int retries = 3;
        while (retries > 0) {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return response.body().string();
                } else if (response.code() == 503) {
                    log.warn("API unavailable. Retrying...");
                    retries--;
                    Thread.sleep(5000);
                } else {
                    log.error("API Error {}: {}", response.code(), response.body() != null ? response.body().string() : "Unknown error");
                    return "";
                }
            } catch (InterruptedException e) {
                log.error("Retry interrupted: ", e);
                Thread.currentThread().interrupt();
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
        if (editor != null) {
            editor.getDocument().setText(correctedCode);
        }
    }

    private void highlightViolations(Project project, List<Violation> violations) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) return;

        for (Violation violation : violations) {
            highlightText(editor, violation.line, JBColor.YELLOW, "PEP 8 Violation: " + violation.message);
        }
    }

    private void highlightVulnerabilities(Project project, List<Vulnerability> vulnerabilities) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) return;

        for (Vulnerability vulnerability : vulnerabilities) {
            highlightText(editor, vulnerability.line, JBColor.RED, "Vulnerability: " + vulnerability.message);
        }
    }

    private void highlightText(Editor editor, int line, Color color, String tooltip) {
        int startOffset = editor.getDocument().getLineStartOffset(line - 1);
        int endOffset = editor.getDocument().getLineEndOffset(line - 1);
        TextAttributes attributes = new TextAttributes(null, color, null, null, Font.PLAIN);
        RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(
                startOffset, endOffset, HighlighterLayer.ERROR, attributes, HighlighterTargetArea.EXACT_RANGE
        );
        highlighter.setErrorStripeTooltip(tooltip);
    }

    private static class RequestPayload {
        public String prompt;
        public RequestPayload(String prompt) {
            this.prompt = prompt;
        }
    }

    private static class AnalysisResult {
        public String corrected_code;
        public List<Violation> violations;
        public List<Vulnerability> vulnerabilities;
    }

    private static class Violation {
        public int line;
        public int column;
        public String message;
    }

    private static class Vulnerability {
        public int line;
        public int column;
        public String message;
    }
}