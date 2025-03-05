package fyp;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import com.jetbrains.python.psi.PyBinaryExpression;
import com.jetbrains.python.psi.PyElement;
import com.jetbrains.python.psi.PyExpression;

public class PEP8Annotator implements Annotator {
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof PyBinaryExpression)) {
            return;
        }

        PyBinaryExpression binaryExpression = (PyBinaryExpression) element;
        PyExpression leftOperand = binaryExpression.getLeftExpression();
        PyExpression rightOperand = binaryExpression.getRightExpression();

        // Ensure comparison is `== None`
        if (rightOperand != null && "None".equals(rightOperand.getText()) && "==".equals(binaryExpression.getPsiOperator().getText())) {
            holder.newAnnotation(HighlightSeverity.WARNING, "Use 'is None' instead of '== None'")
                    .range(binaryExpression.getTextRange())
                    .withFix(new PEP8QuickFix())
                    .create();
        }
    }
}
