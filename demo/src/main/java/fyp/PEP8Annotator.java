package fyp;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class PEP8Annotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof PsiFile file)) {
            return;
        }

        String fileContent = file.getText();

        // Example: Highlight the word "error" in the file
        String keyword = "error";
        System.out.println(fileContent);
        int index = fileContent.indexOf(keyword);
        if (index != -1) {
            TextRange range = new TextRange(index, index + keyword.length());
            highlightAndRemoveAfterDelay(file.getProject(), file, range, JBColor.YELLOW);
        }
    }

    private void highlightAndRemoveAfterDelay(Project project, PsiFile file, TextRange range, Color color) {
        Editor[] editors = EditorFactory.getInstance().getEditors(Objects.requireNonNull(FileDocumentManager.getInstance().getDocument(file.getVirtualFile())));
        if (editors.length == 0) {
            return;
        }

        Editor editor = editors[0];
        TextAttributes attributes = new TextAttributes(null, color, null, null, Font.PLAIN);
        RangeHighlighter highlighter = editor.getMarkupModel().addRangeHighlighter(
                range.getStartOffset(),
                range.getEndOffset(),
                HighlighterLayer.ERROR,
                attributes,
                HighlighterTargetArea.EXACT_RANGE
        );

        // Schedule removal of the highlighter after 5 seconds
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                editor.getMarkupModel().removeHighlighter(highlighter);
            }
        }, 5000); // 5000 milliseconds = 5 seconds
    }
}