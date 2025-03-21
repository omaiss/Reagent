package fyp;

import java.util.List;
import java.util.ArrayList;
// Class updated

public class LogWriter {
    private static final LogWriter instance = new LogWriter();
    private final List<String> userJourneyLogs = new ArrayList<>();

    private LogWriter() {}

    public static LogWriter getInstance() {
        return instance;
    }

    public void logInteraction(String prompt, String aiSuggestedCode) {
        String problemType;
        String originalCode;

        // Extract problem type and original code
        if (prompt.contains("PEP8 violations") && prompt.contains("security vulnerabilities")) {
            problemType = "PEP8 & Security Issues";
            originalCode = prompt.split("Code:\n", 2)[1].trim();
        } else if (prompt.contains("PEP8 violations")) {
            problemType = "PEP8 Issues";
            originalCode = prompt.split("Code:\n", 2)[1].trim();
        } else if (prompt.contains("security vulnerabilities")) {
            problemType = "Security Issues";
            originalCode = prompt.split("Code:\n", 2)[1].trim();
        } else {
            problemType = "Unknown Issue";
            originalCode = prompt.trim();
        }

        // Prepare AI prompt
        String summaryPrompt = String.format(
                "Problem Code:\n%s\n\nFixed Code:\n%s\n\n" +
                        "Describe the problem and solution in the format:\n" +
                        "Problem: <describe in one line>\nSolution: <describe in one line>",
                originalCode, aiSuggestedCode
        );

        try {
            AITalker aiTalker = new AITalker();
            String summaryResponse = aiTalker.analyzeCodeWithModel(summaryPrompt);

            // Extract problem and solution description
            String problemSummary = summaryResponse.split("Solution:")[0].replace("Problem:", "").trim();
            String solutionSummary = summaryResponse.split("Solution:")[1].trim();

            // Format the log entry
            String logEntry = String.format(
                    "Problem Code:\n%s\n\nProblem Summary:\n%s\n\nFixed Code:\n%s\n\nSolution Summary:\n%s",
                    originalCode, problemSummary, aiSuggestedCode, solutionSummary
            );

            // Store log entry
            userJourneyLogs.add(logEntry);
        } catch (Exception e) {
            userJourneyLogs.add("⚠️ Failed to summarize: " + e.getMessage());
        }
    }

    public List<String> getUserJourneyLogs() {
        return new ArrayList<>(userJourneyLogs);
    }
}
