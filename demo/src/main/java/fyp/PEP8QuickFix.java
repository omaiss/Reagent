package fyp;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.psi.PyElementGenerator;
import com.jetbrains.python.psi.PyBinaryExpression;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.LanguageLevel;
import org.jetbrains.annotations.NotNull;

public class PEP8QuickFix implements IntentionAction {
    @Override
    public @NotNull String getText() {
        return "Replace '== None' with 'is None'";
    }

    @Override
    public @NotNull String getFamilyName() {
        return "PEP8 Fixes";
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        return file.getLanguage().getID().equals("Python");
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) {

        PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());


        while (element != null && !(element instanceof PyBinaryExpression)) {
            element = element.getParent();
        }

        if (!(element instanceof PyBinaryExpression)) {
            return;
        }

        PyBinaryExpression binaryExpression = (PyBinaryExpression) element;
        PyExpression leftOperand = binaryExpression.getLeftExpression();
        PyExpression rightOperand = binaryExpression.getRightExpression();

        if (rightOperand != null && "None".equals(rightOperand.getText())) {
            PyElementGenerator generator = PyElementGenerator.getInstance(project);

            String newCode = leftOperand.getText() + " is None";
            PyExpression newExpression = generator.createExpressionFromText(LanguageLevel.PYTHON39, newCode);

            binaryExpression.replace(newExpression);
        }
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
