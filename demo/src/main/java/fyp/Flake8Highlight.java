package fyp;

import com.intellij.lang.annotation.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiDocumentManager;
import com.jetbrains.python.psi.PyFile;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

public class Flake8Highlight implements Annotator {
    private static final TextAttributesKey ERROR_ATTRIBUTES = TextAttributesKey.createTextAttributesKey("FLAKE8_ERROR", CodeInsightColors.ERRORS_ATTRIBUTES);

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
        String code = file.getText();

        List<Violation> violations = executeFlake8(code);
        for (Violation violation : violations) {
            int lineNumber = violation.line - 1;
            if (lineNumber < document.getLineCount()) {
                int lineStartOffset = document.getLineStartOffset(lineNumber);
                System.out.println("Line Start Offset: " + lineStartOffset);

                PsiElement targetElement = file.findElementAt(lineStartOffset);
                if (targetElement != null) {
                    System.out.println(targetElement.getTextRange());

                    holder.newAnnotation(HighlightSeverity.WARNING, "PEP8 Violation: " + violation.message)
                            .range(targetElement.getTextRange())
                            .withFix(new AIQuickFix())
                            .create();
                }
            }
        }
    }


    private List<Violation> executeFlake8(String code) {
        List<Violation> violations = new ArrayList<>();
        try {
            List<String> flake8Output = getFlake8Output(code);
            for (String line : flake8Output) {
                String[] parts = line.split(":");
                if (parts.length >= 3) {
                    try {
                        int lineNum = Integer.parseInt(parts[0].trim());
                        int colNum = Integer.parseInt(parts[1].trim());
                        String message = parts[2].trim();
                        violations.add(new Violation(lineNum, colNum, message));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return violations;
    }

    private List<String> getFlake8Output(String code) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("python", "-m", "flake8", "--format=%(row)d:%(col)d: %(code)s %(text)s", "-");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream())) {
            writer.write(code);
            writer.flush();
        }

        process.waitFor();

        List<String> outputLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputLines.add(line);
            }
        }
        return outputLines;
    }

    public static class Violation {
        int line;
        int column;
        String message;

        Violation(int line, int column, String message) {
            this.line = line;
            this.column = column;
            this.message = message;
        }
    }
}
