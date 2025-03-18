package fyp;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.jetbrains.python.psi.PyFile;
import org.jetbrains.annotations.NotNull;

public class AIResponseAnnotator implements Annotator {
    private static String aiResponse = ""; // Store AI response statically

    public static void setAIResponse(String response) {
        aiResponse = response;
    }

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof PyFile)) {
            return;
        }
        PyFile file = (PyFile) element;

        Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
        if (document == null) {
            return;
        }

        int[] columns = {1, 6, 10, 20, 15};
        for (int i = 0; i < 5; i++) {
            int lineNumber = columns[i] - 1; // Convert to zero-based index
            if (lineNumber < document.getLineCount()) {
                int lineStartOffset = document.getLineStartOffset(lineNumber);
                System.out.println("Line Start Offset: " + lineStartOffset);

                PsiElement targetElement = file.findElementAt(lineStartOffset);
                if (targetElement != null) {
                    System.out.println(targetElement.getTextRange());

                    holder.newAnnotation(HighlightSeverity.WARNING, "AI Suggestion: Click to apply the suggested change")
                            .range(targetElement.getTextRange())
                            .withFix((IntentionAction) new AITalker())
                            .create();
                }
            }
        }
    }
}
