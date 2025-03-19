package fyp;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiDocumentManager;
import com.jetbrains.python.psi.PyFile;
import org.jetbrains.annotations.NotNull;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

public class AIQuickFix implements IntentionAction {
    private final List<String> violations;

    public AIQuickFix(List<String> violations) {
        this.violations = violations;
    }

    @Override
    public @NotNull String getText() {
        return "Apply AI Suggested Code";
    }

    @Override
    public @NotNull String getFamilyName() {
        return "AI Code Fixes";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return file instanceof PyFile;
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {
        if (!(file instanceof PyFile)) {
            return;
        }

        Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating AI Fix", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(true);
                indicator.setText("AI is processing... Please wait.");

                AITalker talker = new AITalker();
                String userCode = file.getText();
                String violationsText = String.join("", violations);
                String prompt = "The following Code below has PEP8 violations as listed below in the Violations heading." +
                        "Fix all the violations and return the corrected code only (NO EXPLANATION).\n\nViolations:\nLine Number : Violations\n" + violationsText + "\n\nCode:\n" + userCode;

                try {
                    String aiSuggestedCode = talker.analyzeCodeWithModel(prompt);
                    if (aiSuggestedCode == null || aiSuggestedCode.trim().isEmpty()) {
                        showError("AI did not return a valid response.");
                        return;
                    }

                    String formattedCode = aiSuggestedCode.replaceAll("^\"```python\\s*", "")
                            .replaceAll("\\s*```\"$", "")
                            .trim();

                    formattedCode = formattedCode.replace("\\n", "\n")
                            .replace("\\\"", "\"")  // Fix escaped double quotes
                            .replace("\\'", "'");   // Fix escaped single quotes

                    // Remove explanation if it exists
                    int explanationIndex = formattedCode.lastIndexOf("'''");
                    if (explanationIndex != -1) {
                        formattedCode = formattedCode.substring(0, explanationIndex).trim();
                    }

                    if (formattedCode.isEmpty()) {
                        showError("AI response was empty or invalid.");
                        return;
                    }

                    updateDocument(document, formattedCode, project);
                } catch (Exception e) {
                    showError("Error processing AI response: " + e.getMessage());
                }
            }
        });
    }

    private void showError(String message) {
        Messages.showErrorDialog(message, "AI Fix Error");
    }

    private void updateDocument(Document document, String newCode, Project project) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() -> {
            com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project, () -> {
                document.setText(newCode);
            });
        });
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
