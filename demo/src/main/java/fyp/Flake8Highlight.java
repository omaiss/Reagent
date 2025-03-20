package fyp;

import com.github.weisj.jsvg.L;
import com.intellij.lang.annotation.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiDocumentManager;
import com.jetbrains.python.psi.PyFile;
import it.unimi.dsi.fastutil.bytes.V;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;

public class Flake8Highlight implements Annotator {
    private static List<Violation> vulnerabilities;

    public static void setVulnerabilities(List<Violation> _vulnerabilities) {
        vulnerabilities = _vulnerabilities;
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
        String code = file.getText();
        List<String> violations_strings = new ArrayList<>();
        List<Violation> violations = executeFlake8(code);

        for (Violation violation : violations) {
            violations_strings.add(violation.line + " : " + violation.message + '\n');
        }

        if (vulnerabilities != null) {
            for (Violation vulnerability : vulnerabilities) {
                violations_strings.add(vulnerability.line + " : " + vulnerability.message + '\n');
            }
        }

        HighlightSeverity pep8Severity = HighlightSeverity.WARNING;
        HighlightSeverity vulnerabilitySeverity = HighlightSeverity.ERROR;

        List<Violation> allIssues = new ArrayList<>();
        allIssues.addAll(violations);
        if (vulnerabilities != null) {
            allIssues.addAll(vulnerabilities);
        }

        for (Violation issue : allIssues) {
            int lineNumber = issue.line - 1;
            if (lineNumber < document.getLineCount()) {
                int lineStartOffset = document.getLineStartOffset(lineNumber);
                PsiElement targetElement = file.findElementAt(lineStartOffset);
                if (targetElement != null) {
                    boolean isPep8 = violations.contains(issue);
                    HighlightSeverity severity = isPep8 ? pep8Severity : vulnerabilitySeverity;
                    String issueType = isPep8 ? "PEP8 Violation: " : "Code Vulnerability: ";

                    holder.newAnnotation(severity, issueType + issue.message)
                            .range(targetElement.getTextRange())
                            .withFix(new AIQuickFix(violations_strings, "pep8"))
                            .withFix(new AIQuickFix(violations_strings, "vulnerabilities"))
                            .withFix(new AIQuickFix(violations_strings, "both"))
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
        if (!isFlake8Installed()) {
            installFlake8();
        }

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

    private boolean isFlake8Installed() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("python", "-m", "flake8", "--version");
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();
        return process.exitValue() == 0;
    }

    private void installFlake8() throws IOException, InterruptedException {
        System.out.println("Flake8 not found. Installing...");
        ProcessBuilder pb = new ProcessBuilder("python", "-m", "pip", "install", "flake8");
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();
        System.out.println("Flake8 installed successfully.");
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
