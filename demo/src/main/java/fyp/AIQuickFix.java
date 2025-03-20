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

import java.util.List;
import java.util.Arrays;

public class AIQuickFix implements IntentionAction {
    private final List<String> violations;
    private final String fixType;
    private static final LogWriter logWriter = LogWriter.getInstance();  // Singleton LogWriter

    public AIQuickFix(List<String> violations, String fixType) {
        this.violations = violations;
        this.fixType = fixType;
    }

    @Override
    public @NotNull String getText() {
        switch (fixType) {
            case "pep8":
                return "Fix PEP8 Violations";
            case "vulnerabilities":
                return "Fix Vulnerabilities";
            case "both":
                return "Fix PEP8 & Vulnerabilities";
            default:
                return "Apply AI Suggested Code";
        }
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

                String prompt;
                switch (fixType) {
                    case "pep8":
                        prompt = "The following code has PEP8 violations." +
                                "Fix them and return the corrected code only (NO EXPLANATION).\n\n" +
                                "Violations:\n" + violationsText + "\n\nCode:\n" + userCode;
                        break;
                    case "vulnerabilities":
                        prompt = "The following code has security vulnerabilities. " +
                                "Fix only the vulnerable parts and return the corrected code only (NO EXPLANATION).\n\n" +
                                "Vulnerabilities:\n" +
                                violationsText +
                                "Code:\n" + userCode;
                        break;
                    case "both":
                        prompt = "The following code has both PEP8 violations and security vulnerabilities. " +
                                "Fix them all and return the corrected code only (NO EXPLANATION).\n\n" +
                                "Violations:\n" + violationsText + "\n\nCode:\n" + userCode;
                        break;
                    default:
                        prompt = userCode;
                }

                try {
                    String aiSuggestedCode = talker.analyzeCodeWithModel(prompt);
                    if (aiSuggestedCode == null || aiSuggestedCode.trim().isEmpty()) {
                        showError("AI did not return a valid response.");
                        return;
                    }

                    // Clean AI response and apply the fix
                    aiSuggestedCode = cleanAIResponse(aiSuggestedCode);
                    updateDocument(document, aiSuggestedCode, project);

                    // ✅ Log the interaction: problem (prompt) + solution (aiSuggestedCode)
                    logWriter.logInteraction(prompt, aiSuggestedCode);
                    System.out.println("Interaction logged successfully.");

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

    private String cleanAIResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "";
        }

        // Trim any extra spaces or newlines
        response = response.trim();

        // Remove both leading and trailing code block markers
        response = response.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("\\s*```$", "");

        // Convert escaped newlines and quotes
        response = response.replace("\\n", "\n").replace("\\\"", "\"");

        String[] lines = response.split("\n");
        if (lines.length > 2) { // Ensure at least 2 lines exist to avoid errors
            response = String.join("\n", Arrays.copyOfRange(lines, 1, lines.length - 1));
        } else {
            response = ""; // If there are only 1-2 lines, return an empty string
        }

        return response.trim();
    }
}
