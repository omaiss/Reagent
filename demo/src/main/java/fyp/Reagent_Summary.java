package fyp;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Reagent_Summary implements ToolWindowFactory {
    private JButton refreshButton;
    private Tree summaryTree;
    private Project project;

    public Reagent_Summary() {
        ObjectMapper objectMapper = new ObjectMapper();
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        JPanel panel = new JPanel(new BorderLayout());

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshButton = new JButton("🔄 Generate Summary");
        refreshButton.addActionListener(e -> fetchSummary());
        headerPanel.add(refreshButton);

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("AI Code Summary");
        summaryTree = new Tree(new DefaultTreeModel(root));
        JBScrollPane scrollPane = new JBScrollPane(summaryTree);

        scrollPane.setBorder(JBUI.Borders.empty(10));

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private void fetchSummary() {
        refreshButton.setEnabled(false);
        refreshButton.setText("⏳ Generating...");

        try {
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Generating code summary...", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        String codeToSummarize = getSelectedCode();
                        if (codeToSummarize.isEmpty()) {
                            SwingUtilities.invokeLater(() -> updateSummary(" Please select code to generate a summary."));
                            return;
                        }

                        String fullPrompt = "Generate a structured summary with the following headings: Vulnerabilities, PEP8 Violations, Fixes, and Summary of the Code. Ensure consistency in formatting. Do not include extra explanations, --- lines or code—just the categorized summary.\n\n" + codeToSummarize;

                        System.out.println("Code sent for summary: " + fullPrompt);
                        AITalker aiTalker = new AITalker();
                        String summary = aiTalker.analyzeCodeWithModel(fullPrompt);
                        SwingUtilities.invokeLater(() -> updateSummary(summary));
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> updateSummary("Failed to fetch summary: " + e.getMessage()));
                    } finally {
                        SwingUtilities.invokeLater(() -> {
                            refreshButton.setEnabled(true);
                            refreshButton.setText("🔄 Refresh Summary");
                        });
                    }
                }
            });
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private void updateSummary(String response) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("📋 AI Generated Summary");

        try {
            String aiText = response.trim();
            aiText = aiText.replace("\\n", "\n").replace("\\\"", "\"");

            String[] lines = aiText.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                root.add(new DefaultMutableTreeNode(line));
            }
        } catch (Exception e) {
            root.add(new DefaultMutableTreeNode("⚠️ Error parsing response: " + e.getMessage()));
        }

        summaryTree.setModel(new DefaultTreeModel(root));
        summaryTree.updateUI();
    }

    private String getSelectedCode() {
        return ReadAction.nonBlocking(() -> {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor != null) {
                String selectedText = editor.getSelectionModel().getSelectedText();
                return (selectedText != null && !selectedText.isEmpty()) ? selectedText : "";
            }
            return "";
        }).executeSynchronously();
    }

    private static class RequestPayload {
        public String prompt;
        public RequestPayload(String prompt) {
            this.prompt = prompt;
        }
    }

    private static class ResponsePayload {
        public String ai_response;
        public ResponsePayload(String ai_response) {
            this.ai_response = ai_response;
        }
    }
}
