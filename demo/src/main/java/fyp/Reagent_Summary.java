package fyp;

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
import java.util.List;

public class Reagent_Summary implements ToolWindowFactory {
    private JButton refreshButton;
    private Tree summaryTree;
    private Project project;
    private static final LogWriter logWriter = LogWriter.getInstance();

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
                            SwingUtilities.invokeLater(() -> updateSummary("Please select code to generate a summary."));
                            return;
                        }

                        String fullPrompt = "Generate a structured summary with the following headings: Vulnerabilities, PEP8 Violations, Fixes, and Summary of the Code. Ensure consistency in formatting. Do not include extra explanations, --- lines or code—just the categorized summary.\n\n" + codeToSummarize;

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
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void updateSummary(String response) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("📋 AI Generated Summary");

        try {
            String aiText = response.trim().replace("\\n", "\n").replace("\\\"", "\"");
            String[] lines = aiText.split("\n");

            for (String line : lines) {
                line = line.trim().replaceAll("^\"|\"$", ""); // Remove quotes
                if (line.isEmpty()) continue;

                if (line.equalsIgnoreCase("**Vulnerabilities**")) {
                    root.add(new DefaultMutableTreeNode("💡 Vulnerabilities"));
                } else if (line.equalsIgnoreCase("**PEP8 Violations**")) {
                    root.add(new DefaultMutableTreeNode("⚠️ PEP8 Violations"));
                } else if (line.equalsIgnoreCase("**Fixes**")) {
                    root.add(new DefaultMutableTreeNode("✅ Fixes"));
                } else if (line.equalsIgnoreCase("**Summary of the Code**")) {
                    root.add(new DefaultMutableTreeNode("📌 Code Summary"));
                } else {
                    root.add(new DefaultMutableTreeNode(line));
                }
            }
        } catch (Exception e) {
            root.add(new DefaultMutableTreeNode("⚠️ Error parsing response: " + e.getMessage()));
        }

        DefaultMutableTreeNode lastNode = new DefaultMutableTreeNode("🚀 User Journey");
        List<String> userJourneyLogs = logWriter.getUserJourneyLogs();  // Fetch logs

        if (userJourneyLogs == null || userJourneyLogs.isEmpty()) {
            lastNode.add(new DefaultMutableTreeNode("No interactions yet."));
        } else {
            int logNumber = 1;
            for (String log : userJourneyLogs) {
                String cleanedLog = log;
                cleanedLog = cleanedLog.trim();
                cleanedLog = logNumber + ") " + cleanedLog;
                logNumber++;

                String[] parts = cleanedLog.split("Problem:");
                if (parts.length > 1) {
                    String[] problemSolution = parts[1].split("Solution:");
                    if (problemSolution.length == 2) {
                        lastNode.add(new DefaultMutableTreeNode("Problem:"));
                        lastNode.add(new DefaultMutableTreeNode(problemSolution[0].trim()));
                        lastNode.add(new DefaultMutableTreeNode("Solution:"));
                        lastNode.add(new DefaultMutableTreeNode(problemSolution[1].trim()));
                    } else {
                        lastNode.add(new DefaultMutableTreeNode(cleanedLog));
                    }
                } else {
                    lastNode.add(new DefaultMutableTreeNode(cleanedLog));
                }
            }
        }
        root.add(lastNode);

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
}
