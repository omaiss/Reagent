package org.example.mvp;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;

public class DisplayPlugin extends AnAction {
    public void actionPerformed(AnActionEvent event) {
        Messages.showMessageDialog(
                "Hello from Java!",
                "Greeting",
                Messages.getInformationIcon()
        );
    }
}
