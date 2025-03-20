package fyp;

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

public class Reagent_Solution_Ranking implements ToolWindowFactory {
    private JButton rankButton;
    private Tree rankingTree;
    private Project project;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 248, 255));

        // Header Panel with Rank Button
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rankButton = new JButton("🏆 Rank Solutions");
        rankButton.setForeground(Color.WHITE);
        rankButton.setBackground(new Color(65, 105, 225));
        rankButton.setFocusPainted(false);
        rankButton.setFont(new Font("Arial", Font.BOLD, 14));
        rankButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        rankButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        rankButton.addActionListener(e -> fetchRanking());
        headerPanel.add(rankButton);

        // Ranking Tree Setup
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("🏆 Solution Rankings");
        rankingTree = new Tree(new DefaultTreeModel(root));
        rankingTree.setCellRenderer(new RankingTreeRenderer());
        JBScrollPane scrollPane = new JBScrollPane(rankingTree);
        scrollPane.setBorder(JBUI.Borders.empty(10));

        // Assemble UI
        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "Solution Ranking", false);
        toolWindow.getContentManager().addContent(content);
    }

    private void fetchRanking() {
        rankButton.setEnabled(false);
        rankButton.setText("⏳ Ranking...");

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Ranking Solutions...", false) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    String codeToRank = getSelectedCode();
                    if (codeToRank.isEmpty()) {
                        SwingUtilities.invokeLater(() -> updateRankingTree("⚠️ Please select code to rank."));
                        return;
                    }

                    // Send prompt to AI model via AITalker
                    String fullPrompt = """
                            Evaluate the following code on a scale of 1-10 based on efficiency, readability, and best practices.
                            Then, generate two alternative solutions with different approaches and rank all three.
                            Format the response as:
                            **Original Solution**
                            Score: X
                            Solution: ...
                            
                            **Alternative 1**
                            Score: Y
                            Solution: ...
                            
                            **Alternative 2**
                            Score: Z
                            Solution: ...
                            """ + codeToRank;

                    AITalker aiTalker = new AITalker();
                    String response = aiTalker.analyzeCodeWithModel(fullPrompt);

                    SwingUtilities.invokeLater(() -> updateRankingTree(response));
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> updateRankingTree("⚠️ Failed to rank solutions: " + e.getMessage()));
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        rankButton.setEnabled(true);
                        rankButton.setText("🏆 Rank Solutions");
                    });
                }
            }
        });
    }

    private void updateRankingTree(String response) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("🏆 Solution Rankings");

        try {
            String aiText = response.trim().replace("\\n", "\n").replace("\\\"", "\"");
            String[] lines = aiText.split("\n");
            DefaultMutableTreeNode currentNode = null;

            for (String line : lines) {
                line = line.trim().replaceAll("^\"|\"$", "");
                if (line.isEmpty()) continue;

                // Section Headers with Stickers 🌟
                if (line.equalsIgnoreCase("**Original Solution**")) {
                    currentNode = new DefaultMutableTreeNode("⭐ Original Solution");
                } else if (line.equalsIgnoreCase("**Alternative 1**")) {
                    currentNode = new DefaultMutableTreeNode("⚡ Alternative 1");
                } else if (line.equalsIgnoreCase("**Alternative 2**")) {
                    currentNode = new DefaultMutableTreeNode("💡 Alternative 2");
                } else if (line.startsWith("Score:")) {
                    if (currentNode != null) {
                        DefaultMutableTreeNode scoreNode = new DefaultMutableTreeNode("📊 " + line);
                        currentNode.add(scoreNode);
                    }
                } else if (line.startsWith("Solution:")) {
                    if (currentNode != null) {
                        DefaultMutableTreeNode solutionNode = new DefaultMutableTreeNode("📝 " + line);
                        currentNode.add(solutionNode);
                    }
                } else {
                    if (currentNode != null) currentNode.add(new DefaultMutableTreeNode(line));
                }

                if (currentNode != null && !root.isNodeChild(currentNode)) {
                    root.add(currentNode);
                }
            }
        } catch (Exception e) {
            root.add(new DefaultMutableTreeNode("⚠️ Error parsing response: " + e.getMessage()));
        }

        rankingTree.setModel(new DefaultTreeModel(root));
        rankingTree.updateUI();
    }

    private String getSelectedCode() {
        return com.intellij.openapi.application.ReadAction.nonBlocking(() -> {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (editor != null) {
                String selectedText = editor.getSelectionModel().getSelectedText();
                return (selectedText != null && !selectedText.isEmpty()) ? selectedText : "";
            }
            return "";
        }).executeSynchronously();
    }
}
