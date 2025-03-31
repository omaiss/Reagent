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

        // Header with Refresh Button
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshButton = new JButton("🔄 Generate Summary");
        refreshButton.addActionListener(e -> fetchSummary());
        headerPanel.add(refreshButton);

        // Tree for displaying summaries
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("📋 AI Code Summary");
        summaryTree = new Tree(new DefaultTreeModel(root));
        JBScrollPane scrollPane = new JBScrollPane(summaryTree);
        scrollPane.setBorder(JBUI.Borders.empty(10));

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Add panel to tool window
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
                            SwingUtilities.invokeLater(() -> updateSummary("⚠️ Please select code to generate a summary."));
                            return;
                        }

                        String fullPrompt = "Generate a structured summary with these headings: "
                                + "**Vulnerabilities**, **PEP8 Violations**, **Fixes**, **Summary of the Code**. "
                                + "Ensure proper formatting and avoid extra explanations.\n\n" + codeToSummarize;

                        AITalker aiTalker = new AITalker();
                        String summary = aiTalker.analyzeCodeWithModel(fullPrompt);
                        SwingUtilities.invokeLater(() -> updateSummary(summary));
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(() -> updateSummary("⚠️ Failed to fetch summary: " + e.getMessage()));
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

    // Function updated
    private void updateSummary(String response) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("📋 AI Generated Summary");

        try {
            String[] lines = response.trim().replace("\\n", "\n").replace("\\\"", "\"").split("\n");
            DefaultMutableTreeNode currentCategory = null;

            for (String line : lines) {
                line = line.trim().replaceAll("^\"|\"$", "");
                if (line.isEmpty()) continue;

                switch (line) {
                    case "**Vulnerabilities**":
                        currentCategory = new DefaultMutableTreeNode("💡 Vulnerabilities");
                        root.add(currentCategory);
                        break;
                    case "**PEP8 Violations**":
                        currentCategory = new DefaultMutableTreeNode("⚠️ PEP8 Violations");
                        root.add(currentCategory);
                        break;
                    case "**Fixes**":
                        currentCategory = new DefaultMutableTreeNode("✅ Fixes");
                        root.add(currentCategory);
                        break;
                    case "**Summary of the Code**":
                        currentCategory = new DefaultMutableTreeNode("📌 Code Summary");
                        root.add(currentCategory);
                        break;
                    default:
                        if (currentCategory != null) {
                            currentCategory.add(new DefaultMutableTreeNode(line));
                        }
                        break;
                }
            }
        } catch (Exception e) {
            root.add(new DefaultMutableTreeNode("⚠️ Error parsing response: " + e.getMessage()));
        }

        // Adding User Journey Logs
        DefaultMutableTreeNode userJourneyNode = new DefaultMutableTreeNode("🚀 User Journey");
        List<String> userJourneyLogs = logWriter.getUserJourneyLogs();

        if (userJourneyLogs == null || userJourneyLogs.isEmpty()) {
            userJourneyNode.add(new DefaultMutableTreeNode("No interactions yet."));
        } else {
            int logNumber = 1;
            for (String log : userJourneyLogs) {
<<<<<<< HEAD
                String cleanedLog = log;
                cleanedLog = cleanedLog.trim();
                cleanedLog = logNumber + ") " + cleanedLog;
=======
                DefaultMutableTreeNode logEntryNode = new DefaultMutableTreeNode("🔹 Interaction " + logNumber);
>>>>>>> d724a15cf251a3750020264ebcf2bead9f217a55
                logNumber++;
                String[] parts = log.split("\n\n");

                // Parse and add each section in a structured format
                for (String part : parts) {
                    if (part.startsWith("Problem Code:")) {
                        DefaultMutableTreeNode problemCodeNode = new DefaultMutableTreeNode("📝 Problem Code");
                        String code = part.replace("Problem Code:\n", "").trim();
                        problemCodeNode.add(new DefaultMutableTreeNode(code));
                        logEntryNode.add(problemCodeNode);
                    } else if (part.startsWith("Problem Summary:")) {
                        DefaultMutableTreeNode problemSummaryNode = new DefaultMutableTreeNode("❌ Problem Summary");
                        String summary = part.replace("Problem Summary:\n", "").trim();
                        problemSummaryNode.add(new DefaultMutableTreeNode(summary));
                        logEntryNode.add(problemSummaryNode);
                    } else if (part.startsWith("Fixed Code:")) {
                        DefaultMutableTreeNode fixedCodeNode = new DefaultMutableTreeNode("🛠️ Fixed Code");
                        String fixedCode = part.replace("Fixed Code:\n", "").trim();
                        fixedCodeNode.add(new DefaultMutableTreeNode(fixedCode));
                        logEntryNode.add(fixedCodeNode);
                    } else if (part.startsWith("Solution Summary:")) {
                        DefaultMutableTreeNode solutionSummaryNode = new DefaultMutableTreeNode("✅ Solution Summary");
                        String solution = part.replace("Solution Summary:\n", "").trim();
                        solutionSummaryNode.add(new DefaultMutableTreeNode(solution));
                        logEntryNode.add(solutionSummaryNode);
                    }
                }

                userJourneyNode.add(logEntryNode);
            }
        }
        root.add(userJourneyNode);

        // Update UI
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
