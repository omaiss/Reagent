package fyp;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiDocumentManager;
import com.jetbrains.python.psi.PyFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class AIQuickFix implements IntentionAction {

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
        return file instanceof PyFile; // Only available for Python files
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

        AITalker talker = new AITalker();
        String userCode = file.getText();
        String prompt = "You are a code fixer AI. Correct the following Python code for PEP 8 violations and security vulnerabilities. " +
                "Just return the corrected code.\n\nCode:\n" + userCode;

        String aiSuggestedCode = null;
        try {
            aiSuggestedCode = talker.analyzeCodeWithModel(prompt);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (aiSuggestedCode != null && !aiSuggestedCode.trim().isEmpty()) {
            document.setText(aiSuggestedCode); // Replace user code with AI-generated code
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
