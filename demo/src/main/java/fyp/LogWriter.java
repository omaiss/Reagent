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
import java.util.ArrayList;

public class LogWriter {
    private static final LogWriter instance = new LogWriter();
    private final List<String> userJourneyLogs = new ArrayList<>();

    private LogWriter() {}

    public static LogWriter getInstance() {
        return instance;
    }

    public void logInteraction(String prompt, String response) {
        String summaryPrompt = String.format(
                "Summarize the following interaction in this format keep it short:\n"
                        + "Problem: <describe the problem> Solution: <describe the solution>"
                        + "Problem: %s Solution: %s", prompt, response
        );

        try {
            AITalker aiTalker = new AITalker();
            String summary = aiTalker.analyzeCodeWithModel(summaryPrompt);
            userJourneyLogs.add(summary.trim());
        } catch (Exception e) {
            userJourneyLogs.add("⚠️ Failed to summarize: " + e.getMessage());
        }
    }

    public List<String> getUserJourneyLogs() {
        return new ArrayList<>(userJourneyLogs);
    }
}
