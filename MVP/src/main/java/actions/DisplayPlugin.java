package actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

public class DisplayPlugin extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Messages.showInfoMessage("Hello from Java!", "Hello");
    }
}
