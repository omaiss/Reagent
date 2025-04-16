package fyp;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiDocumentManager;
import it.unimi.dsi.fastutil.bytes.F;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DetectVulnerabilitiesAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) return;

        Editor editor = event.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);
        if (editor == null) return;

        Document document = editor.getDocument();
        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (psiFile == null) return;

        String code = document.getText();
        String[] lines = code.split("\n");
        StringBuilder numberedCode = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            numberedCode.append(i + 1).append("  ").append(lines[i]).append("\n");
        }

        code = numberedCode.toString();

        try {
            List<Flake8Highlight.Violation> vulnerabilities = getVulnerabilites(code);
            Flake8Highlight.setVulnerabilities(vulnerabilities);

            if (vulnerabilities.isEmpty()) {
                System.out.println("No vulnerabilities found!");
            } else {
                System.out.println("Vulnerabilities Found:");
                for (Flake8Highlight.Violation v : vulnerabilities) {
                    System.out.println("Line " + v.line + ": " + v.message);
                }
            }
            PsiDocumentManager.getInstance(project).commitAllDocuments();
            DaemonCodeAnalyzer.getInstance(project).restart();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error running vulnerability detection.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    List<Flake8Highlight.Violation> getVulnerabilites(String code) throws IOException {
        String prompt = "\"Analyze the following Python code and identify only the definite vulnerabilities. " +
                "Do not include potential or speculative issues—only those that are certain and directly present in the code.\n"
                + "For each issue, provide the format:\n"
                + "Line Number : Vulnerability\n"
                + "Example:\n 15 : SQL Injection\n"
                + "Ensure the response strictly follows this format without extra explanations.\n\n"
                + "Code:\n"
                + code;

        AITalker aiTalker = new AITalker();
        String response = aiTalker.analyzeCodeWithModel(prompt);
        List<Flake8Highlight.Violation> violations = new ArrayList<>();
        response = response.replace("\"", "");
        response = response.replace("\\n", "\n");

        for (String line : response.split("\n")) {
            String[] parts = line.split(":", 2);
            if (parts.length == 2) {
                try {
                    int lineNum = Integer.parseInt(parts[0].trim());
                    String message = parts[1].trim();
                    violations.add(new Flake8Highlight.Violation(lineNum, 0, message));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return violations;
    }
}
